package com.example.intern_stockmate.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intern_stockmate.model.LocationInfo
import com.example.intern_stockmate.model.StockItem
import com.example.intern_stockmate.model.UomInfo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class StockViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _allItems = MutableStateFlow<List<StockItem>>(emptyList())
    val allItems: StateFlow<List<StockItem>> = _allItems.asStateFlow()

    val searchQuery = MutableStateFlow("")
    private val _selectedLocation = MutableStateFlow("")

    private val _isInvalidSearch = MutableStateFlow(false)
    val isInvalidSearch: StateFlow<Boolean> = _isInvalidSearch.asStateFlow()

    private val _stockState = MutableStateFlow<StockUiState>(StockUiState.Loading)
    val stockState: StateFlow<StockUiState> = _stockState.asStateFlow()

    val filteredItems: StateFlow<List<StockItem>> = combine(
        _allItems,
        searchQuery,
        _selectedLocation
    ) { items, query, location ->
        val trimmed = query.trim()
        val filtered = items.filter { item ->
            val matchQuery = trimmed.isBlank() ||
                    item.itemCode.contains(trimmed, ignoreCase = true) ||
                    item.description.contains(trimmed, ignoreCase = true) ||
                    item.desc2.orEmpty().contains(trimmed, ignoreCase = true) ||
                    item.uomList.any { it.barCode.orEmpty().contains(trimmed, ignoreCase = true) }

            val matchLocation = location.isBlank() || item.locationList.any { it.location == location }
            matchQuery && matchLocation
        }

        _isInvalidSearch.value = trimmed.isNotBlank() && filtered.isEmpty()
        filtered
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        getStockList()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onLocationSelected(location: String) {
        _selectedLocation.value = location
    }

    fun getStockList() {
        _stockState.value = StockUiState.Loading

        firestore.collection(COLLECTION_NAME)
            .document(DOCUMENT_NAME)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    _stockState.value = StockUiState.Error("Stock list document was not found.")
                    return@addOnSuccessListener
                }

                val rawItems = snapshot.get("stockItems") as? List<Map<String, Any?>>
                if (rawItems.isNullOrEmpty()) {
                    _stockState.value = StockUiState.Error("No stock items found in Firebase.")
                    return@addOnSuccessListener
                }

                val parsed = rawItems.map { it.toStockItem() }
                val grouped = groupStockItems(parsed)
                _allItems.value = grouped
                _stockState.value = StockUiState.Success(grouped)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to load stock list", error)
                _stockState.value = StockUiState.Error(
                    error.message ?: "Failed to load stock list from Firebase."
                )
            }
    }

    private fun groupStockItems(rawList: List<StockItem>): List<StockItem> {
        return rawList
            .groupBy { it.itemCode }
            .map { (_, records) ->
                val base = records.first()

                val uniqueUoms = records
                    .distinctBy { it.uom }
                    .map {
                        UomInfo(
                            uom = it.uom,
                            rate = it.rate ?: 1.0,
                            price1 = it.price,
                            price2 = it.price2 ?: 0.0,
                            price3 = it.price3 ?: 0.0,
                            price4 = it.price4 ?: 0.0,
                            price5 = it.price5 ?: 0.0,
                            price6 = it.price6 ?: 0.0,
                            barCode = it.barCode
                        )
                    }

                val uniqueLocations = records
                    .filter { it.location.isNotBlank() }
                    .distinctBy { it.location }
                    .map { LocationInfo(it.location, it.balQty) }

                base.copy(
                    uomList = uniqueUoms,
                    locationList = uniqueLocations,
                    balQty = uniqueLocations.sumOf { it.qty }
                )
            }
            .sortedBy { it.itemCode }
    }

    private fun Map<String, Any?>.toStockItem(): StockItem {
        return StockItem(
            itemCode = string("itemCode"),
            description = string("description"),
            desc2 = nullableString("desc2"),
            isActive = nullableString("isActive"),
            itemGroup = nullableString("itemGroup"),
            uom = string("uom", "UNIT"),
            rate = nullableDouble("rate"),
            price = double("price"),
            price2 = nullableDouble("price2"),
            price3 = nullableDouble("price3"),
            price4 = nullableDouble("price4"),
            price5 = nullableDouble("price5"),
            price6 = nullableDouble("price6"),
            shelf = nullableString("shelf"),
            barCode = nullableString("barCode"),
            balQty = int("balQty"),
            location = string("location"),
            itemPhoto = nullableString("itemPhoto")
        )
    }

    private fun Map<String, Any?>.string(key: String, default: String = ""): String =
        when (val value = this[key]) {
            is String -> value
            is Number -> value.toString()
            else -> default
        }

    private fun Map<String, Any?>.nullableString(key: String): String? =
        when (val value = this[key]) {
            is String -> value
            is Number -> value.toString()
            else -> null
        }

    private fun Map<String, Any?>.double(key: String, default: Double = 0.0): Double =
        when (val value = this[key]) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: default
            else -> default
        }

    private fun Map<String, Any?>.nullableDouble(key: String): Double? =
        when (val value = this[key]) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }

    private fun Map<String, Any?>.int(key: String, default: Int = 0): Int =
        when (val value = this[key]) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: default
            else -> default
        }

    private companion object {
        const val TAG = "StockViewModel"
        const val COLLECTION_NAME = "StockList"
        const val DOCUMENT_NAME = "Current"
    }
}

sealed interface StockUiState {
    data object Loading : StockUiState
    data class Success(val data: List<StockItem>) : StockUiState
    data class Error(val message: String) : StockUiState
}