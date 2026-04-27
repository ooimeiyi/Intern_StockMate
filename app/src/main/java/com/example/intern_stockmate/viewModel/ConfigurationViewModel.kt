package com.example.intern_stockmate.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.intern_stockmate.data.AccountBookContext
import com.example.intern_stockmate.data.AccessPasswordStore
import com.example.intern_stockmate.data.CompanyContext
import com.example.intern_stockmate.data.DocumentNumberFormatStore
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.example.intern_stockmate.model.StockAccessRights
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CompanyOption(
    val id: String,
    val displayName: String
)

sealed interface CompanyListUiState {
    data object Loading : CompanyListUiState
    data class Success(val companies: List<CompanyOption>) : CompanyListUiState
    data class Error(val message: String) : CompanyListUiState
}

class ConfigurationViewModel(application: Application) : AndroidViewModel(application) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _companyListState = MutableStateFlow<CompanyListUiState>(CompanyListUiState.Loading)
    val companyListState: StateFlow<CompanyListUiState> = _companyListState.asStateFlow()

    val selectedCompanyId: StateFlow<String> = CompanyContext.selectedCompanyId

    val salesOrderFormat: StateFlow<String> = DocumentNumberFormatStore.salesOrderFormat
    val stockAdjustmentFormat: StateFlow<String> = DocumentNumberFormatStore.stockAdjustmentFormat
    val adminPassword: StateFlow<String> = AccessPasswordStore.adminPassword
    val stockPassword: StateFlow<String> = AccessPasswordStore.stockPassword

    private val _enabledStockAccessRoutes = MutableStateFlow<Set<String>>(emptySet())
    val enabledStockAccessRoutes: StateFlow<Set<String>> = _enabledStockAccessRoutes.asStateFlow()

    private var companiesListener: ListenerRegistration? = null
    private var userAccessListener: ListenerRegistration? = null
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        observeLoggedInUserPermissions(firebaseAuth.currentUser?.email)
    }
    private var allCompanies: List<CompanyOption> = emptyList()
    private var allowedCompanyIds: Set<String>? = null

    init {
        auth.addAuthStateListener(authStateListener)
        observeLoggedInUserPermissions(auth.currentUser?.email)
        observeCompanies()
    }

    private fun observeCompanies() {
        _companyListState.value = CompanyListUiState.Loading
        companiesListener?.remove()
        companiesListener = firestore.collection(COMPANIES_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _companyListState.value = CompanyListUiState.Error(
                        error.message ?: "Unable to load companies"
                    )
                    return@addSnapshotListener
                }

                val companiesFromDocuments = snapshot?.documents
                    ?.map { doc ->
                        CompanyOption(
                            id = doc.id,
                            displayName = doc.getString("CompanyName")
                                ?: doc.getString("name")
                                ?: doc.id
                        )
                    }
                    ?.sortedBy { it.displayName }
                    .orEmpty()


                detectCompaniesFromSubcollections(
                    onSuccess = { detectedOptions ->
                        allCompanies = (companiesFromDocuments + detectedOptions)
                            .distinctBy { it.id }
                            .sortedBy { it.displayName }

                        publishAccessibleCompanies()
                    },
                    onFailure = { detectError ->
                        if (companiesFromDocuments.isNotEmpty()) {
                            allCompanies = companiesFromDocuments
                            publishAccessibleCompanies()
                        } else {
                            _companyListState.value = CompanyListUiState.Error(
                                detectError.message ?: "Unable to detect companies"
                            )
                        }
                    }
                )
            }
    }

    private fun observeLoggedInUserPermissions(email: String?) {
        userAccessListener?.remove()
        userAccessListener = null

        val normalizedEmail = email?.trim().orEmpty()
        if (normalizedEmail.isBlank()) {
            allowedCompanyIds = emptySet()
            _companyListState.value = CompanyListUiState.Error("Please log in to load company access.")
            return
        }

        _companyListState.value = CompanyListUiState.Loading
        userAccessListener = firestore.collection(USERS_COLLECTION)
            .document(normalizedEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _companyListState.value = CompanyListUiState.Error(
                        error.message ?: "Unable to load user company access"
                    )
                    return@addSnapshotListener
                }

                val allowed = snapshot?.get("allowedCompanies")
                    ?.let { raw ->
                        (raw as? List<*>)?.mapNotNull { value ->
                            (value as? String)?.trim()?.takeIf { it.isNotBlank() }
                        }?.toSet()
                    }
                    ?: emptySet()

                allowedCompanyIds = allowed

                val rightsMap = snapshot?.get(STOCK_ACCESS_RIGHTS_FIELD) as? Map<*, *>
                val enabledRoutes = rightsMap
                    ?.mapNotNull { (key, value) ->
                        val route = (key as? String)?.let(StockAccessRights::routeFromRemoteKey)
                        val enabled = value as? Boolean
                        if (route != null && enabled == true) route else null
                    }
                    ?.toSet()
                    .orEmpty()

                _enabledStockAccessRoutes.value = enabledRoutes
                publishAccessibleCompanies()
            }
    }

    private fun detectCompaniesFromSubcollections(
        onSuccess: (List<CompanyOption>) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val tasks = COMPANY_SUBCOLLECTION_HINTS.map { subcollection ->
            firestore.collectionGroup(subcollection).limit(30).get()
        }

        Tasks.whenAllSuccess<QuerySnapshot>(tasks)
            .addOnSuccessListener { snapshots ->
                val detectedCompanyIds = snapshots
                    .flatMap { it.documents }
                    .mapNotNull { document ->
                        document.reference.parent.parent?.id
                    }
                    .distinct()
                    .sorted()

                val detectedOptions = detectedCompanyIds.map { companyId ->
                    CompanyOption(id = companyId, displayName = companyId)
                }
                onSuccess(detectedOptions)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    private fun publishAccessibleCompanies() {
        val allowedIds = allowedCompanyIds
        if (allowedIds == null) {
            _companyListState.value = CompanyListUiState.Loading
            return
        }


        if (allowedIds.isEmpty()) {
            _companyListState.value = CompanyListUiState.Error(
                "No companies are assigned to your account."
            )
            CompanyContext.updateSelectedCompany(
                context = getApplication(),
                companyId = ""
            )
            return
        }

        val filteredCompanies = allCompanies.filter { it.id in allowedIds }
        val missingCompanies = allowedIds
            .filterNot { allowedId -> allCompanies.any { it.id == allowedId } }
            .map { missingId -> CompanyOption(id = missingId, displayName = missingId) }
        val companies = (filteredCompanies + missingCompanies).sortedBy { it.displayName }

        if (companies.isEmpty()) {
            _companyListState.value = CompanyListUiState.Error(
                "No company records were found in your account."
            )
            CompanyContext.updateSelectedCompany(
                context = getApplication(),
                companyId = ""
            )
            return
        }
        val selected = selectedCompanyId.value
        val selectedExists = companies.any { it.id == selected }
        if (!selectedExists) {
            val fallbackCompanyId = if (selected.isBlank()) {
                val selectedAccountBookId = AccountBookContext.selectedAccountBookId.value.trim()
                companies.firstOrNull { it.id == selectedAccountBookId }?.id ?: companies.first().id
            } else {
                companies.first().id
            }
            CompanyContext.updateSelectedCompany(
                context = getApplication(),
                companyId = fallbackCompanyId
            )
        }
        _companyListState.value = CompanyListUiState.Success(companies)
    }

    fun selectCompany(companyId: String) {
        CompanyContext.updateSelectedCompany(getApplication(), companyId)
    }

    fun saveDocumentFormats(salesOrderFormat: String, stockAdjustmentFormat: String): Result<Unit> {
        val normalizedSo = salesOrderFormat.trim()
        val normalizedSt = stockAdjustmentFormat.trim()

        if (!DocumentNumberFormatStore.isValidFormat(normalizedSo)) {
            return Result.failure(IllegalArgumentException("Sales Order format must contain at least one 0"))
        }

        if (!DocumentNumberFormatStore.isValidFormat(normalizedSt)) {
            return Result.failure(IllegalArgumentException("Stock Take format must contain at least one 0"))
        }

        DocumentNumberFormatStore.updateFormats(
            context = getApplication(),
            salesOrderFormat = normalizedSo,
            stockAdjustmentFormat = normalizedSt
        )
        return Result.success(Unit)
    }

    fun saveAccessPasswords(adminPassword: String, stockPassword: String): Result<Unit> {
        val normalizedAdmin = adminPassword.trim()
        val normalizedStock = stockPassword.trim()

        if (normalizedAdmin.isBlank()) {
            return Result.failure(IllegalArgumentException("Admin password cannot be empty"))
        }

        if (normalizedStock.isBlank()) {
            return Result.failure(IllegalArgumentException("Stock password cannot be empty"))
        }

        if (normalizedAdmin == normalizedStock) {
            return Result.failure(
                IllegalArgumentException("Admin password and Stock User password cannot be same")
            )
        }

        AccessPasswordStore.updatePasswords(
            context = getApplication(),
            adminPassword = normalizedAdmin,
            stockPassword = normalizedStock
        )
        return Result.success(Unit)
    }

    fun updateStockAccessRight(route: String, enabled: Boolean): Result<Unit> {
        val email = auth.currentUser?.email?.trim().orEmpty()
        if (email.isBlank()) {
            return Result.failure(IllegalStateException("Please log in to update stock access rights"))
        }

        if (StockAccessRights.configurableRights.none { it.route == route }) {
            return Result.failure(IllegalArgumentException("Invalid route: $route"))
        }

        val updateValue: Any = if (enabled) true else FieldValue.delete()
        val remoteKey = StockAccessRights.remoteKeyForRoute(route)
        val updates = mutableMapOf<String, Any>(
            "$STOCK_ACCESS_RIGHTS_FIELD.$remoteKey" to updateValue
        )
        if (remoteKey != route) {
            updates["$STOCK_ACCESS_RIGHTS_FIELD.$route"] = FieldValue.delete()
        }
        firestore.collection(USERS_COLLECTION)
            .document(email)
            .update(updates as Map<String, Any>)
            .addOnFailureListener { error ->
                if (error.message?.contains("No document to update", ignoreCase = true) == true) {
                    val initialValue = if (enabled) true else null
                    val payload = mutableMapOf<String, Any>()
                    payload[STOCK_ACCESS_RIGHTS_FIELD] = if (initialValue == null) {
                        emptyMap<String, Any>()
                    } else {
                        mapOf(remoteKey to initialValue)
                    }
                    firestore.collection(USERS_COLLECTION).document(email).set(payload, com.google.firebase.firestore.SetOptions.merge())
                }
            }

        _enabledStockAccessRoutes.value = if (enabled) {
            _enabledStockAccessRoutes.value + route
        } else {
            _enabledStockAccessRoutes.value - route
        }
        return Result.success(Unit)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        userAccessListener?.remove()
        userAccessListener = null
        companiesListener?.remove()
        companiesListener = null
        super.onCleared()
    }

    private companion object {
        const val COMPANIES_COLLECTION = "Companies"
        const val USERS_COLLECTION = "Users"
        const val STOCK_ACCESS_RIGHTS_FIELD = "StockAccessRights"
        val COMPANY_SUBCOLLECTION_HINTS = listOf(
            "CreditorSummary",
            "DebtorSummary",
            "DailySalesSummary",
            "HourlySales",
            "ItemSummary",
            "MemberSummary",
            "StockList",
            "TopItems",
            "YearlySales",
            "MonthlySales",
            "SalesOverviewSummary",
            "salesOrders",
            "stockAdjustments"
        )
    }
}