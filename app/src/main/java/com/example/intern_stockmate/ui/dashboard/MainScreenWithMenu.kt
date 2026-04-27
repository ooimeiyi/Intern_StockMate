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
import com.example.intern_stockmate.model.AccessRole
import com.example.intern_stockmate.model.HamburgerScreen
import com.example.intern_stockmate.model.StockAccessRights
import com.example.intern_stockmate.ui.cashSales.CashSalesScreen
import com.example.intern_stockmate.ui.configuration.ConfigScreen
import com.example.intern_stockmate.ui.contact.ContactScreen
import com.example.intern_stockmate.ui.creditor.CreditorInfoScreenContainer
import com.example.intern_stockmate.ui.dailySales.DailySalesScreenContainer
import com.example.intern_stockmate.ui.debtor.DebtorInfoScreenContainer
import com.example.intern_stockmate.ui.hourlySales.HourlySalesScreenContainer
import com.example.intern_stockmate.ui.invoice.InvoiceScreen
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
import com.example.intern_stockmate.viewModel.ConfigurationViewModel
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
    accessRole: AccessRole,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val configurationViewModel: ConfigurationViewModel = viewModel()
    val enabledStockAccessRoutes by configurationViewModel.enabledStockAccessRoutes.collectAsState()

    val allowedScreens = remember(accessRole, enabledStockAccessRoutes) {
        if (accessRole == AccessRole.STOCK) {
            StockAccessRights.stockAllowedRoutesFromRemote(enabledStockAccessRoutes) + setOf("stockDetail")
        } else {
            setOf(
                HamburgerScreen.Dashboard.route,
                HamburgerScreen.StockList.route,
                HamburgerScreen.StockTake.route,
                HamburgerScreen.SalesOrder.route,
                HamburgerScreen.SalesOverview.route,
                HamburgerScreen.HourlySales.route,
                HamburgerScreen.DailySales.route,
                HamburgerScreen.MonthlySales.route,
                HamburgerScreen.Rank.route,
                HamburgerScreen.Items.route,
                HamburgerScreen.Members.route,
                HamburgerScreen.Debtor.route,
                HamburgerScreen.Creditor.route,
                HamburgerScreen.Config.route,
                HamburgerScreen.Contact.route,
                "stockDetail",
                "adjustmentDetails",
                "salesOrderDetails"
            )
        }
    }

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
    val scope = rememberCoroutineScope()

    fun navigateToScreen(screen: HamburgerScreen) {
        if (!allowedScreens.contains(screen.route)) return
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
                        HamburgerScreen.Dashboard.route -> HamburgerScreen.Dashboard.topBarTitle
                        HamburgerScreen.StockList.route -> HamburgerScreen.StockList.topBarTitle
                        HamburgerScreen.StockTake.route -> HamburgerScreen.StockTake.topBarTitle
                        HamburgerScreen.SalesOrder.route -> HamburgerScreen.SalesOrder.topBarTitle
                        HamburgerScreen.SalesOverview.route -> HamburgerScreen.SalesOverview.topBarTitle
                        HamburgerScreen.HourlySales.route -> HamburgerScreen.HourlySales.topBarTitle
                        HamburgerScreen.DailySales.route -> HamburgerScreen.DailySales.topBarTitle
                        HamburgerScreen.MonthlySales.route -> HamburgerScreen.MonthlySales.topBarTitle
                        HamburgerScreen.Rank.route -> HamburgerScreen.Rank.topBarTitle
                        HamburgerScreen.Items.route -> HamburgerScreen.Items.topBarTitle
                        HamburgerScreen.Members.route -> HamburgerScreen.Members.topBarTitle
                        HamburgerScreen.Debtor.route -> HamburgerScreen.Debtor.topBarTitle
                        HamburgerScreen.Creditor.route -> HamburgerScreen.Creditor.topBarTitle
                        HamburgerScreen.Config.route -> HamburgerScreen.Config.topBarTitle
                        HamburgerScreen.Contact.route -> HamburgerScreen.Contact.topBarTitle
                        else -> "Sales Mate"
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
                    DashboardScreen(
                        onNavigate = ::navigateToScreen,
                        accessRole = accessRole,
                        allowedStockRoutes = allowedScreens
                    )
                }

                if (allowedScreens.contains(HamburgerScreen.StockList.route)) {
                    composable(HamburgerScreen.StockList.route) {
                        StockListScreenContainer(navController = navController)
                    }
                }

                if (allowedScreens.contains("stockDetail")) {
                    composable("stockDetail") {
                    val item = navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.get<com.example.intern_stockmate.model.StockItem>("selectedStockItem")
                        if (item != null) {
                            StockDetailScreen(item = item)
                        }
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.StockTake.route)) {
                    composable(HamburgerScreen.StockTake.route) {
                        StockAdjustmentScreen(navController = navController, stockViewModel = stockAdjustmentViewModel)
                    }
                }

                if (allowedScreens.contains("adjustmentDetails")) {
                    composable("adjustmentDetails") {
                        AdjustmentItemsScreen(
                            navController = navController,
                            stockViewModel = stockViewModel,
                            stockAdjustmentViewModel = stockAdjustmentViewModel
                        )
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.SalesOrder.route)) {
                    composable(HamburgerScreen.SalesOrder.route) {
                        SalesOrderScreen(
                            navController = navController,
                            salesOrderViewModel = salesOrderViewModel
                        )
                    }
                }

                if (allowedScreens.contains("salesOrderDetails")) {
                    composable("salesOrderDetails") {
                        SalesOrderDetailsScreen(
                            navController = navController,
                            stockViewModel = stockViewModel,
                            salesOrderViewModel = salesOrderViewModel
                        )
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.CashSales.route)) {
                    composable(HamburgerScreen.CashSales.route) {
                        CashSalesScreen()
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.Invoice.route)) {
                    composable(HamburgerScreen.Invoice.route) {
                        InvoiceScreen()
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.SalesOverview.route)) {
                    composable(HamburgerScreen.SalesOverview.route) {
                        SalesOverviewScreenContainer()
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.HourlySales.route)) {
                    composable(HamburgerScreen.HourlySales.route) {
                        HourlySalesScreenContainer()
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.DailySales.route)) {
                    composable(HamburgerScreen.DailySales.route) {
                        DailySalesScreenContainer()
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.MonthlySales.route)) {
                    composable(HamburgerScreen.MonthlySales.route) {
                        MonthlySalesScreenContainer()
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.Rank.route)) {
                    composable(HamburgerScreen.Rank.route) {
                        SalesRankScreenContainer()
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.Items.route)) {
                    composable(HamburgerScreen.Items.route) {
                        ItemInfoScreenContainer()
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.Members.route)) {
                    composable(HamburgerScreen.Members.route) {
                        MemberInfoScreenContainer()
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.Debtor.route)) {
                    composable(HamburgerScreen.Debtor.route) {
                        DebtorInfoScreenContainer()
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.Creditor.route)) {
                    composable(HamburgerScreen.Creditor.route) {
                        CreditorInfoScreenContainer()
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.Config.route)) {
                    composable(HamburgerScreen.Config.route) {
                        ConfigScreen(
                            innerPadding = PaddingValues(0.dp),
                            loginViewModel = loginViewModel,
                            scope = scope
                        )
                    }
                }

                if (allowedScreens.contains(HamburgerScreen.Contact.route)) {
                    composable(HamburgerScreen.Contact.route) {
                        ContactScreen()
                    }
                }
            }
        }
    }
}



