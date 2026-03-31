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
import androidx.compose.ui.text.font.FontWeight
import com.example.intern_stockmate.ui.dashboard.MainScreenWithMenu
import com.example.intern_stockmate.ui.configuration.ConfigScreen
import com.example.intern_stockmate.ui.loginScreen.LogInScreen
import com.example.intern_stockmate.data.local.UserCredentialDatabase
import com.example.intern_stockmate.viewModel.LoginViewModel
import com.example.intern_stockmate.viewModel.LoginViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockMateScreen() {
    val context = LocalContext.current
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(
            UserCredentialDatabase.getInstance(context).userCredentialDao()
        )
    )
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var showConfigFromLogin by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Crossfade(
            targetState = isLoggedIn,
            animationSpec = tween(durationMillis = 220),
            label = "auth_navigation"
        ) { loggedIn ->
            if (loggedIn) {
                MainScreenWithMenu(
                    loginViewModel = loginViewModel,
                    onLogout = {
                        isLoggedIn = false
                        showConfigFromLogin = false
                        loginViewModel.clearFields()
                    }
                )
            } else {
                if (showConfigFromLogin) {
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
                                    containerColor = Color.Red
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
                            isLoggedIn = true
                            showConfigFromLogin = false
                            loginViewModel.clearFields()
                        },
                        onSettingsClick = {
                            showConfigFromLogin = true
                        }
                    )
                }
            }
        }
    }
}