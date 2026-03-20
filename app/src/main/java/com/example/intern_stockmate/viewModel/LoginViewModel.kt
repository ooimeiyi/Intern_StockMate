package com.example.intern_stockmate.viewModel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {

    // Make credentials mutable
    private var userId by mutableStateOf("Admin")
    private var password by mutableStateOf("Admin")

    // User input
    var inputUserId by mutableStateOf("")
        private set
    var inputPassword by mutableStateOf("")
        private set

    // Login attempt status
    var loginError by mutableStateOf(false)
        private set

    // Hide password
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

    // Login button enabled when fields not empty
    val isLoginEnabled by derivedStateOf {
        inputUserId.isNotBlank() && inputPassword.isNotBlank()
    }

    // Attempt login
    fun attemptLogin(): Boolean {
        return if (inputUserId == userId && inputPassword == password) {
            loginError = false
            true
        } else {
            loginError = true
            false
        }
    }

    // FUNCTION TO UPDATE PASSWORD
    fun updatePassword(newPassword: String) {
        password = newPassword
    }

    fun getCurrentUser(): String = userId
}