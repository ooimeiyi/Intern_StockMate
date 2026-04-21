package com.example.intern_stockmate.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

data class AccountBook(
    val id: String,
    val expiryDate: Date?,
    val isActive: Boolean
) {
    fun expiryDateLabel(): String {
        val date = expiryDate ?: return "No expiry date"
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return formatter.format(
            Instant.ofEpochMilli(date.time)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        )
    }
}