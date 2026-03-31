package com.example.intern_stockmate.viewModel

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import com.example.intern_stockmate.model.DailySales
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.YearMonth

class DailySalesViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    var selectedTabIndex = mutableIntStateOf(0)
        private set

    private val _dailySalesState = MutableStateFlow<DailySalesUiState>(DailySalesUiState.Loading)
    val dailySalesState: StateFlow<DailySalesUiState> = _dailySalesState.asStateFlow()

    init {
        fetchDailySales()
    }

    fun selectTab(index: Int) {
        selectedTabIndex.intValue = index
    }

    fun fetchDailySales(yearMonth: YearMonth = YearMonth.now()) {
        _dailySalesState.value = DailySalesUiState.Loading

        firestore.collection(COLLECTION_NAME)
            .document(DOCUMENT_NAME)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    _dailySalesState.value = DailySalesUiState.Error(
                        "Daily sales document was not found."
                    )
                    return@addOnSuccessListener
                }

                val monthlyData = (snapshot.get("monthlyData") as? List<*>)
                    .orEmpty()
                    .mapNotNull { entry ->
                        val item = entry as? Map<*, *> ?: return@mapNotNull null
                        DailySales(
                            day = item.intValue("Day"),
                            cashSales = item.doubleValue("CashSales"),
                            invoiceSales = item.doubleValue("InvoiceSales"),
                            posSales = item.doubleValue("PosSales")
                        )
                    }
                    .sortedBy { it.day }

                _dailySalesState.value = DailySalesUiState.Success(
                    data = monthlyData,
                    yearMonth = yearMonth,
                    lastUpdate = snapshot.get("lastUpdate")?.toString().orEmpty()
                )
            }
            .addOnFailureListener { error ->
                _dailySalesState.value = DailySalesUiState.Error(
                    error.message ?: "Failed to load daily sales from Firestore."
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
        const val COLLECTION_NAME = "MonthlySales"
        const val DOCUMENT_NAME = "Current"
    }
}

sealed interface DailySalesUiState {
    data object Loading : DailySalesUiState
    data class Success(
        val data: List<DailySales>,
        val yearMonth: YearMonth,
        val lastUpdate: String
    ) : DailySalesUiState
    data class Error(val message: String) : DailySalesUiState
}