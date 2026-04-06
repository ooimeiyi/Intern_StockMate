package com.example.intern_stockmate.viewModel

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import com.example.intern_stockmate.data.CompanyContext
import com.example.intern_stockmate.model.SalesOverviewSummaryData
import com.example.intern_stockmate.model.SalesOverviewTodayData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class SalesOverviewViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    var selectedTabIndex = mutableIntStateOf(0)
        private set

    private val _uiState = MutableStateFlow<SalesOverviewUiState>(
        SalesOverviewUiState.Success(
            todayData = SalesOverviewTodayData(),
            summaryData = null
        )
    )
    val uiState: StateFlow<SalesOverviewUiState> = _uiState.asStateFlow()

    init {
        fetchSummaryForTab(TABS.first())
    }

    fun selectTab(index: Int) {
        selectedTabIndex.intValue = index
        fetchSummaryForTab(TABS.getOrElse(index) { TABS.first() })
    }

    fun fetchSummaryForTab(tab: String) {
        _uiState.value = SalesOverviewUiState.Loading

        when (tab) {
            "Today" -> fetchTodaySummary()
            "Week" -> fetchRangeSummary("ThisWeek", tab)
            "Month" -> fetchRangeSummary("ThisMonth", tab)
            "Year" -> fetchRangeSummary("ThisYear", tab)
            else -> fetchTodaySummary()
        }
    }

    private fun fetchTodaySummary() {
        val todayDocId = LocalDate.now().format(TODAY_DOC_FORMATTER)

        CompanyContext.collection(firestore, DAILY_COLLECTION)
            .document(DOCUMENT_NAME)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    _uiState.value = SalesOverviewUiState.Success(
                        todayData = SalesOverviewTodayData(
                            reportDate = todayDocId,
                            lastUpdate = ""
                        ),
                        summaryData = null
                    )
                    return@addOnSuccessListener
                }

                val todayData = SalesOverviewTodayData(
                    totalSalesToday = snapshot.doubleValue("grandTotal"),
                    cashSales = snapshot.doubleValue("cashSale"),
                    invoiceSales = snapshot.doubleValue("invoice"),
                    posSales = snapshot.doubleValue("pos"),
                    reportDate = snapshot.stringValue("reportDate").ifBlank { todayDocId },
                    lastUpdate = snapshot.stringValue("lastUpdate")
                )
                _uiState.value = SalesOverviewUiState.Success(todayData = todayData, summaryData = null)
            }
            .addOnFailureListener {
                _uiState.value = SalesOverviewUiState.Success(
                    todayData = SalesOverviewTodayData(reportDate = todayDocId),
                    summaryData = null
                )
            }
    }

    private fun fetchRangeSummary(documentId: String, rangeLabel: String) {
        CompanyContext.collection(firestore, SUMMARY_COLLECTION)
            .document(documentId)
            .get()
            .addOnSuccessListener { snapshot ->
                val summary = if (!snapshot.exists()) {
                    SalesOverviewSummaryData(
                        rangeLabel = rangeLabel,
                        invoice = 0.0,
                        cashSale = 0.0,
                        pos = 0.0,
                        total = 0.0,
                        lastUpdate = ""
                    )
                } else {
                    SalesOverviewSummaryData(
                        rangeLabel = rangeLabel,
                        invoice = snapshot.doubleValue("invoice"),
                        cashSale = snapshot.doubleValue("cashSale"),
                        pos = snapshot.doubleValue("pos"),
                        total = snapshot.doubleValue("total"),
                        lastUpdate = snapshot.stringValue("lastUpdate")
                    )
                }
                _uiState.value = SalesOverviewUiState.Success(todayData = null, summaryData = summary)
            }
            .addOnFailureListener {
                _uiState.value = SalesOverviewUiState.Success(
                    todayData = null,
                    summaryData = SalesOverviewSummaryData(
                        rangeLabel = rangeLabel,
                        invoice = 0.0,
                        cashSale = 0.0,
                        pos = 0.0,
                        total = 0.0,
                        lastUpdate = ""
                    )
                )
            }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.doubleValue(key: String): Double {
        val value = get(key) ?: return 0.0
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.stringValue(key: String): String {
        return get(key)?.toString().orEmpty()
    }

    companion object {
        val TABS = listOf("Today", "Week", "Month", "Year")
        private const val DAILY_COLLECTION = "DailySalesSummary"
        private const val SUMMARY_COLLECTION = "SalesSummary"
        private const val DOCUMENT_NAME = "Current"
        private val TODAY_DOC_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US)
    }
}

sealed interface SalesOverviewUiState {
    data object Loading : SalesOverviewUiState
    data class Success(
        val todayData: SalesOverviewTodayData?,
        val summaryData: SalesOverviewSummaryData?
    ) : SalesOverviewUiState
}