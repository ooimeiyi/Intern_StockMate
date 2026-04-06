package com.example.intern_stockmate.viewModel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.intern_stockmate.data.CompanyContext
import com.example.intern_stockmate.data.local.StockAdjustmentDatabase
import com.example.intern_stockmate.data.local.toEntity
import com.example.intern_stockmate.data.local.toModel
import com.example.intern_stockmate.model.StockAdjustmentDetail
import com.example.intern_stockmate.model.StockAdjustmentHeader
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.collections.set
import kotlin.math.max

class StockAdjustmentViewModel(
    application: Application,
    private val stockViewModel: StockViewModel
) : AndroidViewModel(application) {
    private val stockTakePrefix = "SM-ADJ"
    private val stockTakeRegex = Regex("^${stockTakePrefix}(\\d+)$")
    private val stockTakeDigits = 6

    private var stockAdjustmentDao =
        StockAdjustmentDatabase.getInstance(application, CompanyContext.selectedCompanyId.value).stockAdjustmentDao()

    val searchQuery: MutableStateFlow<String> = stockViewModel.searchQuery
    val locations: StateFlow<List<String>> = stockViewModel.locations
    val selectedLocation: StateFlow<String> = stockViewModel.selectedLocation

    fun onSearchQueryChange(query: String) {
        stockViewModel.onSearchQueryChange(query)
    }

    fun onLocationSelected(location: String) {
        stockViewModel.onLocationSelected(location)
    }

    val physicalCounts = mutableStateMapOf<String, String>()
    val diffCounts = mutableStateMapOf<String, Int>()

    private val _savedHeaders = MutableStateFlow<List<StockAdjustmentHeader>>(emptyList())
    val savedHeaders: StateFlow<List<StockAdjustmentHeader>> = _savedHeaders.asStateFlow()

    private val _selectedHeader = MutableStateFlow<StockAdjustmentHeader?>(null)
    val selectedHeader: StateFlow<StockAdjustmentHeader?> = _selectedHeader.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    init {
        viewModelScope.launch {
            CompanyContext.selectedCompanyId
                .collect { companyId ->
                    stockAdjustmentDao = StockAdjustmentDatabase
                        .getInstance(getApplication(), companyId)
                        .stockAdjustmentDao()

                    if (companyId.isBlank()) {
                        _savedHeaders.value = emptyList()
                        _selectedHeader.value = null
                    } else {
                        loadSavedAdjustmentsFromLocal()
                    }
                }
        }
    }

    fun prepareAdjustmentHeader(
        description: String,
        date: String,
        stockTakeNo: String,
        location: String
    ) {
        physicalCounts.clear()
        diffCounts.clear()

        _selectedHeader.value = StockAdjustmentHeader(
            description = description,
            date = date,
            stockTakeNo = stockTakeNo,
            location = location,
            items = emptyList(),
            status = "KIV"
        )
        _isEditMode.value = false
        searchQuery.value = ""

        stockViewModel.onLocationSelected(location)
    }

    fun prepareNewAdjustmentHeader(location: String, onReady: () -> Unit) {
        generateNextStockTakeNo { nextStockTakeNo ->
            prepareAdjustmentHeader("", "", nextStockTakeNo, location)
            onReady()
        }
    }

    fun onHeaderSelected(header: StockAdjustmentHeader) {
        _selectedHeader.value = header
        _isEditMode.value = true
        searchQuery.value = ""
        stockViewModel.onLocationSelected(header.location)

        physicalCounts.clear()
        diffCounts.clear()

        header.items.forEach { detail ->
            physicalCounts[detail.itemCode] = detail.qty

            val item = stockViewModel.allItems.value.find { it.itemCode == detail.itemCode }
            val onHand = item?.locationList?.find { it.location == header.location }?.qty ?: 0
            val physicalInt = detail.qty.toIntOrNull() ?: 0
            diffCounts[detail.itemCode] = physicalInt - onHand
        }
    }

    fun updateSelectedHeaderFields(
        description: String,
        date: String,
        stockTakeNo: String,
        location: String
    ) {
        val current = _selectedHeader.value ?: return
        _selectedHeader.value = current.copy(
            description = description,
            date = date,
            stockTakeNo = stockTakeNo,
            location = location
        )
        stockViewModel.onLocationSelected(location)
    }

    fun saveCurrentAsKiv(): Boolean {
        val currentHeader = _selectedHeader.value ?: return false
        val previousStatus = currentHeader.status
        val headerWithId = ensureStockTakeNo(currentHeader)
        val localHeader = buildHeaderWithCurrentItems(headerWithId, "KIV")
        upsertHeader(localHeader)
        persistHeadersToLocal()
        _selectedHeader.value = localHeader
        if (previousStatus == "Submitted") {
            markSubmittedDocumentAsKiv(localHeader.stockTakeNo)
        }
        return true
    }

    fun submitCurrentAdjustment(onResult: (Boolean, String) -> Unit) {
        val currentHeader = _selectedHeader.value ?: run {
            onResult(false, "No selected header")
            return
        }

        val headerWithId = ensureStockTakeNo(currentHeader)
        val localHeader = buildHeaderWithCurrentItems(headerWithId, "Submitted")
        val firebaseHeader = localHeader.copy(
            items = physicalCounts.keys
                .filter { itemCode -> physicalCounts[itemCode].orEmpty().isNotBlank() }
                .map { itemCode ->
                    StockAdjustmentDetail(
                        itemCode = itemCode,
                        qty = (diffCounts[itemCode] ?: 0).toString(),
                        uom = stockViewModel.allItems.value.find { it.itemCode == itemCode }?.uom.orEmpty()
                    )
                }
        )
        saveStockAdjustmentToFirebase(firebaseHeader) { success, message ->
            if (success) {
                upsertHeader(localHeader)
                persistHeadersToLocal()
                _selectedHeader.value = localHeader
            }
            onResult(success, message)
        }
    }

    private fun ensureStockTakeNo(header: StockAdjustmentHeader): StockAdjustmentHeader {
        if (header.stockTakeNo.isNotBlank()) return header
        return header.copy(stockTakeNo = "KIV-${UUID.randomUUID().toString().take(8)}")
    }

    private fun generateNextStockTakeNo(onResult: (String) -> Unit) {
        val localMax = _savedHeaders.value
            .mapNotNull { extractStockTakeSequence(it.stockTakeNo) }
            .maxOrNull() ?: 0

        val firestore = getFirestoreOrNull()
        if (firestore == null) {
            onResult(formatStockTakeNo(localMax + 1))
            return
        }

        CompanyContext.collection(firestore, "stockAdjustments")
            .get()
            .addOnSuccessListener { snapshot ->
                val firebaseMax = snapshot.documents
                    .mapNotNull { doc ->
                        extractStockTakeSequence(doc.getString("stockTakeNo") ?: doc.id)
                    }
                    .maxOrNull() ?: 0

                onResult(formatStockTakeNo(max(localMax, firebaseMax) + 1))
            }
            .addOnFailureListener {
                onResult(formatStockTakeNo(localMax + 1))
            }
    }

    private fun extractStockTakeSequence(stockTakeNo: String): Int? {
        val match = stockTakeRegex.matchEntire(stockTakeNo) ?: return null
        return match.groupValues.getOrNull(1)?.toIntOrNull()
    }

    private fun formatStockTakeNo(sequence: Int): String {
        return "$stockTakePrefix${sequence.toString().padStart(stockTakeDigits, '0')}"
    }

    private fun buildHeaderWithCurrentItems(
        header: StockAdjustmentHeader,
        status: String
    ): StockAdjustmentHeader {
        return header.copy(
            status = status,
            items = physicalCounts.keys.map { itemCode ->
                StockAdjustmentDetail(
                    itemCode = itemCode,
                    qty = physicalCounts[itemCode] ?: "",
                    uom = stockViewModel.allItems.value.find { it.itemCode == itemCode }?.uom.orEmpty()
                )
            }
        )
    }

    private fun upsertHeader(header: StockAdjustmentHeader) {
        _savedHeaders.value = _savedHeaders.value.let { list ->
            val index = list.indexOfFirst { it.stockTakeNo == header.stockTakeNo }
            if (index != -1) list.toMutableList().apply { set(index, header) }
            else list + header
        }.sortedByDescending { it.date }
    }

    private fun persistHeadersToLocal() {
        val headersSnapshot = _savedHeaders.value
        viewModelScope.launch(Dispatchers.IO) {
            stockAdjustmentDao.clearAll()
            if (headersSnapshot.isNotEmpty()) {
                stockAdjustmentDao.upsertAll(headersSnapshot.map { it.toEntity() })
            }
        }
    }

    fun loadSavedAdjustmentsFromLocal() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = stockAdjustmentDao.getAllHeaders()
                .map { it.toModel() }
                .sortedByDescending { it.date }

            withContext(Dispatchers.Main) {
                _savedHeaders.value = loaded
            }
        }
    }

    private fun getFirestoreOrNull(): FirebaseFirestore? =
        runCatching { FirebaseFirestore.getInstance() }
            .onFailure { Log.e("StockAdjustmentViewModel", "Firebase init failed", it) }
            .getOrNull()

    private fun markSubmittedDocumentAsKiv(stockTakeNo: String) {
        if (stockTakeNo.isBlank()) return
        val firestore = getFirestoreOrNull() ?: return
        CompanyContext.collection(firestore, "stockAdjustments")
            .document(stockTakeNo)
            .set(
                mapOf(
                    "status" to "KIV",
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnFailureListener { exception ->
                Log.e("StockAdjustmentViewModel", "Failed to switch submitted adjustment to KIV", exception)
            }
    }

    fun loadSavedAdjustmentsFromFirebase() {
        val firestore = getFirestoreOrNull() ?: return

        CompanyContext.collection(firestore, "stockAdjustments")
            .get()
            .addOnSuccessListener { snapshot ->
                val firebaseHeaders = snapshot.documents.mapNotNull { doc ->
                    val status = doc.getString("status") ?: "Submitted"
                    if (status != "Submitted") return@mapNotNull null
                    val description = doc.getString("description") ?: ""
                    val date = doc.getString("date") ?: ""
                    val stockTakeNo = doc.getString("stockTakeNo")?.ifBlank { doc.id } ?: doc.id
                    val location = doc.getString("location") ?: ""

                    val items = (doc.get("items") as? List<*>)
                        ?.mapNotNull { rawItem ->
                            val map = rawItem as? Map<*, *> ?: return@mapNotNull null
                            val itemCode = map["itemCode"] as? String ?: return@mapNotNull null
                            val diffQtyString = (map["qty"] as? String) ?: (map["qty"]?.toString() ?: "0")

                            val onHand = stockViewModel.allItems.value
                                .find { it.itemCode == itemCode }
                                ?.locationList
                                ?.find { it.location == location }
                                ?.qty ?: 0

                            val physicalQty = onHand + (diffQtyString.toIntOrNull() ?: 0)
                            val uom = (map["uom"] as? String).orEmpty().ifBlank {
                                stockViewModel.allItems.value.find { it.itemCode == itemCode }?.uom.orEmpty()
                            }
                            StockAdjustmentDetail(itemCode = itemCode, qty = physicalQty.toString(), uom = uom)
                        }
                        ?: emptyList()

                    StockAdjustmentHeader(
                        description = description,
                        date = date,
                        stockTakeNo = stockTakeNo,
                        location = location,
                        items = items,
                        status = status
                    )
                }

                val localMap = _savedHeaders.value.associateBy { it.stockTakeNo }.toMutableMap()
                firebaseHeaders.forEach { header -> localMap[header.stockTakeNo] = header }
                _savedHeaders.value = localMap.values.sortedByDescending { it.date }
                persistHeadersToLocal()
            }
            .addOnFailureListener { exception ->
                Log.e("StockAdjustmentViewModel", "Load from Firebase failed", exception)
            }
    }

    fun saveStockAdjustmentToFirebase(
        header: StockAdjustmentHeader,
        onResult: (Boolean, String) -> Unit
    ) {
        if (header.status != "Submitted") {
            onResult(false, "Only submitted adjustments are allowed to sync to Firebase")
            return
        }
        val firestore = getFirestoreOrNull()
        if (firestore == null) {
            onResult(false, "Firebase is not initialized")
            return
        }

        val stockTakeNo = header.stockTakeNo.ifBlank { "SUB-${UUID.randomUUID().toString().take(8)}" }
        val docRef = CompanyContext.collection(firestore, "stockAdjustments").document(stockTakeNo)

        val payload = mapOf(
            "description" to header.description,
            "date" to header.date,
            "stockTakeNo" to stockTakeNo,
            "location" to header.location,
            "status" to header.status,
            "syncStatus" to "Pending",
            "items" to header.items.map { detail ->
                mapOf(
                    "itemCode" to detail.itemCode,
                    "qty" to detail.qty,
                    "uom" to detail.uom
                )
            },
            "stability" to FieldValue.delete()
        )

        docRef.set(payload, SetOptions.merge())
            .addOnSuccessListener { onResult(true, "Saved successfully") }
            .addOnFailureListener { exception ->
                onResult(false, exception.message ?: "Unknown error")
            }
    }
}

class StockAdjustmentViewModelFactory(
    private val application: Application,
    private val stockViewModel: StockViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockAdjustmentViewModel::class.java)) {
            return StockAdjustmentViewModel(application, stockViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}