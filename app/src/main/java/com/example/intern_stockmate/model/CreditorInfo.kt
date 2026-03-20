package com.example.intern_stockmate.model

data class CreditorSummary(
    val activeCreditor: Int = 0,
    val nonActiveCreditor: Int = 0,
    val totalCountOutstanding: Int = 0,
    val totalSumOutstanding: Double = 0.0
)

data class OutstandingCreditorItem(
    val billCount: Int = 0,
    val companyName: String = "",
    val creditorCode: String = "",
    val outstandingAmount: Double = 0.0
)

data class CreditorResponse(
    val summary: CreditorSummary = CreditorSummary(),
    val outstandingList: List<OutstandingCreditorItem> = emptyList()
)