package com.example.intern_stockmate.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AccountBookContext {
    private const val PREFS_NAME = "stockmate_account_book"
    private const val KEY_ACCOUNT_BOOK_ID = "selected_account_book_id"

    private val _selectedAccountBookId = MutableStateFlow("")
    val selectedAccountBookId: StateFlow<String> = _selectedAccountBookId.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _selectedAccountBookId.value = prefs.getString(KEY_ACCOUNT_BOOK_ID, null)?.trim().orEmpty()
    }

    fun updateSelectedAccountBook(context: Context, accountBookId: String) {
        val normalized = accountBookId.trim()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACCOUNT_BOOK_ID, normalized).apply()
        _selectedAccountBookId.value = normalized
    }
}