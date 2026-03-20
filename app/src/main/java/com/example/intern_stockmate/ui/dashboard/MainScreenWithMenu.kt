package com.example.intern_stockmate.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.intern_stockmate.model.HamburgerScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithMenu(navController: NavHostController) {

    val internalNavController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by internalNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
                    navController = internalNavController,
                    startDestination = HamburgerScreen.Dashboard.route
                ) {
                    composable(HamburgerScreen.Dashboard.route) {
                        DashboardScreenContainer(
                            navController = internalNavController
                        )
                    }

                }
            }
        }
    }
}