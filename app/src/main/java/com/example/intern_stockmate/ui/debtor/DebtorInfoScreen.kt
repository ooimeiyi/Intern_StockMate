package com.example.intern_stockmate.ui.debtor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.intern_stockmate.model.DebtorSummary
import com.example.intern_stockmate.model.OutstandingDebtorItem
import com.example.intern_stockmate.viewModel.DebtorInfoUiState
import com.example.intern_stockmate.viewModel.DebtorInfoViewModel

@Composable
fun DebtorInfoScreenContainer(
    viewModel: DebtorInfoViewModel = viewModel()
) {
    val state by viewModel.debtorState.collectAsState()

    val summary = (state as? DebtorInfoUiState.Success)?.data?.summary ?: DebtorSummary()
    val outstandingList = (state as? DebtorInfoUiState.Success)?.data?.outstandingList.orEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        DebtorInfoScreen(
            summary = summary,
            outstandingDebtorList = outstandingList
        )

        when (state) {
            is DebtorInfoUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFD32F2F))
                }
            }
            is DebtorInfoUiState.Error -> {
                val message = (state as DebtorInfoUiState.Error).message
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFCDD2))
                        .padding(12.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Text(
                        text = message,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            is DebtorInfoUiState.Success -> Unit
        }
    }
}

@Composable
fun DebtorInfoScreen(
    summary: DebtorSummary,
    outstandingDebtorList: List<OutstandingDebtorItem>
) {
    var isExpanded by remember { mutableStateOf(true) }
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
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "DEBTOR SUMMARY",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                        color = Color.Black
                    )
                }

                HorizontalDivider(color = Color(0xFFF0F0F0))

                DebtorSummaryRow("Active Debtor", summary.activeDebtor.toString())
                DebtorSummaryRow("Non Active Debtor", summary.nonActiveDebtor.toString())
                DebtorSummaryRow("Total Count of Outstanding", summary.totalCountOutstanding.toString())
                DebtorSummaryRow(
                    "Total Sum of Outstanding",
                    "RM ${String.format("%,.2f", summary.totalSumOutstanding)}"
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF4A90E2)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF3399FF),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isExpanded) "Hide Details" else "Show Details",
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isExpanded) "^" else "v", fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = "OUTSTANDING LIST",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 8.dp)
        )

        if (isExpanded) {
            OutstandingDebtorListSection(items = outstandingDebtorList)
        }
    }
}

@Composable
fun DebtorSummaryRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = Color.Black,
                fontSize = 13.sp
            )
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
        }
        HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
    }
}

@Composable
fun OutstandingDebtorListSection(items: List<OutstandingDebtorItem>) {
    if (items.isEmpty()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "No outstanding debtor records found.",
                modifier = Modifier.padding(16.dp),
                color = Color.Gray
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEach { item ->
            OutstandingDebtorItemCard(item)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun OutstandingDebtorItemCard(item: OutstandingDebtorItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFEBF2FF), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", color = Color(0xFF0052FF))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.companyName, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(item.debtorCode, color = Color.Black, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${item.billCount} Bills",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = Color.Black
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "RM ${String.format("%,.2f", item.outstandingAmount)}",
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text("Outstanding", color = Color.Black, fontSize = 11.sp)
            }
        }
    }
}