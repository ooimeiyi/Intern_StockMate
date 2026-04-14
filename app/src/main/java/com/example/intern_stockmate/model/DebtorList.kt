package com.example.intern_stockmate.model

data class DebtorListPayload(
    val totalItems: Int? = null,
    val lastUpdate: String? = null,
    val data: List<Map<String, Any?>>? = null
)

data class DebtorListItem(
    val debtorCode: String = "",
    val companyName: String = "",
    val multiPrice: String = ""
) {
    fun toLabel(): String = companyName
}