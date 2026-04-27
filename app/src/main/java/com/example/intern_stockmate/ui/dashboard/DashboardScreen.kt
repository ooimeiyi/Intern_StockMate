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

private data class DashboardCardConfig(
    val title: String,
    val icon: ImageVector,
    val screen: HamburgerScreen
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigate: (HamburgerScreen) -> Unit,
    accessRole: AccessRole,
    allowedStockRoutes: Set<String> = emptySet()
) {
    val isStockOnly = accessRole == AccessRole.STOCK
    fun canAccess(screen: HamburgerScreen): Boolean = !isStockOnly || allowedStockRoutes.contains(screen.route)
    fun availableCards(cards: List<DashboardCardConfig>): List<DashboardCardConfig> =
        cards.filter { canAccess(it.screen) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
    ) {
        item { SectionHeader("Operation Functions") }

        item {
            val operationCards = availableCards(
                listOf(
                    DashboardCardConfig("Stock List", Icons.Default.Inventory, HamburgerScreen.StockList),
                    DashboardCardConfig("Stock Take", Icons.Default.SyncAlt, HamburgerScreen.StockTake),
                    DashboardCardConfig("Sales Order", Icons.Default.ShoppingCart, HamburgerScreen.SalesOrder)
                )
            )
            DashboardCardRows(cards = operationCards, onNavigate = onNavigate)
        }

        val groupSalesCards = availableCards(
            listOf(
                DashboardCardConfig("Cash Sales", Icons.Default.PointOfSale, HamburgerScreen.CashSales),
                DashboardCardConfig("Invoice", Icons.Default.ReceiptLong, HamburgerScreen.Invoice)
            )
        )
        if (groupSalesCards.isNotEmpty()) {
            item { SectionHeader("Group Sales") }
            item { DashboardCardRows(cards = groupSalesCards, onNavigate = onNavigate) }
        }

        val salesCards = availableCards(
            listOf(
                DashboardCardConfig("Sales Overview", Icons.Default.PieChart, HamburgerScreen.SalesOverview),
                DashboardCardConfig("Hourly Sales", Icons.Default.Schedule, HamburgerScreen.HourlySales),
                DashboardCardConfig("Daily Sales", Icons.Default.Today, HamburgerScreen.DailySales),
                DashboardCardConfig("Monthly Sales", Icons.Default.DateRange, HamburgerScreen.MonthlySales),
                DashboardCardConfig("Sales Rank", Icons.Default.TrendingUp, HamburgerScreen.Rank)
            )
        )
        if (salesCards.isNotEmpty()) {
            item { SectionHeader("Sales Report") }

            item { DashboardCardRows(cards = salesCards, onNavigate = onNavigate) }

        }

        val itemInfoCards = availableCards(
            listOf(DashboardCardConfig("Item Info", Icons.Default.Info, HamburgerScreen.Items))
        )
        if (itemInfoCards.isNotEmpty()) {
            item { SectionHeader("Item Info") }

            item { DashboardCardRows(cards = itemInfoCards, onNavigate = onNavigate) }
        }

        val crmCards = availableCards(
            listOf(
                DashboardCardConfig("Member Info", Icons.Default.AccountCircle, HamburgerScreen.Members),
                DashboardCardConfig("Debtor Info", Icons.Default.BusinessCenter, HamburgerScreen.Debtor),
                DashboardCardConfig("Creditor Info", Icons.Default.CreditCard, HamburgerScreen.Creditor)
            )
        )
        if (crmCards.isNotEmpty()) {
            item { SectionHeader("CRM & Account") }

            item { DashboardCardRows(cards = crmCards, onNavigate = onNavigate) }

        }

        val configurationCards = availableCards(
            listOf(
                DashboardCardConfig("Configuration", Icons.Default.Settings, HamburgerScreen.Config),
                DashboardCardConfig("Contact Us", Icons.Default.Phone, HamburgerScreen.Contact)
            )
        )
        if (configurationCards.isNotEmpty()) {
            item { SectionHeader("Configuration and Contact") }

            item { DashboardCardRows(cards = configurationCards, onNavigate = onNavigate) }
        }
    }
}

@Composable
private fun DashboardCardRows(
    cards: List<DashboardCardConfig>,
    onNavigate: (HamburgerScreen) -> Unit
) {
    cards.chunked(2).forEach { rowCards ->
        Row(Modifier.padding(horizontal = 8.dp)) {
            rowCards.forEach { card ->
                Box(Modifier.weight(1f)) {
                    DashboardCard(title = card.title, icon = card.icon) { onNavigate(card.screen) }
                }
            }
            if (rowCards.size == 1) {
                Spacer(Modifier.weight(1f))
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