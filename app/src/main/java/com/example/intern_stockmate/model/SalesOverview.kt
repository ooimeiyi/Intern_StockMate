package com.example.intern_stockmate.model

data class SalesOverviewTodayData(
    val totalSalesToday: Double = 0.0,
    val cashSales: Double = 0.0,
    val invoiceSales: Double = 0.0,
    val posSales: Double = 0.0,
    val reportDate: String = "",
    val lastUpdate: String = ""
)

data class SalesOverviewSummaryData(
    val rangeLabel: String,
    val invoice: Double,
    val cashSale: Double,
    val pos: Double,
    val total: Double,
    val lastUpdate: String
)