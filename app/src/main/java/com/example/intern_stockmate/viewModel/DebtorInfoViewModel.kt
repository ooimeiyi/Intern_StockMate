package com.example.intern_stockmate.viewModel

import androidx.lifecycle.ViewModel
import com.example.intern_stockmate.data.CompanyContext
import com.example.intern_stockmate.model.DebtorResponse
import com.example.intern_stockmate.model.DebtorSummary
import com.example.intern_stockmate.model.OutstandingDebtorItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DebtorInfoViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _debtorState = MutableStateFlow<DebtorInfoUiState>(DebtorInfoUiState.Loading)
    val debtorState: StateFlow<DebtorInfoUiState> = _debtorState.asStateFlow()

    init {
        fetchDebtorSummary()
    }

    fun fetchDebtorSummary() {
        _debtorState.value = DebtorInfoUiState.Loading

        CompanyContext.collection(firestore, COLLECTION_NAME)
            .document(DOCUMENT_NAME)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    _debtorState.value = DebtorInfoUiState.Error(
                        "Debtor summary document was not found."
                    )
                    return@addOnSuccessListener
                }

                val summaryMap = snapshot.get("summary") as? Map<*, *> ?: emptyMap<String, Any>()
                val outstandingList = (snapshot.get("outstandingList") as? List<*>)
                    .orEmpty()
                    .mapNotNull { entry ->
                        val item = entry as? Map<*, *> ?: return@mapNotNull null
                        OutstandingDebtorItem(
                            billCount = item.intValue("BillCount"),
                            companyName = item.stringValue("CompanyName"),
                            debtorCode = item.stringValue("DebtorCode"),
                            outstandingAmount = item.doubleValue("OutstandingAmount")
                        )
                    }

                val response = DebtorResponse(
                    summary = DebtorSummary(
                        activeDebtor = summaryMap.intValue("ActiveDebtor"),
                        nonActiveDebtor = summaryMap.intValue("NonActiveDebtor"),
                        totalCountOutstanding = summaryMap.intValue("TotalCountOutstanding"),
                        totalSumOutstanding = summaryMap.doubleValue("TotalSumOutstanding")
                    ),
                    outstandingList = outstandingList
                )

                _debtorState.value = DebtorInfoUiState.Success(response)
            }
            .addOnFailureListener { error ->
                _debtorState.value = DebtorInfoUiState.Error(
                    error.message ?: "Failed to load debtor info from Firestore."
                )
            }
    }

    private fun Map<*, *>.intValue(key: String): Int {
        val value = get(key) ?: return 0
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: 0
            else -> 0
        }
    }

    private fun Map<*, *>.doubleValue(key: String): Double {
        val value = get(key) ?: return 0.0
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun Map<*, *>.stringValue(key: String): String = get(key)?.toString().orEmpty()

    private companion object {
        const val COLLECTION_NAME = "DebtorSummary"
        const val DOCUMENT_NAME = "Current"
    }
}

sealed interface DebtorInfoUiState {
    data object Loading : DebtorInfoUiState
    data class Success(val data: DebtorResponse) : DebtorInfoUiState
    data class Error(val message: String) : DebtorInfoUiState
}