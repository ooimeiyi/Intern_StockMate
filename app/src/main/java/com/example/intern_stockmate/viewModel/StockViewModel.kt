package com.example.intern_stockmate.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intern_stockmate.data.CompanyContext
import com.example.intern_stockmate.model.LocationInfo
import com.example.intern_stockmate.model.StockItem
import com.example.intern_stockmate.model.UomInfo
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StockViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _allItems = MutableStateFlow<List<StockItem>>(emptyList())
    val allItems: StateFlow<List<StockItem>> = _allItems.asStateFlow()

    val searchQuery = MutableStateFlow("")
    private val _selectedLocation = MutableStateFlow("")

    val selectedLocation: StateFlow<String> = _selectedLocation.asStateFlow()

    private val _locations = MutableStateFlow<List<String>>(emptyList())
    val locations: StateFlow<List<String>> = _locations.asStateFlow()

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
        viewModelScope.launch {
            CompanyContext.selectedCompanyId.collect { companyId ->
                if (companyId.isBlank()) {
                    _allItems.value = emptyList()
                    _locations.value = emptyList()
                    _selectedLocation.value = ""
                    _stockState.value = StockUiState.Error("No company selected.")
                } else {
                    getStockList()
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onLocationSelected(location: String) {
        _selectedLocation.value = location
    }

    fun clearLocationSelection() {
        _selectedLocation.value = ""
    }

    fun getStockList() {
        _stockState.value = StockUiState.Loading

        CompanyContext.collection(firestore, COLLECTION_NAME)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    clearStockData()
                    _stockState.value = StockUiState.Error("No stock items found in Firebase.")
                    return@addOnSuccessListener
                }

                val parsed = querySnapshot.documents.map { it.toStockItem() }
                    .filter { it.itemCode.isNotBlank() }

                if (parsed.isEmpty()) {
                    clearStockData()
                    _stockState.value = StockUiState.Error("Stock documents were found, but item data is empty.")
                    return@addOnSuccessListener
                }

                _allItems.value = parsed
                _locations.value = parsed
                    .flatMap { item -> item.locationList.map { it.location } }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                if (_selectedLocation.value.isNotBlank() && _selectedLocation.value !in _locations.value) {
                    _selectedLocation.value = ""
                }
                _stockState.value = StockUiState.Success(parsed)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to load stock list", error)
                clearStockData()
                _stockState.value = StockUiState.Error(
                    error.message ?: "Failed to load stock list from Firebase."
                )
            }
    }

    private fun clearStockData() {
        _allItems.value = emptyList()
        _locations.value = emptyList()
        _selectedLocation.value = ""
    }

    private fun DocumentSnapshot.toStockItem(): StockItem {
        val payload = data.orEmpty()

        val uomMaps = payload["uoms"] as? List<*>
        val uomList = uomMaps.orEmpty().mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            UomInfo(
                uom = map.string("uom", "UNIT"),
                rate = map.double("rate", 1.0),
                price1 = map.double("price"),
                price2 = map.double("price2"),
                price3 = map.double("price3"),
                price4 = map.double("price4"),
                price5 = map.double("price5"),
                price6 = map.double("price6"),
                barCode = map.nullableString("barCode")
            )
        }.distinctBy { it.uom.uppercase() }

        val locationList = uomMaps.orEmpty().mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val location = map.string("location")
            if (location.isBlank()) return@mapNotNull null
            LocationInfo(
                location = location,
                qty = map.int("balQty")
            )
        }.distinctBy { it.location }

        return StockItem(
            itemCode = payload.string("itemCode").ifBlank { id },
            description = payload.string("description"),
            desc2 = payload.nullableString("desc2"),
            isActive = payload.nullableString("isActive"),
            itemGroup = payload.nullableString("itemGroup"),
            uom = uomList.firstOrNull()?.uom ?: "UNIT",
            rate = uomList.firstOrNull()?.rate ?: 1.0,
            price = uomList.firstOrNull()?.price1 ?: 0.0,
            price2 = uomList.firstOrNull()?.price2 ?: 0.0,
            price3 = uomList.firstOrNull()?.price3 ?: 0.0,
            price4 = uomList.firstOrNull()?.price4 ?: 0.0,
            price5 = uomList.firstOrNull()?.price5 ?: 0.0,
            price6 = uomList.firstOrNull()?.price6 ?: 0.0,
            shelf = uomMaps.orEmpty().firstMapValue("shelf"),
            barCode = uomMaps.orEmpty().firstMapValue("barCode"),
            balQty = locationList.sumOf { it.qty },
            location = locationList.firstOrNull()?.location.orEmpty(),
            itemPhoto = payload.nullableString("itemPhoto"),
            uomList = uomList,
            locationList = locationList
        )
    }

    private fun Map<*, *>.string(key: String, default: String = ""): String =
        when (val value = this[key]) {
            is String -> value
            is Number -> value.toString()
            else -> default
        }

    private fun Map<*, *>.nullableString(key: String): String? =
        when (val value = this[key]) {
            is String -> value
            is Number -> value.toString()
            else -> null
        }

    private fun Map<*, *>.double(key: String, default: Double = 0.0): Double =
        when (val value = this[key]) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: default
            else -> default
        }

    private fun Map<*, *>.int(key: String, default: Int = 0): Int =
        when (val value = this[key]) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: default
            else -> default
        }

    private fun List<*>.firstMapValue(key: String): String? =
        firstNotNullOfOrNull { (it as? Map<*, *>)?.nullableString(key) }


    private companion object {
        const val TAG = "StockViewModel"
        const val COLLECTION_NAME = "StockList"
    }
}

sealed interface StockUiState {
    data object Loading : StockUiState
    data class Success(val data: List<StockItem>) : StockUiState
    data class Error(val message: String) : StockUiState
}