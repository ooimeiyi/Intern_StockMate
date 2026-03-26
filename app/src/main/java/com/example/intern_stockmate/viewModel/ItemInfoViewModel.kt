package com.example.intern_stockmate.viewModel

import androidx.lifecycle.ViewModel
import com.example.intern_stockmate.model.ItemInfo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ItemInfoViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _itemState = MutableStateFlow<ItemInfoUiState>(ItemInfoUiState.Loading)
    val itemState: StateFlow<ItemInfoUiState> = _itemState.asStateFlow()

    init {
        fetchItemSummary()
    }

    fun fetchItemSummary() {
        _itemState.value = ItemInfoUiState.Loading

        firestore.collection(COLLECTION_NAME)
            .document(DOCUMENT_NAME)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    _itemState.value = ItemInfoUiState.Error("Item summary document was not found.")
                    return@addOnSuccessListener
                }

                _itemState.value = ItemInfoUiState.Success(
                    ItemInfo(
                        allItem = snapshot.intValue("TotalItems"),
                        active = snapshot.intValue("ActiveItems"),
                        nonActive = snapshot.intValue("InactiveItems"),
                        stockControl = snapshot.intValue("StockControlOn"),
                        nonStockControl = snapshot.intValue("StockControlOff"),
                        itemGroupCount = snapshot.intValue("TotalGroups"),
                        itemTypeCount = snapshot.intValue("TotalTypes"),
                        negativeQty = snapshot.intValue("NegativeStockCount"),
                        lastUpdate = snapshot.get("lastUpdate")?.toString().orEmpty()
                    )
                )
            }
            .addOnFailureListener { error ->
                _itemState.value = ItemInfoUiState.Error(
                    error.message ?: "Failed to load item info from Firestore."
                )
            }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.intValue(key: String): Int {
        val value = get(key) ?: return 0
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: 0
            else -> 0
        }
    }

    private companion object {
        const val COLLECTION_NAME = "ItemSummary"
        const val DOCUMENT_NAME = "Current"
    }
}

sealed interface ItemInfoUiState {
    data object Loading : ItemInfoUiState
    data class Success(val data: ItemInfo) : ItemInfoUiState
    data class Error(val message: String) : ItemInfoUiState
}