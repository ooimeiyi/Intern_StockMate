package com.example.intern_stockmate.viewModel

import androidx.lifecycle.ViewModel
import com.example.intern_stockmate.model.AccountBook
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date

sealed interface AccountBookUiState {
    data object Loading : AccountBookUiState
    data class Success(val books: List<AccountBook>) : AccountBookUiState
    data class Error(val message: String) : AccountBookUiState
}

class AccountBookViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountBookUiState>(AccountBookUiState.Loading)
    val uiState: StateFlow<AccountBookUiState> = _uiState.asStateFlow()

    init {
        loadAccountBooks()
    }

    fun loadAccountBooks() {
        _uiState.value = AccountBookUiState.Loading
        firestore.collection("Licenses")
            .get()
            .addOnSuccessListener { snapshot ->
                val books = snapshot.documents
                    .map { document ->
                        AccountBook(
                            id = document.id,
                            expiryDate = document.getFieldAsDate("expiryDate"),
                            isActive = document.getBoolean("isActive") ?: false
                        )
                    }
                    .sortedBy { it.id }

                _uiState.value = AccountBookUiState.Success(books)
            }
            .addOnFailureListener { error ->
                _uiState.value = AccountBookUiState.Error(
                    error.message ?: "Unable to load account books"
                )
            }
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.getFieldAsDate(fieldName: String): Date? {
    val value = get(fieldName) ?: return null
    return when (value) {
        is Timestamp -> value.toDate()
        is Date -> value
        is Long -> Date(value)
        is String -> parseDateString(value)
        else -> null
    }
}

private fun parseDateString(value: String): Date? {
    val trimmedValue = value.trim()
    if (trimmedValue.isEmpty()) return null

    return parseIsoLocalDate(trimmedValue)
        ?: runCatching { Date(trimmedValue.toLong()) }.getOrNull()
}

private fun parseIsoLocalDate(value: String): Date? {
    return try {
        val localDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
        Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    } catch (_: DateTimeParseException) {
        null
    }
}