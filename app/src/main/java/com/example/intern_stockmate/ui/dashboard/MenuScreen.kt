package com.example.intern_stockmate.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.intern_stockmate.model.HamburgerScreen

@Composable
fun MenuScreen(
    currentRoute: String?,
    onItemClick: (HamburgerScreen) -> Unit,
    onLogout: () -> Unit
) {
    var isSalesReportExpanded by remember { mutableStateOf(false) }
    var isCrmAccountExpanded by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        drawerContainerColor = Color(0xFF232F3E),
        drawerShape = androidx.compose.ui.graphics.RectangleShape
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = Color.LightGray.copy(alpha = 0.2f)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Administrator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Admin", color = Color.Gray, fontSize = 14.sp)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))

                DrawerMenuItem(
                    screen = HamburgerScreen.Dashboard,
                    currentRoute = currentRoute,
                    onItemClick = onItemClick
                )

                DrawerMenuItem(
                    screen = HamburgerScreen.StockList,
                    currentRoute = currentRoute,
                    onItemClick = onItemClick
                )

                DrawerMenuItem(
                    screen = HamburgerScreen.StockAdjustment,
                    currentRoute = currentRoute,
                    onItemClick = onItemClick
                )

                DrawerMenuItem(
                    screen = HamburgerScreen.SalesOrder,
                    currentRoute = currentRoute,
                    onItemClick = onItemClick
                )

                ExpandableDrawerMenuItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null, tint = Color.White) },
                    label = "Sales Report",
                    isExpanded = isSalesReportExpanded,
                    onClick = { isSalesReportExpanded = !isSalesReportExpanded }
                )

                if (isSalesReportExpanded) {
                    listOf(
                        HamburgerScreen.SalesOverview,
                        HamburgerScreen.HourlySales,
                        HamburgerScreen.DailySales,
                        HamburgerScreen.MonthlySales,
                        HamburgerScreen.Rank
                    ).forEach { screen ->
                        DrawerMenuItem(
                            screen = screen,
                            currentRoute = currentRoute,
                            onItemClick = onItemClick,
                            modifier = Modifier.padding(start = 24.dp)
                        )
                    }
                }

                DrawerMenuItem(
                    screen = HamburgerScreen.Items,
                    currentRoute = currentRoute,
                    onItemClick = onItemClick,
                    titleOverride = "Items Info"
                )

                ExpandableDrawerMenuItem(
                    icon = { Icon(Icons.Default.Group, contentDescription = null, tint = Color.White) },
                    label = "CRM & Account",
                    isExpanded = isCrmAccountExpanded,
                    onClick = { isCrmAccountExpanded = !isCrmAccountExpanded }
                )

                if (isCrmAccountExpanded) {
                    listOf(
                        HamburgerScreen.Members,
                        HamburgerScreen.Debtor,
                        HamburgerScreen.Creditor
                    ).forEach { screen ->
                        DrawerMenuItem(
                            screen = screen,
                            currentRoute = currentRoute,
                            onItemClick = onItemClick,
                            modifier = Modifier.padding(start = 24.dp)
                        )
                    }
                }

                DrawerMenuItem(
                    screen = HamburgerScreen.Config,
                    currentRoute = currentRoute,
                    onItemClick = onItemClick
                )

                DrawerMenuItem(
                    screen = HamburgerScreen.Contact,
                    currentRoute = currentRoute,
                    onItemClick = onItemClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.ExitToApp, contentDescription = "Logout", tint = Color.Red) },
                    label = { Text("Logout", fontWeight = FontWeight.Bold, color = Color.Red) },
                    selected = false,
                    onClick = onLogout,
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = Color.Red,
                        unselectedTextColor = Color.Red
                    ),
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                //HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Version: Beta 1.0.2",
                        color = Color.Gray.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

private val DrawerItemColors
    @Composable
    get() = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = Color.Red,
        selectedIconColor = Color.White,
        selectedTextColor = Color.White,
        unselectedContainerColor = Color.Transparent,
        unselectedIconColor = Color.White,
        unselectedTextColor = Color.White
    )

@Composable
private fun DrawerMenuItem(
    screen: HamburgerScreen,
    currentRoute: String?,
    onItemClick: (HamburgerScreen) -> Unit,
    modifier: Modifier = Modifier,
    titleOverride: String? = null
) {
    NavigationDrawerItem(
        icon = { Icon(screen.icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
        label = { Text(titleOverride ?: screen.title, fontSize = 16.sp) },
        selected = currentRoute == screen.route,
        onClick = { onItemClick(screen) },
        colors = DrawerItemColors,
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .then(modifier)
            .height(52.dp)
    )
}

@Composable
fun ExpandableDrawerMenuItem(
    icon: @Composable () -> Unit,
    label: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 12.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = label, fontSize = 16.sp, color = Color.White)
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}