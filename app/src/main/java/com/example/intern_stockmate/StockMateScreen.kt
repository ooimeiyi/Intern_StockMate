package com.example.intern_stockmate


import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.intern_stockmate.ui.dashboard.MainScreenWithMenu
import com.example.intern_stockmate.ui.loginScreen.LogInScreen
import com.example.intern_stockmate.viewModel.LoginViewModel

@Composable
fun StockMateScreen() {
    val loginViewModel: LoginViewModel = viewModel()
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }

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
                    onLogout = {
                        isLoggedIn = false
                        loginViewModel.clearFields()
                    }
                )
            } else {
                LogInScreen(
                    loginViewModel = loginViewModel,
                    onLoginSuccess = {
                        isLoggedIn = true
                        loginViewModel.clearFields()
                    }
                )
            }
        }
    }
}