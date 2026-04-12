package com.example.intern_stockmate.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DocumentNumberFormatStore {
    private const val PREFS_NAME = "stockmate_document_format"
    private const val KEY_SALES_ORDER_FORMAT = "sales_order_format"
    private const val KEY_STOCK_ADJUSTMENT_FORMAT = "stock_adjustment_format"

    private const val DEFAULT_PLACEHOLDER = "000000"
    private val PLACEHOLDER_REGEX = Regex("\\{0+\\}|0+")
    private val LEGACY_BRACE_ZERO_REGEX = Regex("\\{(0+)\\}")

    const val DEFAULT_SALES_ORDER_FORMAT = "SM-SO$DEFAULT_PLACEHOLDER"
    const val DEFAULT_STOCK_ADJUSTMENT_FORMAT = "SM-ST$DEFAULT_PLACEHOLDER"

    private val _salesOrderFormat = MutableStateFlow(DEFAULT_SALES_ORDER_FORMAT)
    val salesOrderFormat: StateFlow<String> = _salesOrderFormat.asStateFlow()

    private val _stockAdjustmentFormat = MutableStateFlow(DEFAULT_STOCK_ADJUSTMENT_FORMAT)
    val stockAdjustmentFormat: StateFlow<String> = _stockAdjustmentFormat.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _salesOrderFormat.value = normalizeFormat(
            prefs.getString(KEY_SALES_ORDER_FORMAT, DEFAULT_SALES_ORDER_FORMAT),
            DEFAULT_SALES_ORDER_FORMAT
        )
        _stockAdjustmentFormat.value = normalizeFormat(
            prefs.getString(KEY_STOCK_ADJUSTMENT_FORMAT, DEFAULT_STOCK_ADJUSTMENT_FORMAT),
            DEFAULT_STOCK_ADJUSTMENT_FORMAT
        )
    }

    fun updateFormats(
        context: Context,
        salesOrderFormat: String,
        stockAdjustmentFormat: String
    ) {
        val normalizedSo = normalizeFormat(salesOrderFormat, DEFAULT_SALES_ORDER_FORMAT)
        val normalizedSt = normalizeFormat(stockAdjustmentFormat, DEFAULT_STOCK_ADJUSTMENT_FORMAT)

        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SALES_ORDER_FORMAT, normalizedSo)
            .putString(KEY_STOCK_ADJUSTMENT_FORMAT, normalizedSt)
            .apply()

        _salesOrderFormat.value = normalizedSo
        _stockAdjustmentFormat.value = normalizedSt
    }

    fun isValidFormat(format: String): Boolean {
        val trimmed = normalizeLegacyBraceFormat(format.trim())
        return PLACEHOLDER_REGEX.containsMatchIn(trimmed)
    }

    fun formatPreview(format: String): String {
        val trimmed = format.trim()
        if (!isValidFormat(trimmed)) return trimmed
        return applySequence(trimmed, 1)
    }

    fun enforceFixedPlaceholder(input: String, previous: String): String {
        val normalizedInput = normalizeLegacyBraceFormat(input.trim())
        if (isValidFormat(normalizedInput)) return normalizedInput

        // Keep at least one zero-placeholder group in format.
        return normalizeFormat(previous, DEFAULT_SALES_ORDER_FORMAT)
    }

    fun prefix(format: String): String {
        val trimmed = normalizeLegacyBraceFormat(format.trim())
        val placeholder = PLACEHOLDER_REGEX.find(trimmed) ?: return trimmed
        return trimmed.substring(0, placeholder.range.first)
    }

    fun applySequence(format: String, sequence: Int): String {
        val trimmed = normalizeLegacyBraceFormat(format.trim())
        val placeholder = PLACEHOLDER_REGEX.find(trimmed) ?: return trimmed
        val digitsCount = placeholder.value.count { it == '0' }
        val replacement = sequence.toString().padStart(digitsCount, '0')
        return trimmed.replaceRange(placeholder.range, replacement)
    }

    fun extractSequence(format: String, documentNo: String): Int? {
        val trimmedFormat = normalizeLegacyBraceFormat(format.trim())
        val placeholder = PLACEHOLDER_REGEX.find(trimmedFormat) ?: return null
        val prefix = trimmedFormat.substring(0, placeholder.range.first)
        val suffix = trimmedFormat.substring(placeholder.range.last + 1)

        if (!documentNo.startsWith(prefix) || !documentNo.endsWith(suffix)) return null
        val numberPart = documentNo.removePrefix(prefix).removeSuffix(suffix)
        val digitsCount = placeholder.value.count { it == '0' }
        if (numberPart.length != digitsCount) return null
        return numberPart.toIntOrNull()
    }

    private fun normalizeFormat(raw: String?, fallback: String): String {
        val trimmed = normalizeLegacyBraceFormat(raw?.trim().orEmpty())
        if (!isValidFormat(trimmed)) return fallback
        return trimmed
    }

    private fun normalizeLegacyBraceFormat(value: String): String {
        return value.replace(LEGACY_BRACE_ZERO_REGEX, "$1")
    }
}