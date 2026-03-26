package com.example.intern_stockmate.model

data class MonthlySales(
    val month: Int = 1,
    val posSales: Double = 0.0,
    val invoiceSales: Double = 0.0,
    val cashSales: Double = 0.0,
    val amount: Double = 0.0
) {
    companion object {
        fun mergeWithFullYear(items: List<MonthlySales>): List<MonthlySales> {
            val indexed = items.associateBy { it.month }
            return (1..12).map { month ->
                val existing = indexed[month]
                if (existing != null) {
                    existing.copy(
                        amount = existing.amount.takeIf { it != 0.0 }
                            ?: (existing.posSales + existing.invoiceSales + existing.cashSales)
                    )
                } else {
                    MonthlySales(month = month)
                }
            }
        }
    }
}