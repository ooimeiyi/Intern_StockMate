package com.example.intern_stockmate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_config")
data class ApiConfigEntity(
    @PrimaryKey val id: Int = 1,
    val apiUrl: String = "",
    val stockJson: String = "",
    val stockLastUpdate: String = ""
)
