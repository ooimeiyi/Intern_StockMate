package com.example.intern_stockmate.ui.stockAdjustment

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.intern_stockmate.viewModel.StockAdjustmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentScreen(
    navController: NavHostController,
    stockViewModel: StockAdjustmentViewModel
) {
    val savedHeaders by stockViewModel.savedHeaders.collectAsState()

    LaunchedEffect(Unit) {
        stockViewModel.loadSavedAdjustmentsFromLocal()
        stockViewModel.loadSavedAdjustmentsFromFirebase()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("SO", color = Color.White, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Text("Stock Adjustment", fontSize = 20.sp, color = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Outlined.Settings, null, tint = Color.White)
                    }
                    IconButton(onClick = { navController.navigate("login") }) {
                        Icon(Icons.Outlined.ExitToApp, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Red)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    stockViewModel.prepareNewAdjustmentHeader(stockViewModel.selectedLocation.value) {
                        navController.navigate("adjustmentDetails")
                    }
                },
                containerColor = Color.Red,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New stock take")
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            if (savedHeaders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recent adjustments", color = Color.LightGray)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Spacer(modifier = Modifier.height(10.dp))

                    savedHeaders.forEach { header ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    stockViewModel.onHeaderSelected(header)
                                    navController.navigate("adjustmentDetails")
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {

                            Column(modifier = Modifier.padding(14.dp)) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = header.stockTakeNo,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black
                                    )

                                    Text(
                                        text = header.status,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (header.status == "Submitted")
                                            Color(0xFF2E7D32)
                                        else
                                            Color(0xFFE65100)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = header.date,
                                    fontSize = 13.sp,
                                    color = Color.DarkGray
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = header.description,
                                    fontSize = 15.sp,
                                    color = Color.Black
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = header.location,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

