package com.example.intern_stockmate.ui.salesRank

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.intern_stockmate.model.TopSalesItem
import com.example.intern_stockmate.viewModel.SalesRankUiState
import com.example.intern_stockmate.viewModel.SalesRankViewModel

@Composable
fun SalesRankScreenContainer(viewModel: SalesRankViewModel = viewModel()) {
    val state by viewModel.salesRankState.collectAsState()
    val selectedTabIndex = viewModel.selectedTabIndex.intValue

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7F9))) {
        Column(modifier = Modifier.fillMaxSize()) {
            SalesRankTabHeader(selectedTabIndex = selectedTabIndex, onTabSelected = viewModel::selectTab)
            SalesRankScreen(state = state, selectedTabIndex = selectedTabIndex)
        }
        if (state is SalesRankUiState.Loading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Color.Red) }
        }
    }
}

@Composable
private fun SalesRankTabHeader(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("POS", "Invoice", "Cash Sales")

    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFEEEEEE)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTabIndex == index
            Box(
                modifier = Modifier.weight(1f).height(40.dp)
                    .then(
                        if (isSelected) Modifier.background(Color.White, RoundedCornerShape(12.dp)).padding(2.dp)
                        else Modifier
                    )
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) Color.Red else Color.Black,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SalesRankScreen(state: SalesRankUiState, selectedTabIndex: Int) {
    when (state) {
        is SalesRankUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = Color.Red, fontWeight = FontWeight.Bold)
            }
        }

        is SalesRankUiState.Success -> {
            val list = when (selectedTabIndex) {
                0 -> state.posItems
                1 -> state.invoiceItems
                else -> state.cashSalesItems
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TOP ITEMS", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                        if (state.lastUpdate.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Last update: ${state.lastUpdate}", fontSize = 12.sp, color = Color(0xFF5A6B82))
                        }
                    }
                }

                HeaderRow()

                if (list.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No sales data", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(list) { index, item ->
                            SalesItemRow(index = index + 1, item = item)
                        }
                    }
                }
            }
        }
        SalesRankUiState.Loading -> Unit
    }
}

@Composable
private fun HeaderRow() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Text("#", modifier = Modifier.width(30.dp), color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("ITEM / DESC", modifier = Modifier.weight(1.4f), color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("QTY", modifier = Modifier.width(60.dp), color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
        Text("SALES", modifier = Modifier.width(80.dp), color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
    }
}

@Composable
private fun SalesItemRow(index: Int, item: TopSalesItem) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.LightGray,
            modifier = Modifier.width(30.dp)
        )
        Column(modifier = Modifier.weight(1.4f)) {
            Text(text = item.description, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(text = item.code, fontSize = 12.sp, color = Color.Gray)
        }
        Text(
            text = String.format("%.2f", item.qty),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF374151),
            textAlign = TextAlign.End,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = String.format("%,.2f", item.sales),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Red,
            textAlign = TextAlign.End,
            modifier = Modifier.width(80.dp)
        )
    }
}