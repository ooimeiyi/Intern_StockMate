package com.example.intern_stockmate.ui.itemInfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.intern_stockmate.model.ItemInfo
import com.example.intern_stockmate.viewModel.ItemInfoUiState
import com.example.intern_stockmate.viewModel.ItemInfoViewModel

@Composable
fun ItemInfoScreenContainer(viewModel: ItemInfoViewModel = viewModel()) {
    val state by viewModel.itemState.collectAsState()
    val summary = (state as? ItemInfoUiState.Success)?.data ?: ItemInfo()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        ItemInfoScreen(summary = summary, onRefresh = viewModel::fetchItemSummary)

        when (state) {
            is ItemInfoUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFD32F2F))
                }
            }
            is ItemInfoUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFCDD2))
                        .padding(12.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Text(
                        text = (state as ItemInfoUiState.Error).message,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            is ItemInfoUiState.Success -> Unit
        }
    }
}

@Composable
private fun ItemInfoScreen(summary: ItemInfo, onRefresh: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                ItemSummaryRow("All Item", summary.allItem.toString())
                ItemSummaryRow("Active", summary.active.toString())
                ItemSummaryRow("Non Active", summary.nonActive.toString())
                ItemSummaryRow("Stock Control", summary.stockControl.toString())
                ItemSummaryRow("Non Stock Control", summary.nonStockControl.toString())
                ItemSummaryRow("Item Group Count", summary.itemGroupCount.toString())
                ItemSummaryRow("Item Type Count", summary.itemTypeCount.toString())
                ItemSummaryRow("Negative Qty", summary.negativeQty.toString(), isNegativeValue = true)
            }
        }
    }
}

@Composable
private fun ItemSummaryRow(label: String, value: String, isNegativeValue: Boolean = false) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = Color.Black,
                fontSize = 14.sp
            )
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                color = if (isNegativeValue && value != "0") Color.Red else Color.Black
            )
        }
        HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
    }
}