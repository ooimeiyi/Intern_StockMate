package com.example.intern_stockmate.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.intern_stockmate.data.CompanyContext
import com.example.intern_stockmate.data.DocumentNumberFormatStore
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
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

    private val _companyListState = MutableStateFlow<CompanyListUiState>(CompanyListUiState.Loading)
    val companyListState: StateFlow<CompanyListUiState> = _companyListState.asStateFlow()

    val selectedCompanyId: StateFlow<String> = CompanyContext.selectedCompanyId

    val salesOrderFormat: StateFlow<String> = DocumentNumberFormatStore.salesOrderFormat
    val stockAdjustmentFormat: StateFlow<String> = DocumentNumberFormatStore.stockAdjustmentFormat

    private var companiesListener: ListenerRegistration? = null

    init {
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
                        val mergedCompanies = (companiesFromDocuments + detectedOptions)
                            .distinctBy { it.id }
                            .sortedBy { it.displayName }

                        if (mergedCompanies.isEmpty()) {
                            _companyListState.value = CompanyListUiState.Error(
                                "No company documents found. If your companies only have subcollections, add at least one field to each Companies/{companyId} document."
                            )
                            return@detectCompaniesFromSubcollections
                        }

                        applyCompanySelection(mergedCompanies)
                    },
                    onFailure = { detectError ->
                        if (companiesFromDocuments.isNotEmpty()) {
                            applyCompanySelection(companiesFromDocuments)
                        } else {
                            _companyListState.value = CompanyListUiState.Error(
                                detectError.message ?: "Unable to detect companies"
                            )
                        }
                    }
                )
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

    private fun applyCompanySelection(companies: List<CompanyOption>) {
        val selected = selectedCompanyId.value
        val selectedExists = companies.any { it.id == selected }
        if (!selectedExists && selected.isNotBlank()) {
            CompanyContext.updateSelectedCompany(
                context = getApplication(),
                companyId = ""
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
            return Result.failure(IllegalArgumentException("Stock Adjustment format must contain at least one 0"))
        }

        DocumentNumberFormatStore.updateFormats(
            context = getApplication(),
            salesOrderFormat = normalizedSo,
            stockAdjustmentFormat = normalizedSt
        )
        return Result.success(Unit)
    }

    override fun onCleared() {
        companiesListener?.remove()
        companiesListener = null
        super.onCleared()
    }

    private companion object {
        const val COMPANIES_COLLECTION = "Companies"
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