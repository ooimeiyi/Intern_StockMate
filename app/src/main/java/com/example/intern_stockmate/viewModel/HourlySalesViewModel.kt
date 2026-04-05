package com.example.intern_stockmate.viewModel

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import com.example.intern_stockmate.data.CompanyContext
import com.example.intern_stockmate.model.HourlySales
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class HourlySalesViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    var selectedTabIndex = mutableIntStateOf(0)
        private set

    private val _hourlySalesState = MutableStateFlow<HourlySalesUiState>(HourlySalesUiState.Loading)
    val hourlySalesState: StateFlow<HourlySalesUiState> = _hourlySalesState.asStateFlow()

    init {
        fetchHourlySales()
    }

    fun selectTab(index: Int) {
        selectedTabIndex.intValue = index
    }

    fun fetchHourlySales(date: LocalDate = LocalDate.now()) {
        _hourlySalesState.value = HourlySalesUiState.Loading

        CompanyContext.collection(firestore, COLLECTION_NAME)
            .document(DOCUMENT_NAME)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    _hourlySalesState.value = HourlySalesUiState.Error(
                        "Hourly sales document was not found."
                    )
                    return@addOnSuccessListener
                }

                val hourlyData = (snapshot.get("hourlyData") as? List<*>)
                    .orEmpty()
                    .mapNotNull { entry ->
                        val item = entry as? Map<*, *> ?: return@mapNotNull null
                        HourlySales(
                            hour = item.intValue("hour"),
                            cashSales = item.doubleValue("cashSales"),
                            invoiceSales = item.doubleValue("invoiceSales"),
                            posSales = item.doubleValue("posSales")
                        )
                    }
                    .sortedBy { it.hour }

                _hourlySalesState.value = HourlySalesUiState.Success(hourlyData)
            }
            .addOnFailureListener { error ->
                _hourlySalesState.value = HourlySalesUiState.Error(
                    error.message ?: "Failed to load hourly sales from Firestore."
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
        const val COLLECTION_NAME = "HourlySales"
        const val DOCUMENT_NAME = "Current"
    }
}

sealed interface HourlySalesUiState {
    data object Loading : HourlySalesUiState
    data class Success(val data: List<HourlySales>) : HourlySalesUiState
    data class Error(val message: String) : HourlySalesUiState
}