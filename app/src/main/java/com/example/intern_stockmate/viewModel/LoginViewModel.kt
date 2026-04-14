package com.example.intern_stockmate.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class LoginViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
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
            loginErrorMessage = "Email and password are required."
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
}

class LoginViewModelFactory(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(auth) as T
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