package com.example.intern_stockmate.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Default access for Stock role is always:
 * - Stock List
 * - Sales Order
 * - Stock Adjustment
 */
object StockAccessRights {
    val defaultAllowedRoutes: Set<String> = setOf(
        HamburgerScreen.Dashboard.route,
        HamburgerScreen.StockList.route,
        HamburgerScreen.SalesOrder.route,
        "salesOrderDetails",
        HamburgerScreen.StockTake.route,
        "adjustmentDetails"
    )

    val configurableRights: List<StockAccessRightOption> = listOf(
        StockAccessRightOption(
            route = HamburgerScreen.SalesOverview.route,
            title = "Sales Overview",
            subtitle = "Allow stock staff to view sales summary dashboards.",
            icon = Icons.Default.BarChart
        ),
        StockAccessRightOption(
            route = HamburgerScreen.HourlySales.route,
            title = "Hourly Sales",
            subtitle = "Allow stock staff to view hourly sales performance.",
            icon = Icons.Default.Today
        ),
        StockAccessRightOption(
            route = HamburgerScreen.DailySales.route,
            title = "Daily Sales",
            subtitle = "Allow stock staff to view daily sales data.",
            icon = Icons.Default.Today
        ),
        StockAccessRightOption(
            route = HamburgerScreen.MonthlySales.route,
            title = "Monthly Sales",
            subtitle = "Allow stock staff to view monthly sales reports.",
            icon = Icons.Default.Today
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Rank.route,
            title = "Sales Rank",
            subtitle = "Allow stock staff to view top selling item rankings.",
            icon = Icons.Default.TrendingUp
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Items.route,
            title = "Item Info",
            subtitle = "Allow stock staff to view item information.",
            icon = Icons.Default.Info
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Members.route,
            title = "Member Info",
            subtitle = "Allow stock staff to view member records.",
            icon = Icons.Default.AccountCircle
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Debtor.route,
            title = "Debtor Info",
            subtitle = "Allow stock staff to view debtor information.",
            icon = Icons.Default.BusinessCenter
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Creditor.route,
            title = "Creditor Info",
            subtitle = "Allow stock staff to view creditor information.",
            icon = Icons.Default.CreditCard
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Contact.route,
            title = "Contact",
            subtitle = "Allow stock staff to view the contact screen.",
            icon = Icons.Default.Info
        )
    )

    fun stockAllowedRoutesFromRemote(enabledRoutes: Set<String>): Set<String> {
        val safeEnabled = enabledRoutes.intersect(configurableRights.map { it.route }.toSet())
        return defaultAllowedRoutes + safeEnabled
    }
}

data class StockAccessRightOption(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)