package com.example.intern_stockmate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_credentials")
data class UserCredentialEntity(
    @PrimaryKey val userId: String,
    val password: String
)