package com.example.intern_stockmate

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.font.FontWeight
import com.example.intern_stockmate.ui.dashboard.MainScreenWithMenu
import com.example.intern_stockmate.model.AccessRole
import com.example.intern_stockmate.ui.access.AccessScreen
import com.example.intern_stockmate.ui.configuration.ConfigScreen
import com.example.intern_stockmate.ui.loginScreen.LogInScreen
import com.example.intern_stockmate.data.CompanyContext
import com.example.intern_stockmate.data.DocumentNumberFormatStore
import com.example.intern_stockmate.data.AccountBookContext
import com.example.intern_stockmate.data.AccessPasswordStore
import com.example.intern_stockmate.data.local.UserCredentialDatabase
import com.example.intern_stockmate.ui.accountBook.AccountBookScreen
import com.example.intern_stockmate.viewModel.LoginViewModel
import com.example.intern_stockmate.viewModel.LoginViewModelFactory

private enum class AuthStage {
    LOGIN,
    ACCOUNT_BOOK,
    ACCESS,
    DASHBOARD,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockMateScreen() {
    val context = LocalContext.current

    val userCredentialDao = remember(context) {
        UserCredentialDatabase.getInstance(context).userCredentialDao()
    }
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(userCredentialDao = userCredentialDao)
    )
    var authStage by rememberSaveable { mutableStateOf(AuthStage.LOGIN.name) }
    var showConfigFromLogin by rememberSaveable { mutableStateOf(false) }
    var accessRole by rememberSaveable { mutableStateOf(AccessRole.ADMIN.name) }
    var isAppDataReady by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        CompanyContext.initialize(context)
        AccountBookContext.initialize(context)
        DocumentNumberFormatStore.initialize(context)
        AccessPasswordStore.initialize(context)
        isAppDataReady = true
    }

    LaunchedEffect(authStage) {
        if (authStage != AuthStage.DASHBOARD.name && accessRole != AccessRole.ADMIN.name) {
            accessRole = AccessRole.ADMIN.name
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        if (!isAppDataReady) {
            return@Surface
        }
        Crossfade(
            targetState = authStage,
            animationSpec = tween(durationMillis = 220),
            label = "auth_navigation"
        ) { stage ->
            if (stage == AuthStage.DASHBOARD.name) {
                MainScreenWithMenu(
                    loginViewModel = loginViewModel,
                    accessRole = AccessRole.valueOf(accessRole),
                    onLogout = {
                        authStage = AuthStage.ACCESS.name
                        showConfigFromLogin = false
                    }
                )

            } else {
                if (stage == AuthStage.ACCESS.name) {
                    AccessScreen(
                        loginViewModel = loginViewModel,
                        onSwitchToLogin = {
                            authStage = AuthStage.LOGIN.name
                            showConfigFromLogin = false
                        },
                        onAccessGranted = { selectedRole ->
                            accessRole = selectedRole.name
                            authStage = AuthStage.DASHBOARD.name
                        }
                    )
                } else if (stage == AuthStage.ACCOUNT_BOOK.name) {
                    AccountBookScreen(
                        onConfirm = {
                            authStage = AuthStage.ACCESS.name
                            showConfigFromLogin = false
                        }
                    )
                } else if (showConfigFromLogin) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Configuration", color = Color.White, fontWeight = FontWeight.Bold) },

                                navigationIcon = {
                                    IconButton(onClick = { showConfigFromLogin = false }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = Color.White
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color(0xFFEF3636)
                                )
                            )
                        }
                    ) { innerPadding ->
                        ConfigScreen(
                            innerPadding = innerPadding,
                            loginViewModel = loginViewModel,
                            scope = scope
                        )
                    }
                } else {
                    LogInScreen(
                        loginViewModel = loginViewModel,
                        onLoginSuccess = {
                            loginViewModel.fetchSavedAccountBook(
                                onResult = { savedAccountBook ->
                                    if (savedAccountBook.isNullOrBlank()) {
                                        authStage = AuthStage.ACCOUNT_BOOK.name
                                    } else {
                                        AccountBookContext.updateSelectedAccountBook(context, savedAccountBook)
                                        authStage = AuthStage.ACCESS.name
                                    }
                                    showConfigFromLogin = false
                                },
                                onError = {
                                    authStage = AuthStage.ACCOUNT_BOOK.name
                                    showConfigFromLogin = false
                                }
                            )
                        },
                    )
                }
            }
        }
    }
}