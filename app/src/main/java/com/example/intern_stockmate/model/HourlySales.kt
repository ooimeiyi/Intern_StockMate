package com.example.intern_stockmate.model

data class HourlySales(
    val hour: Int = 0,
    val posSales: Double = 0.0,
    val invoiceSales: Double = 0.0,
    val cashSales: Double = 0.0,
    val amount: Double = 0.0
) {
    companion object {
        fun mergeWithFullDay(items: List<HourlySales>): List<HourlySales> {
            val indexed = items.associateBy { it.hour }
            return (0..23).map { hour ->
                val existing = indexed[hour]
                if (existing != null) {
                    existing.copy(
                        amount = existing.amount.takeIf { it != 0.0 }
                            ?: (existing.posSales + existing.invoiceSales + existing.cashSales)
                    )
                } else {
                    HourlySales(hour = hour)
                }
            }
        }
    }
}