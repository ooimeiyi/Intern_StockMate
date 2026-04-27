package com.example.intern_stockmate.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
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
            remoteKey = "SalesOverview",
            title = "Sales Overview",
            subtitle = "Allow stock staff to view sales summary dashboards.",
            icon = Icons.Default.BarChart
        ),
        StockAccessRightOption(
            route = HamburgerScreen.HourlySales.route,
            remoteKey = "HourlySales",
            title = "Hourly Sales",
            subtitle = "Allow stock staff to view hourly sales reports.",
            icon = Icons.Default.Today
        ),
        StockAccessRightOption(
            route = HamburgerScreen.DailySales.route,
            remoteKey = "DailySales",
            title = "Daily Sales",
            subtitle = "Allow stock staff to view daily sales reports.",
            icon = Icons.Default.Today
        ),
        StockAccessRightOption(
            route = HamburgerScreen.MonthlySales.route,
            remoteKey = "MonthlySales",
            title = "Monthly Sales",
            subtitle = "Allow stock staff to view monthly sales reports.",
            icon = Icons.Default.Today
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Rank.route,
            remoteKey = "SalesRank",
            title = "Sales Rank",
            subtitle = "Allow stock staff to view top selling item rankings.",
            icon = Icons.Default.TrendingUp
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Items.route,
            remoteKey = "ItemInfo",
            title = "Item Info",
            subtitle = "Allow stock staff to view item information.",
            icon = Icons.Default.Info
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Members.route,
            remoteKey = "MemberInfo",
            title = "Member Info",
            subtitle = "Allow stock staff to view member records.",
            icon = Icons.Default.AccountCircle
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Debtor.route,
            remoteKey = "DebtorInfo",
            title = "Debtor Info",
            subtitle = "Allow stock staff to view debtor information.",
            icon = Icons.Default.BusinessCenter
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Creditor.route,
            remoteKey = "CreditorInfo",
            title = "Creditor Info",
            subtitle = "Allow stock staff to view creditor information.",
            icon = Icons.Default.CreditCard
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Config.route,
            remoteKey = "Configuration",
            title = "Configuration",
            subtitle = "Allow stock staff to view the configuration screen.",
            icon = Icons.Default.Settings
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Contact.route,
            remoteKey = "Contact",
            title = "Contact",
            subtitle = "Allow stock staff to view the contact screen.",
            icon = Icons.Default.Info
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Contact.route,
            remoteKey = "CashSales",
            title = "Cash Sales",
            subtitle = "Allow stock staff to view the Cash Sales screen.",
            icon = Icons.Default.Info
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Contact.route,
            remoteKey = "Invoice",
            title = "Invoice",
            subtitle = "Allow stock staff to view the Invoice screen.",
            icon = Icons.Default.Info
        ),
        StockAccessRightOption(
            route = HamburgerScreen.CashSales.route,
            remoteKey = "CashSales",
            title = "Cash Sales",
            subtitle = "Allow stock staff to open the cash sales screen.",
            icon = Icons.Default.PointOfSale
        ),
        StockAccessRightOption(
            route = HamburgerScreen.Invoice.route,
            remoteKey = "Invoice",
            title = "Invoice",
            subtitle = "Allow stock staff to open the invoice screen.",
            icon = Icons.Default.ReceiptLong
        )
    )

    private fun normalizeKey(raw: String): String =
        raw.filter { it.isLetterOrDigit() }.lowercase()

    private val routeByNormalizedKey: Map<String, String> = buildMap {
        configurableRights.forEach { option ->
            put(normalizeKey(option.route), option.route)
            put(normalizeKey(option.remoteKey), option.route)
        }
    }

    fun routeFromRemoteKey(rawKey: String): String? = routeByNormalizedKey[normalizeKey(rawKey)]

    fun remoteKeyForRoute(route: String): String =
        configurableRights.firstOrNull { it.route == route }?.remoteKey ?: route

    fun stockAllowedRoutesFromRemote(enabledRoutes: Set<String>): Set<String> {
        val safeEnabled = enabledRoutes.intersect(configurableRights.map { it.route }.toSet())
        return defaultAllowedRoutes + safeEnabled
    }
}

data class StockAccessRightOption(
    val route: String,
    val remoteKey: String = route,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)