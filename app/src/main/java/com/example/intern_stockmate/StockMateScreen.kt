package com.example.intern_stockmate


import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.intern_stockmate.ui.dashboard.DashboardScreen
import com.example.intern_stockmate.ui.dashboard.MainScreenWithMenu
import com.example.intern_stockmate.ui.loginScreen.LogInScreen
import com.example.intern_stockmate.viewModel.LoginViewModel

@Composable
fun StockMateScreen() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = viewModel()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        NavHost(
            navController = navController,
            startDestination = "login"
        ) {
            // Login screen
            composable("login") { LogInScreen(navController, loginViewModel) }

            // All main screens in the drawer
            composable("dashboard") { MainScreenWithMenu(navController = navController) }

        }
    }
}