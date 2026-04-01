package com.example.intern_stockmate.ui.stockLIst

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.intern_stockmate.model.StockItem
import com.example.intern_stockmate.scanner.QRCodeScanner
import com.example.intern_stockmate.viewModel.StockUiState
import com.example.intern_stockmate.viewModel.StockViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun StockListScreenContainer(
    navController: NavHostController,
) {
    val viewModel: StockViewModel = viewModel()
    val state by viewModel.stockState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isInvalidSearch by viewModel.isInvalidSearch.collectAsState()
    val filteredItems by viewModel.filteredItems.collectAsState()

    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.getStockList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(
                indication = null, // no ripple
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus() // hides keyboard
            }
    ) {
        val itemsList = if (state is StockUiState.Success) filteredItems else emptyList()

        StockListScreen(
            navController = navController,
            filteredItems = itemsList,
            searchQuery = searchQuery,
            isInvalidSearch = isInvalidSearch,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) }
        )

        if (state is StockUiState.Error) {
            val message = (state as StockUiState.Error).message
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

        if (state is StockUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Loading...",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(
    navController: NavHostController,
    filteredItems: List<StockItem>,
    searchQuery: String,
    isInvalidSearch: Boolean,
    onSearchQueryChange: (String) -> Unit
) {
    val context = LocalContext.current
    var localQuery by remember { mutableStateOf(searchQuery) }
    var scanning by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        scanning = granted
        if (!granted) Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        cursorColor = Color.Black,
        focusedBorderColor = Color(0xFFE0E0E0),
        unfocusedBorderColor = Color(0xFFE0E0E0)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = localQuery,
                onValueChange = { localQuery = it },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val query = localQuery.trim()
                        onSearchQueryChange(query)
                        CoroutineScope(Dispatchers.Main).launch {
                            kotlinx.coroutines.delay(50)
                            localQuery = ""
                        }
                    }
                ),
                placeholder = { Text("Item Code or Desc.", color = Color.LightGray) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = textFieldColors,
                shape = RoundedCornerShape(8.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.LightGray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            localQuery = ""
                            onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, "Clear", tint = Color.Black)
                        }
                    }
                }
            )
            Button(
                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF3636)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isInvalidSearch) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching item found. Please Try Again",
                    color = Color.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-10).dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (searchQuery.isBlank()) {
                    item {  }
                } else if (isInvalidSearch) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matching item found. Please Try Again",
                                color = Color.Red,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    items(filteredItems) { item ->
                        StockItemRow(item) {
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("selectedStockItem", item)
                            navController.navigate("stockDetail")

                            localQuery = ""
                            onSearchQueryChange("")
                        }
                    }
                }
            }
        }
    }

    QRCodeScanner(
        scanning = scanning,
        onResult = { scannedValue ->
            onSearchQueryChange(scannedValue)
            scanning = false
        },
        onScanFinished = { scanning = false }
    )
}

@Composable
fun StockItemRow(item: StockItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.itemCode, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(
                    text = item.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.desc2.orEmpty(),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.balQty}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (item.balQty > 0) Color(0xFF2E7D32) else Color.Red
                )
                Text("Total Stock", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}