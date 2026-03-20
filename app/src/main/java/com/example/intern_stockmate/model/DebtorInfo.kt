package com.example.intern_stockmate.model

data class DebtorSummary(
    val activeDebtor: Int = 0,
    val nonActiveDebtor: Int = 0,
    val totalCountOutstanding: Int = 0,
    val totalSumOutstanding: Double = 0.0
)

data class OutstandingDebtorItem(
    val billCount: Int = 0,
    val companyName: String = "",
    val debtorCode: String = "",
    val outstandingAmount: Double = 0.0
)

data class DebtorResponse(
    val summary: DebtorSummary = DebtorSummary(),
    val outstandingList: List<OutstandingDebtorItem> = emptyList()
)