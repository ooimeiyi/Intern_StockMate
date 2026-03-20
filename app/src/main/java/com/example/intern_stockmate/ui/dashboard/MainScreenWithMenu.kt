package com.example.intern_stockmate.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.intern_stockmate.model.HamburgerScreen
import com.example.intern_stockmate.ui.stockAdjustment.AdjustmentItemsScreen
import com.example.intern_stockmate.ui.stockAdjustment.StockAdjustmentScreen
import com.example.intern_stockmate.ui.stockList.StockDetailScreen
import com.example.intern_stockmate.ui.stockList.StockListScreen
import com.example.intern_stockmate.viewModel.StockAdjustmentViewModel
import com.example.intern_stockmate.viewModel.StockAdjustmentViewModelFactory
import com.example.intern_stockmate.viewModel.StockViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithMenu(navController: NavHostController) {

    val internalNavController = rememberNavController()

    val context = LocalContext.current
    val stockViewModel: StockViewModel = viewModel()
    val stockAdjustmentViewModel: StockAdjustmentViewModel = viewModel(
        factory = StockAdjustmentViewModelFactory(
            application = context.applicationContext as android.app.Application,
            stockViewModel = stockViewModel
        )
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by internalNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val routesWithOwnTopBar = setOf(
        HamburgerScreen.StockList.route,
        HamburgerScreen.StockAdjustment.route,
        "adjustmentDetails",
        "stockDetail"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuScreen(
                navController = navController,
                currentRoute = currentRoute,
                onItemClick = { screen ->
                    scope.launch {
                        drawerState.close()
                        internalNavController.navigate(screen.route) {
                            popUpTo(HamburgerScreen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                })
        }
    ) {
        Scaffold(
            topBar = {
                if (currentRoute !in routesWithOwnTopBar) {
                    TopAppBar(
                        title = {
                            val screen = HamburgerScreen.all.find { it.route == currentRoute }
                            val displayTitle = screen?.topBarTitle ?: "Sales Mate"
                            Text(displayTitle, color = Color.White, fontWeight = FontWeight.Bold)
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Red
                        )
                    )
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                NavHost(
                    navController = internalNavController,
                    startDestination = HamburgerScreen.Dashboard.route
                ) {
                    composable(HamburgerScreen.Dashboard.route) {
                        DashboardScreenContainer(
                            navController = internalNavController
                        )
                    }
                    composable(HamburgerScreen.StockList.route) {
                        StockListScreen(
                            navController = internalNavController,
                            stockViewModel = stockViewModel
                        )
                    }
                    composable("stockDetail") {
                        val item = internalNavController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.get<com.example.intern_stockmate.model.StockItem>("selectedItem")

                        if (item != null) {
                            StockDetailScreen(
                                navController = internalNavController,
                                item = item
                            )
                        } else {
                            PlaceholderScreen("Stock detail is unavailable.")
                        }
                    }
                    composable(HamburgerScreen.StockAdjustment.route) {
                        StockAdjustmentScreen(
                            navController = internalNavController,
                            stockViewModel = stockAdjustmentViewModel
                        )
                    }
                    composable("adjustmentDetails") {
                        AdjustmentItemsScreen(
                            navController = internalNavController,
                            stockViewModel = stockViewModel,
                            stockAdjustmentViewModel = stockAdjustmentViewModel
                        )
                    }
                    composable(HamburgerScreen.SalesOverview.route) { PlaceholderScreen("Sales overview is not wired yet.") }
                    composable(HamburgerScreen.HourlySales.route) { PlaceholderScreen("Hourly sales is not wired yet.") }
                    composable(HamburgerScreen.DailySales.route) { PlaceholderScreen("Daily sales is not wired yet.") }
                    composable(HamburgerScreen.MonthlySales.route) { PlaceholderScreen("Monthly sales is not wired yet.") }
                    composable(HamburgerScreen.Rank.route) { PlaceholderScreen("Sales rank is not wired yet.") }
                    composable(HamburgerScreen.Items.route) { PlaceholderScreen("Item information is not wired yet.") }
                    composable(HamburgerScreen.Members.route) { PlaceholderScreen("Member information is not wired yet.") }
                    composable(HamburgerScreen.Debtor.route) { PlaceholderScreen("Debtor information is not wired yet.") }
                    composable(HamburgerScreen.Creditor.route) { PlaceholderScreen("Creditor information is not wired yet.") }
                    composable(HamburgerScreen.Config.route) { PlaceholderScreen("Configuration is not wired yet.") }
                    composable(HamburgerScreen.Contact.route) { PlaceholderScreen("Contact screen is not wired yet.") }

                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Gray
        )
    }
}