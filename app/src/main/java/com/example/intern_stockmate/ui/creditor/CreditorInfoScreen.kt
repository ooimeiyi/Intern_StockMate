package com.example.intern_stockmate.ui.creditor

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
import com.example.intern_stockmate.model.CreditorSummary
import com.example.intern_stockmate.model.OutstandingCreditorItem
import com.example.intern_stockmate.viewModel.CreditInfoUiState
import com.example.intern_stockmate.viewModel.CreditorInfoViewModel
import androidx.compose.runtime.collectAsState

@Composable
fun CreditorInfoScreenContainer(
    viewModel: CreditorInfoViewModel = viewModel()
) {
    val state by viewModel.creditorState.collectAsState()

    val summary = (state as? CreditInfoUiState.Success)?.data?.summary ?: CreditorSummary()
    val outstandingList = (state as? CreditInfoUiState.Success)?.data?.outstandingList.orEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        CreditorInfoScreen(
            summary = summary,
            outstandingCreditorList = outstandingList,
            onRefresh = viewModel::fetchCreditorSummary
        )

        when (state) {
            is CreditInfoUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFD32F2F))
                }
            }
            is CreditInfoUiState.Error -> {
                val message = (state as CreditInfoUiState.Error).message
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
            is CreditInfoUiState.Success -> Unit
        }
    }
}

@Composable
fun CreditorInfoScreen(
    summary: CreditorSummary,
    outstandingCreditorList: List<OutstandingCreditorItem>,
    onRefresh: () -> Unit
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
                        text = "CREDITOR SUMMARY",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                        color = Color.Black
                    )
                }

                HorizontalDivider(color = Color(0xFFF0F0F0))

                CreditorSummaryRow("Active Creditor", summary.activeCreditor.toString())
                CreditorSummaryRow("Non Active Creditor", summary.nonActiveCreditor.toString())
                CreditorSummaryRow("Total Count of Outstanding", summary.totalCountOutstanding.toString())
                CreditorSummaryRow(
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

            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFD32F2F)
                )
            ) {
                Text("Refresh", fontWeight = FontWeight.Bold)
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
            OutstandingCreditorListSection(items = outstandingCreditorList)
        }
    }
}

@Composable
fun CreditorSummaryRow(label: String, value: String) {
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
fun OutstandingCreditorListSection(items: List<OutstandingCreditorItem>) {
    if (items.isEmpty()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "No outstanding creditor records found.",
                modifier = Modifier.padding(16.dp),
                color = Color.Gray
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEach { item ->
            OutstandingCreditorItemCard(item)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun OutstandingCreditorItemCard(item: OutstandingCreditorItem) {
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
                Text(item.creditorCode, color = Color.Black, fontSize = 12.sp)

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