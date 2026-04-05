package com.example.intern_stockmate.data

import android.content.Context
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CompanyContext {
    private const val PREFS_NAME = "stockmate_company"
    private const val KEY_COMPANY_ID = "selected_company_id"
    private const val UNSELECTED_COMPANY_ID = ""
    private const val FIRESTORE_FALLBACK_ID = "__UNSELECTED__"

    private val _selectedCompanyId = MutableStateFlow(UNSELECTED_COMPANY_ID)
    val selectedCompanyId: StateFlow<String> = _selectedCompanyId.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCompany = prefs.getString(KEY_COMPANY_ID, null)
        _selectedCompanyId.value = savedCompany?.trim().orEmpty()
    }

    fun updateSelectedCompany(context: Context, companyId: String) {
        val normalized = companyId.trim()
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COMPANY_ID, normalized)
            .apply()
        _selectedCompanyId.value = normalized
    }

    fun collection(firestore: FirebaseFirestore, subCollection: String): CollectionReference {
        val companyId = selectedCompanyId.value.ifBlank { FIRESTORE_FALLBACK_ID }
        return firestore.collection(COMPANIES_COLLECTION)
            .document(companyId)
            .collection(subCollection)
    }

    private const val COMPANIES_COLLECTION = "Companies"
}