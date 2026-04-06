package com.example.intern_stockmate.viewModel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.intern_stockmate.data.CompanyContext
import com.example.intern_stockmate.data.local.SalesOrderDatabase
import com.example.intern_stockmate.data.local.toEntity
import com.example.intern_stockmate.data.local.toModel
import com.example.intern_stockmate.model.SalesOrderDetail
import com.example.intern_stockmate.model.SalesOrderHeader
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

class SalesOrderViewModel(
    private val application: Application,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val stockViewModel: StockViewModel
) : ViewModel() {
    data class DebtorOption(
        val code: String,
        val companyName: String
    )

    data class SalesOrderItemInput(
        val itemCode: String,
        val qty: String,
        val uom: String,
        val unitPrice: Double
    )

    private val soPrefix = "SM-SO"
    private var salesOrderDao = daoForCurrentCompany()
    private val soRegex = Regex("^SM-SO(\\d+)$")
    private val soDigits = 5

    private val _savedHeaders = MutableStateFlow<List<SalesOrderHeader>>(emptyList())
    val savedHeaders: StateFlow<List<SalesOrderHeader>> = _savedHeaders.asStateFlow()

    private val _selectedHeader = MutableStateFlow<SalesOrderHeader?>(null)
    val selectedHeader: StateFlow<SalesOrderHeader?> = _selectedHeader.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _debtors = MutableStateFlow<List<String>>(emptyList())
    val debtors: StateFlow<List<String>> = _debtors.asStateFlow()
    private var debtorCodeByCompanyName: Map<String, String> = emptyMap()

    val selectedLocation: StateFlow<String> = stockViewModel.selectedLocation
    val locations: StateFlow<List<String>> = stockViewModel.locations

    val selectedItems = mutableStateMapOf<String, SalesOrderItemInput>()

    init {
        viewModelScope.launch {
            CompanyContext.selectedCompanyId
                .collect { companyId ->
                    salesOrderDao = daoForCompany(companyId)
                    if (companyId.isBlank()) {
                        _savedHeaders.value = emptyList()
                        _selectedHeader.value = null
                        _debtors.value = emptyList()
                        debtorCodeByCompanyName = emptyMap()
                    } else {
                        loadSavedSalesOrdersFromLocal()
                        loadDebtors()
                    }
                }
        }
    }

    fun onLocationSelected(location: String) {
        if (stockViewModel.selectedLocation.value == location) return
        stockViewModel.onLocationSelected(location)
        stockViewModel.onSearchQueryChange("")
        selectedItems.clear()

        val current = _selectedHeader.value
        if (current != null) {
            _selectedHeader.value = current.copy(location = location)
        }
    }

    fun prepareSalesOrderHeader(
        debtor: String,
        date: String,
        soNo: String,
        location: String
    ) {
        selectedItems.clear()
        _selectedHeader.value = SalesOrderHeader(
            debtor = debtor,
            date = date,
            soNo = soNo,
            location = location,
            items = emptyList(),
            status = "KIV"
        )
        _isEditMode.value = false
        stockViewModel.onLocationSelected(location)
        stockViewModel.onSearchQueryChange("")
    }

    fun prepareNewSalesOrderHeader(location: String, onReady: () -> Unit) {
        generateNextSoNo { nextSoNo ->
            prepareSalesOrderHeader(
                debtor = "",
                date = "",
                soNo = nextSoNo,
                location
            )
            onReady()
        }
    }

    fun onHeaderSelected(header: SalesOrderHeader) {
        _selectedHeader.value = header
        _isEditMode.value = true
        stockViewModel.onLocationSelected(header.location)
        stockViewModel.onSearchQueryChange("")

        selectedItems.clear()
        header.items.forEach { detail ->
            selectedItems[detail.itemCode] = SalesOrderItemInput(
                itemCode = detail.itemCode,
                qty = detail.qty,
                uom = detail.uom,
                unitPrice = detail.unitPrice
            )
        }
    }

    fun updateHeaderFields(
        debtor: String,
        date: String,
        soNo: String,
        location: String
    ) {
        val current = _selectedHeader.value ?: return
        _selectedHeader.value = current.copy(
            debtor = debtor,
            date = date,
            soNo = soNo,
            location = location
        )
        stockViewModel.onLocationSelected(location)
    }

    fun updateSelectedItem(
        itemCode: String,
        qty: String,
        uom: String,
        unitPrice: Double
    ) {
        val normalizedQty = qty.trim()

        selectedItems[itemCode] = SalesOrderItemInput(
            itemCode = itemCode,
            qty = normalizedQty,
            uom = uom,
            unitPrice = unitPrice
        )
    }

    fun loadSavedSalesOrders() {
        loadSavedSalesOrdersFromLocal()
        loadSavedSalesOrdersFromFirebase()
    }

    private fun loadSavedSalesOrdersFromFirebase() {
        CompanyContext.collection(firestore, COLLECTION_NAME)
            .get()
            .addOnSuccessListener { snapshot ->
                val headers = snapshot.documents.map { doc ->
                    val items = (doc.get("items") as? List<*>)
                        ?.mapNotNull { raw ->
                            val map = raw as? Map<*, *> ?: return@mapNotNull null
                            SalesOrderDetail(
                                itemCode = (map["ItemCode"] ?: map["itemCode"])?.toString().orEmpty(),
                                qty = (map["Qty"] ?: map["qty"])?.toString().orEmpty(),
                                uom = (map["UOM"] ?: map["uom"])?.toString().orEmpty(),
                                unitPrice = ((map["UnitPrice"] ?: map["unitPrice"]) as? Number)?.toDouble()
                                    ?: (map["UnitPrice"] ?: map["unitPrice"])?.toString()?.toDoubleOrNull()
                                    ?: 0.0
                            )
                        }
                        ?: emptyList()

                    SalesOrderHeader(
                        debtor = doc.getString("debtor").orEmpty(),
                        date = doc.getString("date").orEmpty(),
                        soNo = doc.getString("soNo")?.ifBlank { doc.id } ?: doc.id,
                        location = doc.getString("location").orEmpty(),
                        status = doc.getString("status") ?: "Submitted",
                        items = items
                    )
                }.sortedByDescending { it.date }

                val localMap = _savedHeaders.value.associateBy { it.soNo }.toMutableMap()
                headers.forEach { header ->
                    val localHeader = localMap[header.soNo]
                    localMap[header.soNo] = if (localHeader != null && localHeader.status == "KIV") {
                        localHeader
                    } else {
                        header
                    }
                }
                _savedHeaders.value = localMap.values.sortedByDescending { it.date }
                persistHeadersToLocal()
            }
            .addOnFailureListener { error ->
                Log.e("SalesOrderViewModel", "Failed to load sales orders", error)
            }
    }

    fun saveCurrentAsKiv(onResult: (Boolean, String) -> Unit) {
        val current = _selectedHeader.value ?: run {
            onResult(false, "No active sales order")
            return
        }

        val wasEditMode = _isEditMode.value

        if (current.debtor.isBlank() || current.date.isBlank() || current.soNo.isBlank() || current.location.isBlank()) {
            onResult(false, "Please fill debtor, date, SO and location")
            return
        }

        val kivHeader = current.copy(
            status = "KIV",
            items = buildDetailsFromSelectedItems()
        )

        _selectedHeader.value = kivHeader
        _savedHeaders.value = (_savedHeaders.value.filterNot { it.soNo == kivHeader.soNo } + kivHeader)
            .sortedByDescending { it.date }
        persistHeadersToLocal()
        if (wasEditMode) {
            updateSalesOrderStatusInFirebase(soNo = kivHeader.soNo, status = "KIV")
        }
        onResult(true, "Saved")
    }

    fun submitCurrentSalesOrder(onResult: (Boolean, String) -> Unit) {
        saveCurrentSalesOrder(status = "Submitted", onResult = onResult)
    }

    private fun saveCurrentSalesOrder(
        status: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val current = _selectedHeader.value ?: run {
            onResult(false, "No active sales order")
            return
        }

        if (current.debtor.isBlank() || current.date.isBlank() || current.soNo.isBlank() || current.location.isBlank()) {
            onResult(false, "Please fill debtor, date, SO and location")
            return
        }

        val finalized = current.copy(
            status = status,
            items = buildDetailsFromSelectedItems()
        )

        val payload = mapOf(
            "debtor" to finalized.debtor,
            "DebtorCode" to resolveDebtorCode(finalized.debtor),
            "date" to finalized.date,
            "soNo" to finalized.soNo,
            "location" to finalized.location,
            "status" to finalized.status,
            "syncStatus" to "Pending",
            "items" to finalized.items.map {
                mapOf(
                    "ItemCode" to it.itemCode,
                    "Qty" to it.qty,
                    "UOM" to it.uom,
                    "UnitPrice" to it.unitPrice
                )
            }
        )

        CompanyContext.collection(firestore, COLLECTION_NAME)
            .document(finalized.soNo)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                _selectedHeader.value = finalized
                val updated = _savedHeaders.value
                    .filterNot { it.soNo == finalized.soNo } + finalized
                _savedHeaders.value = updated.sortedByDescending { it.date }
                persistHeadersToLocal()
                onResult(true, "Saved")
            }
            .addOnFailureListener { exception ->
                onResult(false, exception.message ?: "Unknown error")
            }
    }

    private fun loadSavedSalesOrdersFromLocal() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = salesOrderDao.getAllHeaders()
                .map { it.toModel() }
                .sortedByDescending { it.date }

            withContext(Dispatchers.Main) {
                _savedHeaders.value = loaded
            }
        }
    }

    private fun persistHeadersToLocal() {
        val headersSnapshot = _savedHeaders.value
        viewModelScope.launch(Dispatchers.IO) {
            salesOrderDao.clearAll()
            if (headersSnapshot.isNotEmpty()) {
                salesOrderDao.upsertAll(headersSnapshot.map { it.toEntity() })
            }
        }
    }

    private fun updateSalesOrderStatusInFirebase(soNo: String, status: String) {
        if (soNo.isBlank()) return
        CompanyContext.collection(firestore, COLLECTION_NAME)
            .document(soNo)
            .set(mapOf("status" to status), SetOptions.merge())
            .addOnFailureListener { error ->
                Log.e("SalesOrderViewModel", "Failed to update sales order status to $status", error)
            }
    }

    private fun generateNextSoNo(onResult: (String) -> Unit) {
        val localMax = _savedHeaders.value
            .mapNotNull { extractSequence(it.soNo) }
            .maxOrNull() ?: 0

        CompanyContext.collection(firestore, COLLECTION_NAME)
            .get()
            .addOnSuccessListener { snapshot ->
                val firebaseMax = snapshot.documents
                    .mapNotNull { doc ->
                        extractSequence(doc.getString("soNo") ?: doc.id)
                    }
                    .maxOrNull() ?: 0

                onResult(formatSoNo(max(localMax, firebaseMax) + 1))
            }
            .addOnFailureListener {
                onResult(formatSoNo(localMax + 1))
            }
    }

    private fun loadDebtors() {
        CompanyContext.collection(firestore, DEBTOR_COLLECTION)
            .document(DEBTOR_DOCUMENT)
            .get()
            .addOnSuccessListener { snapshot ->
                val options = (snapshot.get("outstandingList") as? List<*>)
                    .orEmpty()
                    .mapNotNull { raw ->
                        val item = raw as? Map<*, *> ?: return@mapNotNull null
                        val companyName = item["CompanyName"]?.toString()?.trim().orEmpty()
                        if (companyName.isBlank()) return@mapNotNull null
                        DebtorOption(
                            code = item["DebtorCode"]?.toString()?.trim().orEmpty(),
                            companyName = companyName
                        )
                    }
                    .distinctBy { it.companyName }
                    .sortedBy { it.companyName }

                debtorCodeByCompanyName = options.associate { it.companyName to it.code }
                _debtors.value = options.map { it.companyName }
            }
            .addOnFailureListener { error ->
                Log.e("SalesOrderViewModel", "Failed to load debtors", error)
            }
    }

    private fun resolveDebtorCode(debtorLabel: String): String {
        val normalizedLabel = debtorLabel.trim()
        if (normalizedLabel.isBlank()) return ""

        debtorCodeByCompanyName[normalizedLabel]?.takeIf { it.isNotBlank() }?.let { return it }

        return normalizedLabel.substringBefore(" - ", "").trim()
            .takeIf { it.isNotBlank() && it != normalizedLabel }
            .orEmpty()
    }

    private fun extractSequence(soNo: String): Int? {
        val match = soRegex.matchEntire(soNo) ?: return null
        return match.groupValues.getOrNull(1)?.toIntOrNull()
    }

    private fun formatSoNo(sequence: Int): String {
        return "$soPrefix${sequence.toString().padStart(soDigits, '0')}"
    }

    private fun buildDetailsFromSelectedItems(): List<SalesOrderDetail> {
        return selectedItems.values
            .filter { it.qty.isNotBlank() }
            .map {
                SalesOrderDetail(
                    itemCode = it.itemCode,
                    qty = it.qty,
                    uom = it.uom,
                    unitPrice = it.unitPrice
                )
            }
            .sortedBy { it.itemCode }
    }

    private fun daoForCurrentCompany() = daoForCompany(CompanyContext.selectedCompanyId.value)

    private fun daoForCompany(companyId: String) =
        SalesOrderDatabase.getInstance(application, companyId).salesOrderDao()

    private companion object {
        const val COLLECTION_NAME = "SalesOrders"
        const val DEBTOR_COLLECTION = "DebtorSummary"
        const val DEBTOR_DOCUMENT = "Current"
    }
}

class SalesOrderViewModelFactory(
    private val application: Application,
    private val stockViewModel: StockViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesOrderViewModel::class.java)) {
            return SalesOrderViewModel(application = application, stockViewModel = stockViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}