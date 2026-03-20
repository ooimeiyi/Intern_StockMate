package com.example.intern_stockmate.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intern_stockmate.model.LocationInfo
import com.example.intern_stockmate.model.StockItem
import com.example.intern_stockmate.model.UomInfo
import com.example.intern_stockmate.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StockViewModel(
    private val repository: StockRepository
) : ViewModel() {

    /* -------------------- STOCK & SEARCH -------------------- */
    constructor() : this(StockRepository())

    private val _allItems = MutableStateFlow<List<StockItem>>(emptyList())
    val allItems: StateFlow<List<StockItem>> = _allItems.asStateFlow()

    val searchQuery = MutableStateFlow("")

    private val _locations = MutableStateFlow<List<String>>(emptyList())
    val locations: StateFlow<List<String>> = _locations.asStateFlow()

    private val _selectedLocation = MutableStateFlow("")
    val selectedLocation: StateFlow<String> = _selectedLocation.asStateFlow()

    private val _isInvalidSearch = MutableStateFlow(false)
    val isInvalidSearch: StateFlow<Boolean> = _isInvalidSearch

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onLocationSelected(location: String) {
        _selectedLocation.value = location
    }



    /* -------------------- FILTERED ITEMS -------------------- */

    /*val filteredItems: StateFlow<List<StockItem>> = combine(
        _allItems,
        searchQuery,
        _selectedLocation
    ) { items, query, location ->

        val trimmedQuery = query.trim()

        items.filter { item ->
            val matchesQuery =
                trimmedQuery.isBlank() ||
                        item.itemCode.contains(trimmedQuery, true) ||
                        item.description.contains(trimmedQuery, true) ||
                        item.desc2.orEmpty().contains(trimmedQuery, true) ||
                        item.uomList.any {
                            it.barCode.orEmpty().contains(trimmedQuery, true)
                        }

            val matchesLocation =
                location.isBlank() ||
                        item.locationList.any { it.location == location }

            Log.d(
                "DEBUG_FILTER",
                "Item=${item.itemCode}, query=$trimmedQuery, location=$location"
            )

            matchesQuery && matchesLocation
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

     */

    val filteredItems: StateFlow<List<StockItem>> = combine(
        _allItems,
        searchQuery,
        _selectedLocation
    ) { items, query, location ->

        val trimmedQuery = query.trim()

        val filtered = items.filter { item ->

            val matchesQuery =
                trimmedQuery.isBlank() ||
                        item.itemCode.contains(trimmedQuery, true) ||
                        item.description.contains(trimmedQuery, true) ||
                        item.desc2.orEmpty().contains(trimmedQuery, true) ||
                        item.uomList.any { it.barCode.orEmpty().contains(trimmedQuery, true)
                        }

            val matchesLocation =
                location.isBlank() ||
                        item.locationList.any { it.location == location }

            matchesQuery && matchesLocation

        }

        _isInvalidSearch.value = trimmedQuery.isNotBlank() && filtered.isEmpty()

        filtered

    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /* -------------------- FETCH DATA -------------------- */
    fun fetchStockItems() {
        viewModelScope.launch {
            try {
                val rawItems = repository.getStock()
                _allItems.value = groupStockItems(rawItems)
            } catch (e: Exception) {
                Log.e("StockViewModel", "Failed to load stock from Firestore", e)
                _allItems.value = emptyList()
            }
        }
    }

    fun fetchLocations() {
        viewModelScope.launch {
            try {
                val stockItems = repository.getStock()
                _locations.value = stockItems
                    .mapNotNull { it.location }
                    //.filter { it.isNotBlank() && it != "N/A" }
                    .distinct()
                _locations.value = repository.getLocations()
            } catch (e: Exception) {
                Log.e("StockViewModel", "Failed to load locations from Firestore", e)
                _locations.value = emptyList()
            }
        }
    }

    /* -------------------- GROUP ITEMS -------------------- */
    private fun groupStockItems(rawList: List<StockItem>): List<StockItem> {
        return rawList.groupBy { it.itemCode }.map { (_, records) ->
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
                //.filter { it.location.isNotBlank() && it.location != "N/A" }
                .distinctBy { it.location }
                .map {
                    LocationInfo(it.location, it.balQty)
                }

            base.copy(
                uomList = uniqueUoms,
                locationList = uniqueLocations,
                balQty = uniqueLocations.sumOf { it.qty }
            )
        }
    }

    /* -------------------- UPDATE QTY -------------------- */
    fun updateItemQuantityWithDiff(
        itemCode: String,
        location: String,
        diffQty: Int?
    ) {
        val safeDiffQty = diffQty ?: 0

        _allItems.value = _allItems.value.map { item ->
            if (item.itemCode == itemCode) {
                val updatedLocations = item.locationList.map { loc ->
                    if (loc.location == location) loc.copy(diffQty = safeDiffQty) else loc
                }

                item.copy(
                    locationList = updatedLocations,
                    // Note: Ensure your StockItem model has balQty and it's var or copyable
                    balQty = updatedLocations.sumOf { it.qty }
                )
            } else {
                item
            }
        }
    }

    fun updateAllItems(items: List<StockItem>) {
        _allItems.value = items
    }
}