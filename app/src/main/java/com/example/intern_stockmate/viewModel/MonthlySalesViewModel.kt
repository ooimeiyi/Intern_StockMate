package com.example.intern_stockmate.viewModel

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import com.example.intern_stockmate.model.MonthlySales
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Year

class MonthlySalesViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    var selectedTabIndex = mutableIntStateOf(0)
        private set

    private val _monthlySalesState = MutableStateFlow<MonthlySalesUiState>(MonthlySalesUiState.Loading)
    val monthlySalesState: StateFlow<MonthlySalesUiState> = _monthlySalesState.asStateFlow()

    init {
        fetchMonthlySales()
    }

    fun selectTab(index: Int) {
        selectedTabIndex.intValue = index
    }

    fun fetchMonthlySales(year: Year = Year.now()) {
        _monthlySalesState.value = MonthlySalesUiState.Loading

        firestore.collection(COLLECTION_NAME)
            .document(DOCUMENT_NAME)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    _monthlySalesState.value = MonthlySalesUiState.Error(
                        "Monthly sales document was not found."
                    )
                    return@addOnSuccessListener
                }

                val yearlyData = (snapshot.get("yearlyData") as? List<*>)
                    .orEmpty()
                    .mapNotNull { entry ->
                        val item = entry as? Map<*, *> ?: return@mapNotNull null
                        MonthlySales(
                            month = item.intValue("Month"),
                            cashSales = item.doubleValue("CashSales"),
                            invoiceSales = item.doubleValue("InvoiceSales"),
                            posSales = item.doubleValue("PosSales")
                        )
                    }
                    .sortedBy { it.month }

                _monthlySalesState.value = MonthlySalesUiState.Success(
                    data = yearlyData,
                    year = year,
                    lastUpdate = snapshot.get("lastUpdate")?.toString().orEmpty()
                )
            }
            .addOnFailureListener { error ->
                _monthlySalesState.value = MonthlySalesUiState.Error(
                    error.message ?: "Failed to load monthly sales from Firestore."
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

    private companion object {
        const val COLLECTION_NAME = "YearlySales"
        const val DOCUMENT_NAME = "Current"
    }
}

sealed interface MonthlySalesUiState {
    data object Loading : MonthlySalesUiState
    data class Success(
        val data: List<MonthlySales>,
        val year: Year,
        val lastUpdate: String
    ) : MonthlySalesUiState
    data class Error(val message: String) : MonthlySalesUiState
}