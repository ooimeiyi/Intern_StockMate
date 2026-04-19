package com.example.intern_stockmate.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.intern_stockmate.data.CompanyContext
import com.example.intern_stockmate.model.LocationInfo
import com.example.intern_stockmate.model.StockItem
import com.example.intern_stockmate.model.UomInfo
import com.example.intern_stockmate.model.UomLocationInfo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StockViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : ViewModel() {
    private val gson = Gson()

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
    private val _stockLastUpdate = MutableStateFlow("")
    val stockLastUpdate: StateFlow<String> = _stockLastUpdate.asStateFlow()

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
            .document(METADATA_DOCUMENT_ID)
            .get()
            .addOnSuccessListener { metadataDocument ->
                if (!metadataDocument.exists()) {
                    clearStockData()
                    _stockState.value = StockUiState.Error("Stock list was not found.")
                    return@addOnSuccessListener
                }

                val storagePath = metadataDocument.getString("storagePath").orEmpty().trim()
                if (storagePath.isBlank()) {
                    clearStockData()
                    _stockState.value = StockUiState.Error("Stock list is missing storagePath.")
                    return@addOnSuccessListener
                }

                storage.reference.child(storagePath)
                    .getBytes(MAX_STOCK_JSON_SIZE_BYTES)
                    .addOnSuccessListener { bytes ->
                        val json = bytes.toString(Charsets.UTF_8)
                        val parsedPayload = parseStockPayload(json)
                        val parsed = parsedPayload.items

                        if (parsed.isEmpty()) {
                            clearStockData()
                            _stockState.value = StockUiState.Error("Stock list JSON is empty or invalid.")
                            return@addOnSuccessListener
                        }

                        _stockLastUpdate.value = parsedPayload.lastUpdate
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
                        Log.e(TAG, "Failed to download stock JSON from Storage path: $storagePath", error)
                        clearStockData()
                        _stockState.value = StockUiState.Error(
                            error.message ?: "Failed to download stock list JSON from Firebase Storage."
                        )
                    }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to load stock list", error)
                clearStockData()
                _stockState.value = StockUiState.Error(
                    error.message ?: "Failed to load stock list from Firestore."
                )
            }
    }

    private fun parseStockPayload(json: String): ParsedStockPayload =
        runCatching {
            val payloadType = object : TypeToken<StockJsonPayload>() {}.type
            val payload = gson.fromJson<StockJsonPayload>(json, payloadType)

            if (!payload?.data.isNullOrEmpty()) {
                ParsedStockPayload(
                    lastUpdate = payload?.lastUpdate.orEmpty(),
                    items = payload?.data.orEmpty()
                        .map { map -> map.toStockItem() }
                        .filter { it.itemCode.isNotBlank() }
                )
            } else {
                val listType = object : TypeToken<List<Map<String, Any?>>>() {}.type
                val legacyList = gson.fromJson<List<Map<String, Any?>>>(json, listType).orEmpty()
                ParsedStockPayload(
                    lastUpdate = "",
                    items = legacyList
                        .map { map -> map.toStockItem() }
                        .filter { it.itemCode.isNotBlank() }
                )
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to parse stock JSON", error)
        }.getOrDefault(ParsedStockPayload())

    private fun clearStockData() {
        _allItems.value = emptyList()
        _locations.value = emptyList()
        _selectedLocation.value = ""
        _stockLastUpdate.value = ""
    }

    private fun Map<*, *>.toStockItem(fallbackId: String = ""): StockItem {
        val payload = this

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

        val uomLocationList = uomMaps.orEmpty().mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val uom = map.string("uom", "UNIT")
            val location = map.string("location")
            if (location.isBlank()) return@mapNotNull null
            UomLocationInfo(
                uom = uom,
                location = location,
                qty = map.int("balQty"),
                rate = map.double("rate", 1.0)
            )
        }.distinctBy { "${it.uom.uppercase()}|${it.location.uppercase()}" }

        return StockItem(
            itemCode = payload.string("itemCode").ifBlank { fallbackId },
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
            uomLocationList = uomLocationList,
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
        const val METADATA_DOCUMENT_ID = "Metadata"
        const val MAX_STOCK_JSON_SIZE_BYTES = 25L * 1024L * 1024L
    }
}

private data class StockJsonPayload(
    val totalItems: Int? = null,
    val lastUpdate: String? = null,
    val data: List<Map<String, Any?>>? = null
)

private data class ParsedStockPayload(
    val lastUpdate: String = "",
    val items: List<StockItem> = emptyList()
)

sealed interface StockUiState {
    data object Loading : StockUiState
    data class Success(val data: List<StockItem>) : StockUiState
    data class Error(val message: String) : StockUiState
}