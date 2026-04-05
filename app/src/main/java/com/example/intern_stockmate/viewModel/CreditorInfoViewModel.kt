package com.example.intern_stockmate.viewModel

import androidx.lifecycle.ViewModel
import com.example.intern_stockmate.data.CompanyContext
import com.example.intern_stockmate.model.CreditorResponse
import com.example.intern_stockmate.model.CreditorSummary
import com.example.intern_stockmate.model.OutstandingCreditorItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CreditorInfoViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _creditorState = MutableStateFlow<CreditInfoUiState>(CreditInfoUiState.Loading)
    val creditorState: StateFlow<CreditInfoUiState> = _creditorState.asStateFlow()

    init {
        fetchCreditorSummary()
    }

    fun fetchCreditorSummary() {
        _creditorState.value = CreditInfoUiState.Loading

        CompanyContext.collection(firestore, COLLECTION_NAME)
            .document(DOCUMENT_NAME)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    _creditorState.value = CreditInfoUiState.Error(
                        "Creditor summary document was not found."
                    )
                    return@addOnSuccessListener
                }

                val summaryMap = snapshot.get("summary") as? Map<*, *> ?: emptyMap<String, Any>()
                val outstandingList = (snapshot.get("outstandingList") as? List<*>)
                    .orEmpty()
                    .mapNotNull { entry ->
                        val item = entry as? Map<*, *> ?: return@mapNotNull null
                        OutstandingCreditorItem(
                            billCount = item.intValue("BillCount"),
                            companyName = item.stringValue("CompanyName"),
                            creditorCode = item.stringValue("CreditorCode"),
                            outstandingAmount = item.doubleValue("OutstandingAmount")
                        )
                    }

                val response = CreditorResponse(
                    summary = CreditorSummary(
                        activeCreditor = summaryMap.intValue("ActiveCreditor"),
                        nonActiveCreditor = summaryMap.intValue("NonActiveCreditor"),
                        totalCountOutstanding = summaryMap.intValue("TotalCountOutstanding"),
                        totalSumOutstanding = summaryMap.doubleValue("TotalSumOutstanding")
                    ),
                    outstandingList = outstandingList
                )

                _creditorState.value = CreditInfoUiState.Success(response)
            }
            .addOnFailureListener { error ->
                _creditorState.value = CreditInfoUiState.Error(
                    error.message ?: "Failed to load creditor info from Firestore."
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
        const val COLLECTION_NAME = "CreditorSummary"
        const val DOCUMENT_NAME = "Current"
    }
}

sealed interface CreditInfoUiState {
    data object Loading : CreditInfoUiState
    data class Success(val data: CreditorResponse) : CreditInfoUiState
    data class Error(val message: String) : CreditInfoUiState
}