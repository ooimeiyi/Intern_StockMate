package com.example.intern_stockmate.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.intern_stockmate.data.CompanyContext
import com.example.intern_stockmate.data.local.ApiConfigDatabase
import com.example.intern_stockmate.data.local.ApiConfigEntity
import com.example.intern_stockmate.model.LocationInfo
import com.example.intern_stockmate.model.StockItem
import com.example.intern_stockmate.model.UomInfo
import com.example.intern_stockmate.model.UomLocationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.gson.JsonParser
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.net.HttpURLConnection
import java.net.URL

class StockViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val gson = Gson()
    private val apiConfigDao = ApiConfigDatabase.getInstance(application).apiConfigDao()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var remoteControlListener: ListenerRegistration? = null


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
    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    private val _isRemoteSyncing = MutableStateFlow(false)
    val isRemoteSyncing: StateFlow<Boolean> = _isRemoteSyncing.asStateFlow()

    private val _syncCooldownRemainingMs = MutableStateFlow(0L)
    val syncCooldownRemainingMs: StateFlow<Long> = _syncCooldownRemainingMs.asStateFlow()
    private var lastSyncRequestTimeMs: Long = 0L
    private var cooldownJob: Job? = null

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
                    remoteControlListener?.remove()
                    remoteControlListener = null
                    _allItems.value = emptyList()
                    _locations.value = emptyList()
                    _selectedLocation.value = ""
                    _stockState.value = StockUiState.Error("No company selected.")
                } else {
                    listenRemoteSyncCommand(companyId)
                    getStockList()
                }
            }
        }
    }
    override fun onCleared() {
        remoteControlListener?.remove()
        remoteControlListener = null
        super.onCleared()
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
        viewModelScope.launch(Dispatchers.IO) {
            refreshApiUrlFromFirebase()
            val config = apiConfigDao.getConfig()
            val cachedJson = config?.stockJson.orEmpty()
            if (cachedJson.isBlank()) {
                clearStockData()
                _stockState.value = StockUiState.Error("No local stock data. Please sync from API.")
                return@launch
            }

            val parsedPayload = parseStockPayload(cachedJson)
            val effectiveLastUpdate = parsedPayload.lastUpdate.ifBlank { config?.stockLastUpdate.orEmpty() }
            applyParsedStock(parsedPayload, effectiveLastUpdate)
        }
    }

    fun syncStockListFromApi() {
        val currentItems = _allItems.value
        _stockState.value = StockUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            refreshApiUrlFromFirebase()
            val config = apiConfigDao.getConfig() ?: ApiConfigEntity()
            var apiUrl = config.apiUrl.trim()
            if (apiUrl.isBlank()) {
                if (currentItems.isNotEmpty()) {
                    _stockState.value = StockUiState.Success(currentItems)
                    _syncStatusMessage.value = "API URL not set. Using previous data."
                } else {
                    _stockState.value = StockUiState.Error("API URL is empty. Set it in Config screen first.")
                    _syncStatusMessage.value = "Sync failed: API URL is empty."
                }
                return@launch
            }
            apiUrl = apiUrl.ensureHttpScheme()

            runCatching {
                forceSyncAndFetchStockJson(apiUrl)
            }.onSuccess { json ->
                val parsedPayload = parseStockPayload(json)
                val effectiveLastUpdate = parsedPayload.lastUpdate
                if (parsedPayload.items.isEmpty()) {
                    _stockState.value = StockUiState.Error("Stock list JSON is empty or invalid.")
                    _syncStatusMessage.value = "Sync failed: stock data is empty or invalid."
                    if (currentItems.isNotEmpty()) {
                        _stockState.value = StockUiState.Success(currentItems)
                    }
                    return@onSuccess
                }

                apiConfigDao.upsertConfig(
                    config.copy(
                        apiUrl = apiUrl,
                        stockJson = json,
                        stockLastUpdate = effectiveLastUpdate
                    )
                )
                applyParsedStock(parsedPayload, effectiveLastUpdate)
                _syncStatusMessage.value = "Sync success. Total items: ${parsedPayload.items.size}"
            }.onFailure { error ->
                Log.e(TAG, "Failed to sync stock list from API", error)
                val userFriendlyMessage = if (
                    error.message?.contains("HTTP 503", ignoreCase = true) == true
                ) {
                    "Stock data is still being prepared on the server. Please try again."
                } else {
                    "Fail to connect API."
                }
                if (currentItems.isNotEmpty()) {
                    _stockState.value = StockUiState.Success(currentItems)
                    _syncStatusMessage.value = if (userFriendlyMessage.startsWith("Stock data")) {
                        "$userFriendlyMessage Using previous local data."
                    } else {
                        "Fail to connect API. Using previous local data."
                    }
                } else {
                    _stockState.value = StockUiState.Error(userFriendlyMessage)
                    _syncStatusMessage.value = userFriendlyMessage
                }
            }
        }
    }

    private suspend fun refreshApiUrlFromFirebase() {
        val companyId = CompanyContext.selectedCompanyId.value.trim()
        if (companyId.isBlank()) return

        runCatching {
            val snapshot = firestore.collection("Companies")
                .document(companyId)
                .collection("System")
                .document("ApiConfig")
                .get()
                .await()
            snapshot.getString("current_api_url").orEmpty().trim()
        }.onSuccess { remoteUrl ->
            if (remoteUrl.isNotBlank()) {
                val normalizedUrl = remoteUrl.ensureHttpScheme()
                val currentConfig = apiConfigDao.getConfig() ?: ApiConfigEntity()
                if (currentConfig.apiUrl != normalizedUrl) {
                    apiConfigDao.upsertConfig(currentConfig.copy(apiUrl = normalizedUrl))
                }
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to refresh API URL from Firebase", error)
        }
    }


    fun requestRemoteSync() {
        val companyId = CompanyContext.selectedCompanyId.value.trim()
        if (companyId.isBlank()) {
            _syncStatusMessage.value = "Please select a company first."
            return
        }

        val now = System.currentTimeMillis()
        val remaining = (SYNC_COOLDOWN_MS - (now - lastSyncRequestTimeMs)).coerceAtLeast(0L)
        if (remaining > 0L) {
            _syncCooldownRemainingMs.value = remaining
            _syncStatusMessage.value = "Please wait ${remaining / 1000}s before next sync request."
            return
        }

        lastSyncRequestTimeMs = now
        _syncCooldownRemainingMs.value = 0L
        startCooldownTimer()
        viewModelScope.launch {
            runCatching {
                firestore.collection("Companies")
                    .document(companyId)
                    .collection("System")
                    .document("RemoteControl")
                    .set(mapOf("command" to REMOTE_COMMAND_REQUEST_SYNC), SetOptions.merge())
                    .await()
            }.onSuccess {
                _syncStatusMessage.value = "Sync requested. Waiting for PC update"
            }.onFailure { error ->
                _syncStatusMessage.value = error.message ?: "Failed to request remote sync."
            }
        }
    }

    private fun startCooldownTimer() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            while (true) {
                val remaining = (SYNC_COOLDOWN_MS - (System.currentTimeMillis() - lastSyncRequestTimeMs))
                    .coerceAtLeast(0L)
                _syncCooldownRemainingMs.value = remaining
                if (remaining <= 0L) break
                delay(1000)
            }
        }
    }

    private fun listenRemoteSyncCommand(companyId: String) {
        remoteControlListener?.remove()
        remoteControlListener = firestore.collection("Companies")
            .document(companyId)
            .collection("System")
            .document("RemoteControl")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to listen remote sync command", error)
                    return@addSnapshotListener
                }

                val command = snapshot?.getString("command")?.trim().orEmpty()
                _isRemoteSyncing.value = command.equals(REMOTE_COMMAND_SYNCING, ignoreCase = true)

                if (command.equals(REMOTE_COMMAND_COMPLETED, ignoreCase = true)) {
                    _syncStatusMessage.value = "Remote sync completed. Refreshing stock list..."
                    syncStockListFromApi()
                }
            }
    }

    fun clearSyncStatusMessage() {
        _syncStatusMessage.value = null
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

    private fun applyParsedStock(parsedPayload: ParsedStockPayload, lastUpdate: String) {
        val parsed = parsedPayload.items
        if (parsed.isEmpty()) {
            clearStockData()
            _stockState.value = StockUiState.Error("Stock list JSON is empty or invalid.")
            return
        }

        _stockLastUpdate.value = formatLastUpdate(lastUpdate)
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

    private fun formatLastUpdate(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""

        val inputFormats = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        )
        val outputFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")

        for (input in inputFormats) {
            try {
                val dateTime = LocalDateTime.parse(trimmed, input)
                return dateTime.format(outputFormat)
            } catch (_: DateTimeParseException) {
                // try next
            }
        }
        return trimmed
    }

    private suspend fun forceSyncAndFetchStockJson(baseUrl: String): String {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val forceSyncResponse = httpGet("$normalizedBaseUrl/api/forcesync/")
        if (!isForceSyncSuccess(forceSyncResponse)) {
            throw IllegalStateException("Force sync failed: $forceSyncResponse")
        }
        return httpGet("$normalizedBaseUrl/api/stocklist/")
    }

    private fun isForceSyncSuccess(response: String): Boolean {
        return runCatching {
            val json = JsonParser.parseString(response).asJsonObject
            json.get("status")?.asString?.equals("success", ignoreCase = true) == true
        }.getOrDefault(false)
    }

    private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "InternStockMate/1.0 (Android)")
        }
        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("HTTP $responseCode ${errorBody.take(120)}".trim())
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun String.ensureHttpScheme(): String =
        if (startsWith("http://") || startsWith("https://")) this else "http://$this"

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
        const val REMOTE_COMMAND_REQUEST_SYNC = "REQUEST_SYNC"
        const val REMOTE_COMMAND_SYNCING = "SYNCING"
        const val REMOTE_COMMAND_COMPLETED = "COMPLETED"
        const val SYNC_COOLDOWN_MS = 60_000L
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