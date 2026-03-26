package com.example.intern_stockmate.model

import java.time.YearMonth

data class DailySales(
    val day: Int = 1,
    val posSales: Double = 0.0,
    val invoiceSales: Double = 0.0,
    val cashSales: Double = 0.0,
    val amount: Double = 0.0
) {
    companion object {
        fun mergeWithFullMonth(items: List<DailySales>, yearMonth: YearMonth = YearMonth.now()): List<DailySales> {
            val indexed = items.associateBy { it.day }
            return (1..yearMonth.lengthOfMonth()).map { day ->
                val existing = indexed[day]
                if (existing != null) {
                    existing.copy(
                        amount = existing.amount.takeIf { it != 0.0 }
                            ?: (existing.posSales + existing.invoiceSales + existing.cashSales)
                    )
                } else {
                    DailySales(day = day)
                }
            }
        }
    }
}