package com.example.intern_stockmate.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.intern_stockmate.data.AccountBookContext
import com.example.intern_stockmate.model.AccountBook
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AccountBookUiState {
    data object Loading : AccountBookUiState
    data class Success(val books: List<AccountBook>) : AccountBookUiState
    data class Error(val message: String) : AccountBookUiState
}

class AccountBookViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountBookUiState>(AccountBookUiState.Loading)
    val uiState: StateFlow<AccountBookUiState> = _uiState.asStateFlow()

    init {
        loadAccountBooks()
    }

    fun loadAccountBooks() {
        _uiState.value = AccountBookUiState.Loading
        val email = auth.currentUser?.email?.trim().orEmpty()
        if (email.isBlank()) {
            _uiState.value = AccountBookUiState.Error("Please log in to load allowed companies.")
            return
        }

        firestore.collection(USERS_COLLECTION)
            .document(email)
            .get()
            .addOnSuccessListener { snapshot ->
                val books = snapshot.get("allowedCompanies")
                    .let { raw ->
                        (raw as? List<*>)?.mapNotNull { value ->
                            (value as? String)?.trim()?.takeIf { it.isNotBlank() }
                        }
                    }
                    .orEmpty()
                    .distinct()
                    .sorted()
                    .map { companyId -> AccountBook(id = companyId) }

                if (books.isEmpty()) {
                    _uiState.value = AccountBookUiState.Error("No allowed companies are assigned to this account.")
                } else {
                    _uiState.value = AccountBookUiState.Success(books)
                }
            }
            .addOnFailureListener { error ->
                _uiState.value = AccountBookUiState.Error(
                    error.message ?: "Unable to load account books"
                )
            }
    }

    fun saveSelectedAccountBook(
        context: Context,
        accountBookId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val normalizedId = accountBookId.trim()
        if (normalizedId.isBlank()) {
            onError("Please select an account book")
            return
        }

        val email = auth.currentUser?.email?.trim().orEmpty()
        if (email.isBlank()) {
            onError("User email not found. Please login again.")
            return
        }

        val payload = mapOf("AccountBook" to normalizedId)

        firestore.collection(USERS_COLLECTION)
            .document(email)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                AccountBookContext.updateSelectedAccountBook(context, normalizedId)
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Unable to save account book")
            }
    }

    private companion object {
        const val USERS_COLLECTION = "Users"
    }
}
