package com.example.intern_stockmate.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class HamburgerScreen(
    val route: String,
    val title: String,
    val topBarTitle: String,  // This is for the Top App Bar
    val icon: ImageVector) {

    //sales overview
    object Dashboard : HamburgerScreen("dashboard", "Dashboard","Dashboard", Icons.Default.Home)
    object StockList : HamburgerScreen("stockList", "Stock List", "Stock List",Icons.Default.Inventory2)

    object StockTake : HamburgerScreen("stockTake", "Stock Adjustment", "Stock Take", Icons.Default.SyncAlt)
    object SalesOrder : HamburgerScreen("salesOrder", "Sales Order", "Sales Order",Icons.Default.ShoppingCart )

    object SalesOverview : HamburgerScreen("salesOverview", "Sales Overview","Sales Overview", Icons.Default.PieChart)
    object HourlySales : HamburgerScreen("hourlySales", "Hourly Sales", "Hourly Sales",Icons.Default.Schedule )
    object DailySales : HamburgerScreen("dailySales", "Daily Sales", "Daily Sales",Icons.Default.Today )
    object MonthlySales : HamburgerScreen("monthlySales", "Monthly Sales", "Monthly Sales",Icons.Default.DateRange )
    object Rank : HamburgerScreen("rank", "Sales Rank", "Top Daily Sales Item",Icons.Default.TrendingUp)
    object Items : HamburgerScreen("items", "Item Info", "Item Information",Icons.Default.Info)
    object Members : HamburgerScreen("members", "Member Info", "Member Information", Icons.Default.AccountCircle)
    object Debtor : HamburgerScreen("debtor", "Debtor Info", "Debtor Info",Icons.Default.BusinessCenter)
    object Creditor : HamburgerScreen("creditor", "Creditor Info", "Creditor Info",Icons.Default.CreditCard)
    object Config : HamburgerScreen("config", "Configuration", "Configuration",Icons.Default.Settings)
    object Contact : HamburgerScreen("contact", "Contact Us", "Find Us",Icons.Default.Phone)

    object CashSales : HamburgerScreen("cashSales", "Cash Sales", "Cash Sales", Icons.Default.PointOfSale)
    object Invoice : HamburgerScreen("invoice", "Invoice", "Invoice", Icons.Default.ReceiptLong)

    companion object {
        val all = listOf(Dashboard, StockList, StockTake, SalesOrder,  SalesOverview, HourlySales, DailySales, MonthlySales, Rank, Items, Members, Debtor, Creditor, Config, Contact, CashSales, Invoice)
    }
}