package com.example.intern_stockmate.ui.salesOrder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.intern_stockmate.viewModel.SalesOrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesOrderScreen(
    navController: NavHostController,
    salesOrderViewModel: SalesOrderViewModel
) {
    val savedHeaders by salesOrderViewModel.savedHeaders.collectAsState()
    val selectedLocation by salesOrderViewModel.selectedLocation.collectAsState()

    LaunchedEffect(Unit) {
        salesOrderViewModel.loadSavedSalesOrders()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    salesOrderViewModel.prepareNewSalesOrderHeader(selectedLocation) {
                        navController.navigate("salesOrderDetails")
                    }
                },
                containerColor = Color.Red,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New sales order")
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
                    Text("No recent sales orders", color = Color.LightGray)
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
                                    salesOrderViewModel.onHeaderSelected(header)
                                    navController.navigate("salesOrderDetails")
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
                                        text = header.soNo,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black
                                    )

                                    Text(
                                        text = header.status,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (header.status == "Submitted") Color(0xFF2E7D32) else Color(0xFFE65100)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = header.date, fontSize = 13.sp, color = Color.DarkGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = header.debtor, fontSize = 15.sp, color = Color.Black)
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