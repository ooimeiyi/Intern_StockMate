package com.example.intern_stockmate.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.*
import com.example.intern_stockmate.model.HamburgerScreen
import com.example.intern_stockmate.ui.contact.ContactScreen
import com.example.intern_stockmate.ui.creditor.CreditorInfoScreenContainer
import com.example.intern_stockmate.ui.dailySales.DailySalesScreenContainer
import com.example.intern_stockmate.ui.debtor.DebtorInfoScreenContainer
import com.example.intern_stockmate.ui.hourlySales.HourlySalesScreenContainer
import com.example.intern_stockmate.ui.itemInfo.ItemInfoScreenContainer
import com.example.intern_stockmate.ui.member.MemberInfoScreenContainer
import com.example.intern_stockmate.ui.monthlySales.MonthlySalesScreenContainer
import com.example.intern_stockmate.ui.salesOverview.SalesOverviewScreenContainer
import com.example.intern_stockmate.ui.stockLIst.StockDetailScreen
import com.example.intern_stockmate.ui.stockLIst.StockListScreenContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithMenu(
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun navigateToScreen(screen: HamburgerScreen) {
        navController.navigate(screen.route) {
            popUpTo(HamburgerScreen.Dashboard.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuScreen(
                currentRoute = currentRoute,
                onItemClick = { screen ->
                    scope.launch {
                        drawerState.close()
                        navigateToScreen(screen)
                    }
                },
                onLogout = {
                    scope.launch {
                        drawerState.close()
                        navController.popBackStack(HamburgerScreen.Dashboard.route, false)
                        onLogout()
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                if (currentRoute != "stockDetail") {
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
                    navController = navController,
                    startDestination = HamburgerScreen.Dashboard.route
                ) {
                    composable(HamburgerScreen.Dashboard.route) {
                        DashboardScreen(onNavigate = ::navigateToScreen)
                    }

                    composable(HamburgerScreen.StockList.route) {
                        StockListScreenContainer(navController = navController)
                    }

                    composable("stockDetail") {
                        val item = navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.get<com.example.intern_stockmate.model.StockItem>("selectedStockItem")
                        if (item != null) {
                            StockDetailScreen(navController = navController, item = item)
                        }
                    }

                    composable(HamburgerScreen.StockAdjustment.route) { }

                    composable(HamburgerScreen.StockOrder.route) { }

                    composable(HamburgerScreen.SalesOverview.route) {
                        SalesOverviewScreenContainer()
                    }


                    composable(HamburgerScreen.HourlySales.route) {
                        HourlySalesScreenContainer()
                    }

                    composable(HamburgerScreen.DailySales.route) {
                        DailySalesScreenContainer()
                    }

                    composable(HamburgerScreen.MonthlySales.route) {
                        MonthlySalesScreenContainer()
                    }

                    composable(HamburgerScreen.Rank.route) { }

                    composable(HamburgerScreen.Items.route) {
                        ItemInfoScreenContainer()
                    }

                    composable(HamburgerScreen.Members.route) {
                        MemberInfoScreenContainer()
                    }

                    composable(HamburgerScreen.Debtor.route) {
                        DebtorInfoScreenContainer()
                    }

                    composable(HamburgerScreen.Creditor.route) {
                        CreditorInfoScreenContainer()
                    }

                    composable(HamburgerScreen.Config.route) { }


                    composable(HamburgerScreen.Contact.route) {
                        ContactScreen()
                    }

                }
            }
        }
    }
}