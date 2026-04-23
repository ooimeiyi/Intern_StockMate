package com.example.intern_stockmate.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AccessPasswordStore {
    private const val PREFS_NAME = "stockmate_access_passwords"
    private const val KEY_ADMIN_PASSWORD = "admin_password"
    private const val KEY_STOCK_PASSWORD = "stock_password"

    const val DEFAULT_ADMIN_PASSWORD = "admin"
    const val DEFAULT_STOCK_PASSWORD = "stock"

    private val _adminPassword = MutableStateFlow(DEFAULT_ADMIN_PASSWORD)
    val adminPassword: StateFlow<String> = _adminPassword.asStateFlow()

    private val _stockPassword = MutableStateFlow(DEFAULT_STOCK_PASSWORD)
    val stockPassword: StateFlow<String> = _stockPassword.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _adminPassword.value = prefs.getString(KEY_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_ADMIN_PASSWORD
        _stockPassword.value = prefs.getString(KEY_STOCK_PASSWORD, DEFAULT_STOCK_PASSWORD)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_STOCK_PASSWORD
    }

    fun updatePasswords(context: Context, adminPassword: String, stockPassword: String) {
        val normalizedAdmin = adminPassword.trim().ifBlank { DEFAULT_ADMIN_PASSWORD }
        val normalizedStock = stockPassword.trim().ifBlank { DEFAULT_STOCK_PASSWORD }

        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ADMIN_PASSWORD, normalizedAdmin)
            .putString(KEY_STOCK_PASSWORD, normalizedStock)
            .apply()

        _adminPassword.value = normalizedAdmin
        _stockPassword.value = normalizedStock
    }
}