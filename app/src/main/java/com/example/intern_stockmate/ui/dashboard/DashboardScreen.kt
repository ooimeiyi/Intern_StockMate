package com.example.intern_stockmate.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.intern_stockmate.model.AccessRole
import com.example.intern_stockmate.model.HamburgerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigate: (HamburgerScreen) -> Unit,
    accessRole: AccessRole,
    allowedStockRoutes: Set<String> = emptySet()
) {
    val isStockOnly = accessRole == AccessRole.STOCK
    fun canAccess(screen: HamburgerScreen): Boolean = !isStockOnly || allowedStockRoutes.contains(screen.route)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
    ) {
        item { SectionHeader("Operation Functions") }

        item {
            Row(Modifier.padding(horizontal = 8.dp)) {
                Box(Modifier.weight(1f)) {
                    DashboardCard("Stock List", Icons.Default.Inventory) { onNavigate(HamburgerScreen.StockList) }
                }
                Box(Modifier.weight(1f)) {
                    if (canAccess(HamburgerScreen.StockTake)) {
                        DashboardCard("Stock Take", Icons.Default.SyncAlt) { onNavigate(HamburgerScreen.StockTake) }
                    }
                }
            }

            if (canAccess(HamburgerScreen.SalesOrder)) {
                Row(Modifier.padding(horizontal = 8.dp)) {
                    Box(Modifier.weight(1f)) {
                        DashboardCard("Sales Order", Icons.Default.ShoppingCart) { onNavigate(HamburgerScreen.SalesOrder) }
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        val salesScreens = listOf(
            HamburgerScreen.SalesOverview,
            HamburgerScreen.HourlySales,
            HamburgerScreen.DailySales,
            HamburgerScreen.MonthlySales,
            HamburgerScreen.Rank
        )
        if (salesScreens.any(::canAccess)) {
            item { SectionHeader("Sales Report") }

            item {
                Row(Modifier.padding(horizontal = 8.dp)) {
                    Box(Modifier.weight(1f)) {
                        if (canAccess(HamburgerScreen.SalesOverview)) {
                            DashboardCard("Sales Overview", Icons.Default.PieChart) {
                                onNavigate(
                                    HamburgerScreen.SalesOverview
                                )
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        if (canAccess(HamburgerScreen.HourlySales)) {
                            DashboardCard("Hourly Sales", Icons.Default.Schedule) {
                                onNavigate(
                                    HamburgerScreen.HourlySales
                                )
                            }
                        }
                    }
                }


                Row(Modifier.padding(horizontal = 8.dp)) {
                    Box(Modifier.weight(1f)) {
                        if (canAccess(HamburgerScreen.DailySales)) {
                            DashboardCard("Daily Sales", Icons.Default.Today) {
                                onNavigate(
                                    HamburgerScreen.DailySales
                                )
                            }
                        }
                    }

                    Box(Modifier.weight(1f)) {
                        if (canAccess(HamburgerScreen.MonthlySales)) {
                            DashboardCard("Monthly Sales", Icons.Default.DateRange) {
                                onNavigate(
                                    HamburgerScreen.MonthlySales
                                )
                            }
                        }
                    }
                }


                Row(Modifier.padding(horizontal = 8.dp)) {
                    Box(Modifier.weight(1f)) {
                        if (canAccess(HamburgerScreen.Rank)) {
                            DashboardCard("Sales Rank", Icons.Default.TrendingUp) {
                                onNavigate(
                                    HamburgerScreen.Rank
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        if (canAccess(HamburgerScreen.Items)) {
            item { SectionHeader("Item Info") }

            item {
                Row(Modifier.padding(horizontal = 8.dp)) {
                    Box(Modifier.weight(1f)) {
                        DashboardCard("Item Info", Icons.Default.Info) { onNavigate(HamburgerScreen.Items) }
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        val crmScreens = listOf(HamburgerScreen.Members, HamburgerScreen.Debtor, HamburgerScreen.Creditor)
        if (crmScreens.any(::canAccess)) {
            item { SectionHeader("CRM & Account") }

            item {
                Row(Modifier.padding(horizontal = 8.dp)) {
                    Box(Modifier.weight(1f)) {
                        if (canAccess(HamburgerScreen.Members)) {
                            DashboardCard("Member Info", Icons.Default.AccountCircle) {
                                onNavigate(
                                    HamburgerScreen.Members
                                )
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        if (canAccess(HamburgerScreen.Debtor)) {
                            DashboardCard("Debtor Info", Icons.Default.BusinessCenter) {
                                onNavigate(
                                    HamburgerScreen.Debtor
                                )
                            }
                        }
                    }
                }


                Row(Modifier.padding(horizontal = 8.dp)) {
                    Box(Modifier.weight(1f)) {
                        if (canAccess(HamburgerScreen.Creditor)) {
                            DashboardCard("Creditor Info", Icons.Default.CreditCard) {
                                onNavigate(
                                    HamburgerScreen.Creditor
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        if (!isStockOnly || canAccess(HamburgerScreen.Config) || canAccess(HamburgerScreen.Contact)) {
            item { SectionHeader("Configuration and Contact") }
            item {
                Row(Modifier.padding(horizontal = 8.dp)) {
                    Box(Modifier.weight(1f)) {
                        if (canAccess(HamburgerScreen.Config)) {
                            DashboardCard("Configuration", Icons.Default.Settings) { onNavigate(HamburgerScreen.Config) }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        if (canAccess(HamburgerScreen.Contact)) {
                            DashboardCard("Contact Us", Icons.Default.Phone) { onNavigate(HamburgerScreen.Contact) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFFEF3636),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 15.sp,
                    color = Color.Black
                )
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(Color(0xFFEF3636), shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = TextStyle(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Color.Black
            )
        )
    }
}