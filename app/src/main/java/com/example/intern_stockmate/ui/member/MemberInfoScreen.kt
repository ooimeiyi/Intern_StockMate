package com.example.intern_stockmate.ui.member

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
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
import com.example.intern_stockmate.model.MemberInfo
import com.example.intern_stockmate.viewModel.MemberInfoUiState
import com.example.intern_stockmate.viewModel.MemberInfoViewModel

@Composable
fun MemberInfoScreenContainer(
    viewModel: MemberInfoViewModel = viewModel()
) {
    val state by viewModel.memberState.collectAsState()

    val summary = (state as? MemberInfoUiState.Success)?.data ?: MemberInfo()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        MemberInfoScreen(
            summary = summary
        )

        when (state) {
            is MemberInfoUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFD32F2F))
                }
            }
            is MemberInfoUiState.Error -> {
                val message = (state as MemberInfoUiState.Error).message
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
            is MemberInfoUiState.Success -> Unit
        }
    }
}

@Composable
fun MemberInfoScreen(
    summary: MemberInfo
) {
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
                MemberSummaryRow("All Member", summary.totalMembers.toString())
                MemberSummaryRow("Active", summary.activeMembers.toString())
                MemberSummaryRow("Non Active", summary.inactiveMembers.toString())
                MemberSummaryRow("Gender - Male", summary.maleCount.toString())
                MemberSummaryRow("Gender - Female", summary.femaleCount.toString())
                MemberSummaryRow("Member's This Month Birthday", summary.birthdayThisMonth.toString())
            }
        }
    }
}

@Composable
fun MemberSummaryRow(label: String, value: String) {
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
                color = Color.Black
            )
        }
        HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
    }
}