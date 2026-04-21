package com.example.intern_stockmate.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import androidx.lifecycle.viewModelScope
import com.example.intern_stockmate.data.local.UserCredentialDao
import com.example.intern_stockmate.data.local.UserCredentialEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class LoginViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userCredentialDao: UserCredentialDao? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    var inputUserId by mutableStateOf("")
        private set
    var inputPassword by mutableStateOf("")
        private set

    var loginError by mutableStateOf(false)
        private set

    var loginErrorMessage by mutableStateOf("Incorrect Email or Password")
        private set

    var isLoggingIn by mutableStateOf(false)
        private set

    var isPasswordVisible by mutableStateOf(false)
        private set

    init {
        loadSavedCredential()
    }

    private fun loadSavedCredential() {
        viewModelScope.launch(Dispatchers.IO) {
            val savedCredential = userCredentialDao?.getLatestCredential() ?: return@launch
            withContext(Dispatchers.Main) {
                inputUserId = savedCredential.userId
                inputPassword = savedCredential.password
            }
        }
    }
    fun onUserIdChange(newValue: String) {
        inputUserId = newValue
        loginError = false
    }

    fun onPasswordChange(newValue: String) {
        inputPassword = newValue
        loginError = false
    }

    fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
    }

    fun clearFields() {
        inputUserId = ""
        inputPassword = ""
        loginError = false
    }

    val isLoginEnabled: Boolean
        get() = inputUserId.isNotBlank() && inputPassword.isNotBlank()

    fun attemptLogin(onResult: (Boolean) -> Unit) {
        val email = inputUserId.trim()
        val password = inputPassword
        if (email.isBlank() || password.isBlank()) {
            loginError = true
            loginErrorMessage = "Please enter email and password."
            onResult(false)
            return
        }

        isLoggingIn = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                isLoggingIn = false
                if (task.isSuccessful) {
                    loginError = false
                    loginErrorMessage = ""
                    viewModelScope.launch(Dispatchers.IO) {
                        userCredentialDao?.upsertCredential(
                            UserCredentialEntity(
                                userId = email,
                                password = password
                            )
                        )
                    }
                    onResult(true)
                } else {
                    loginError = true
                    loginErrorMessage = when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password"
                        is FirebaseAuthInvalidUserException -> "Email not found"
                        else -> task.exception?.localizedMessage ?: "Incorrect email or password"
                    }
                    onResult(false)
                }
            }

    }

    fun fetchSavedAccountBook(
        onResult: (String?) -> Unit,
        onError: (String) -> Unit
    ) {
        val email = auth.currentUser?.email?.trim().orEmpty()
        if (email.isBlank()) {
            onResult(null)
            return
        }

        firestore.collection(USERS_COLLECTION)
            .document(email)
            .get()
            .addOnSuccessListener { snapshot ->
                val savedAccountBook = snapshot.getString(ACCOUNT_BOOK_FIELD)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                onResult(savedAccountBook)
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Unable to check saved account book")
            }
    }

    private companion object {
        const val USERS_COLLECTION = "Users"
        const val ACCOUNT_BOOK_FIELD = "AccountBook"
    }
}

class LoginViewModelFactory(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userCredentialDao: UserCredentialDao? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(auth, userCredentialDao, firestore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { continuation ->
    task.addOnCompleteListener { completedTask ->
        if (completedTask.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            continuation.resume(completedTask.result as T)
        } else {
            continuation.resumeWithException(
                completedTask.exception ?: IllegalStateException("Firebase task failed")
            )
        }
    }
}