package com.example.intern_stockmate.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.intern_stockmate.data.CompanyContext
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

                if (companiesFromDocuments.isNotEmpty()) {
                    applyCompanySelection(companiesFromDocuments)
                    return@addSnapshotListener
                }

                detectCompaniesFromSubcollections()
            }
    }

    private fun detectCompaniesFromSubcollections() {
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

                if (detectedCompanyIds.isEmpty()) {
                    _companyListState.value = CompanyListUiState.Error(
                        "No company documents found. If your companies only have subcollections, add at least one field to each Companies/{companyId} document."
                    )
                    return@addOnSuccessListener
                }

                val detectedOptions = detectedCompanyIds.map { companyId ->
                    CompanyOption(id = companyId, displayName = companyId)
                }
                applyCompanySelection(detectedOptions)
            }
            .addOnFailureListener { error ->
                _companyListState.value = CompanyListUiState.Error(
                    error.message ?: "Unable to detect companies"
                )
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