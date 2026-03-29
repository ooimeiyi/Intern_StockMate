package com.example.intern_stockmate.viewModel

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import com.example.intern_stockmate.model.TopSalesItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SalesRankViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    var selectedTabIndex = mutableIntStateOf(0)
        private set

    private val _salesRankState = MutableStateFlow<SalesRankUiState>(SalesRankUiState.Loading)
    val salesRankState: StateFlow<SalesRankUiState> = _salesRankState.asStateFlow()

    init {
        fetchTopItems()
    }

    fun selectTab(index: Int) {
        selectedTabIndex.intValue = index
    }

    fun fetchTopItems() {
        _salesRankState.value = SalesRankUiState.Loading

        firestore.collection(COLLECTION_NAME)
            .document(DOCUMENT_NAME)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    _salesRankState.value = SalesRankUiState.Error("Top items document was not found.")
                    return@addOnSuccessListener
                }

                _salesRankState.value = SalesRankUiState.Success(
                    lastUpdate = snapshot.get("lastUpdate")?.toString().orEmpty(),
                    posItems = snapshot.topItems("posTopItems"),
                    invoiceItems = snapshot.topItems("ivTopItems"),
                    cashSalesItems = snapshot.topItems("csTopItems")
                )
            }
            .addOnFailureListener { error ->
                _salesRankState.value = SalesRankUiState.Error(
                    error.message ?: "Failed to load sales rank from Firestore."
                )
            }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.topItems(key: String): List<TopSalesItem> {
        return (get(key) as? List<*>)
            .orEmpty()
            .mapNotNull { entry ->
                val item = entry as? Map<*, *> ?: return@mapNotNull null
                TopSalesItem(
                    code = item.stringValue("ItemCode"),
                    description = item.stringValue("Description"),
                    qty = item.doubleValue("TotalQty"),
                    sales = item.doubleValue("TotalSales")
                )
            }
            .sortedByDescending { it.sales }
    }

    private fun Map<*, *>.stringValue(key: String): String =
        get(key)?.toString().orEmpty()

    private fun Map<*, *>.doubleValue(key: String): Double {
        val value = get(key) ?: return 0.0
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private companion object {
        const val COLLECTION_NAME = "TopItems"
        const val DOCUMENT_NAME = "Current"
    }
}

sealed interface SalesRankUiState {
    data object Loading : SalesRankUiState
    data class Success(
        val lastUpdate: String,
        val posItems: List<TopSalesItem>,
        val invoiceItems: List<TopSalesItem>,
        val cashSalesItems: List<TopSalesItem>
    ) : SalesRankUiState

    data class Error(val message: String) : SalesRankUiState
}