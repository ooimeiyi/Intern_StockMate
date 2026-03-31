package com.example.intern_stockmate.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.intern_stockmate.data.local.UserCredentialDao
import com.example.intern_stockmate.data.local.UserCredentialEntity
import kotlinx.coroutines.launch

class LoginViewModel(
    private val credentialDao: UserCredentialDao
) : ViewModel() {

    // Make credentials mutable
    private val defaultUserId = "Admin"
    private var userId by mutableStateOf(defaultUserId)
    private var password by mutableStateOf("Admin")

    // User input
    var inputUserId by mutableStateOf("")
        private set
    var inputPassword by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            val storedCredential = credentialDao.getCredential(defaultUserId)
            if (storedCredential == null) {
                credentialDao.upsertCredential(
                    UserCredentialEntity(userId = defaultUserId, password = password)
                )
            } else {
                userId = storedCredential.userId
                password = storedCredential.password
            }
        }
    }

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
    val isLoginEnabled: Boolean
        get() = inputUserId.isNotBlank() && inputPassword.isNotBlank()

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

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        if (oldPassword.isBlank() && newPassword.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter old and new password"))
        }

        if (oldPassword != password) {
            return Result.failure(IllegalArgumentException("Old password is incorrect"))
        }
        if (newPassword.isBlank()) {
            return Result.failure(IllegalArgumentException("New password cannot be empty"))
        }
        if(oldPassword==newPassword){
            return Result.failure(IllegalArgumentException("New password cannot be the same as old password"))
        }

        password = newPassword
        credentialDao.upsertCredential(
            UserCredentialEntity(userId = userId, password = newPassword)
        )
        return Result.success(Unit)
    }
}

class LoginViewModelFactory(
    private val credentialDao: UserCredentialDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(credentialDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}