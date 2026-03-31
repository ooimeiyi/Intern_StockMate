package com.example.intern_stockmate.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.intern_stockmate.model.HamburgerScreen
import com.example.intern_stockmate.ui.configuration.ConfigScreen
import com.example.intern_stockmate.ui.contact.ContactScreen
import com.example.intern_stockmate.ui.creditor.CreditorInfoScreenContainer
import com.example.intern_stockmate.ui.dailySales.DailySalesScreenContainer
import com.example.intern_stockmate.ui.debtor.DebtorInfoScreenContainer
import com.example.intern_stockmate.ui.hourlySales.HourlySalesScreenContainer
import com.example.intern_stockmate.ui.itemInfo.ItemInfoScreenContainer
import com.example.intern_stockmate.ui.member.MemberInfoScreenContainer
import com.example.intern_stockmate.ui.monthlySales.MonthlySalesScreenContainer
import com.example.intern_stockmate.ui.salesOrder.SalesOrderDetailsScreen
import com.example.intern_stockmate.ui.salesOrder.SalesOrderScreen
import com.example.intern_stockmate.ui.salesOverview.SalesOverviewScreenContainer
import com.example.intern_stockmate.ui.salesRank.SalesRankScreenContainer
import com.example.intern_stockmate.ui.stockLIst.StockDetailScreen
import com.example.intern_stockmate.ui.stockAdjustment.AdjustmentItemsScreen
import com.example.intern_stockmate.ui.stockAdjustment.StockAdjustmentScreen
import com.example.intern_stockmate.ui.stockLIst.StockListScreenContainer
import com.example.intern_stockmate.viewModel.LoginViewModel
import com.example.intern_stockmate.viewModel.SalesOrderViewModel
import com.example.intern_stockmate.viewModel.SalesOrderViewModelFactory
import com.example.intern_stockmate.viewModel.StockAdjustmentViewModel
import com.example.intern_stockmate.viewModel.StockAdjustmentViewModelFactory
import com.example.intern_stockmate.viewModel.StockViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithMenu(
    loginViewModel: LoginViewModel,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    val stockViewModel: StockViewModel = viewModel()
    val stockAdjustmentViewModel: StockAdjustmentViewModel = viewModel(
        factory = StockAdjustmentViewModelFactory(
            application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
            stockViewModel = stockViewModel
        )
    )

    val salesOrderViewModel: SalesOrderViewModel = viewModel(
        factory = SalesOrderViewModelFactory(
            application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
            stockViewModel = stockViewModel
        )
    )

    val isEditMode by stockAdjustmentViewModel.isEditMode.collectAsState()
    val isSalesOrderEditMode by salesOrderViewModel.isEditMode.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun navigateToScreen(screen: HamburgerScreen) {
        navController.navigate(screen.route) {
            popUpTo(HamburgerScreen.Dashboard.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToDashboard() {
        navController.navigate(HamburgerScreen.Dashboard.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
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
                val isDetailRoute = currentRoute in setOf("stockDetail", "adjustmentDetails", "salesOrderDetails")
                val isDashboardRoute = currentRoute == null || currentRoute == HamburgerScreen.Dashboard.route
                TopAppBar(
                    title = {
                        val displayTitle = when (currentRoute) {
                            "stockDetail" -> "Stock Details"
                            "adjustmentDetails" -> if (isEditMode) "Edit Stock Take" else "New Stock Take"
                            "salesOrderDetails" -> if (isSalesOrderEditMode) "Edit Sales Order" else "New Sales Order"
                            else -> HamburgerScreen.all.find { it.route == currentRoute }?.topBarTitle ?: "Sales Mate"
                        }
                        Text(displayTitle, color = Color.White, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        if (!isDashboardRoute) {
                            IconButton(
                                onClick = {
                                    if (isDetailRoute) {
                                        if (currentRoute == "adjustmentDetails") {
                                            navController.previousBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("selectedTab", 2)
                                        }
                                        navController.popBackStack()
                                    } else {
                                        navigateToDashboard()
                                    }
                                }
                            ) {
                                if (isDetailRoute) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                } else {
                                    // Home icon with square box
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = Color.White.copy(alpha = 0f),
                                                shape = RoundedCornerShape(2.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = "Home",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    },
                    actions = {
                        if (isDashboardRoute) {
                            IconButton(onClick = onLogout) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Logout",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFEF3636)
                    )
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(paddingValues)
            ) {
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
                            StockDetailScreen(item = item)
                        }
                    }

                    composable(HamburgerScreen.StockAdjustment.route) {
                        StockAdjustmentScreen(navController = navController, stockViewModel = stockAdjustmentViewModel)
                    }

                    composable("adjustmentDetails") {
                        AdjustmentItemsScreen(
                            navController = navController,
                            stockViewModel = stockViewModel,
                            stockAdjustmentViewModel = stockAdjustmentViewModel
                        )
                    }

                    composable(HamburgerScreen.SalesOrder.route) {
                        SalesOrderScreen(
                            navController = navController,
                            salesOrderViewModel = salesOrderViewModel
                        )
                    }

                    composable("salesOrderDetails") {
                        SalesOrderDetailsScreen(
                            navController = navController,
                            stockViewModel = stockViewModel,
                            salesOrderViewModel = salesOrderViewModel
                        )
                    }

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

                    composable(HamburgerScreen.Rank.route) {
                        SalesRankScreenContainer()
                    }

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

                    composable(HamburgerScreen.Config.route) {
                        ConfigScreen(
                            innerPadding = PaddingValues(0.dp),
                            loginViewModel = loginViewModel,
                            scope = scope
                        )
                    }


                    composable(HamburgerScreen.Contact.route) {
                        ContactScreen()
                    }

                }
            }
        }
    }
}