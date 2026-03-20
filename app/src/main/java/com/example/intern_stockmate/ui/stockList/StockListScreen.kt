package com.example.intern_stockmate.ui.stockList

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.intern_stockmate.viewModel.StockViewModel
import com.example.intern_stockmate.repository.StockRepository
import com.example.intern_stockmate.model.StockItem
import com.example.intern_stockmate.scanner.QRCodeScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(
    navController: NavHostController,
    settingsViewModel: SettingViewModel = viewModel()
) {
    val apiUrl by settingsViewModel.getApiUrl
    val context = LocalContext.current

    // Scanner State
    var scanning by remember { mutableStateOf(false) }

    // Launcher for camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scanning = true else Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
    }

    // Initialize ViewModel (even if API is blank, so the search state works)
    val stockViewModel: StockViewModel = viewModel(
        key = apiUrl,
        factory = StockViewModelFactory(StockRepository(apiUrl))
    )

    val searchQuery by stockViewModel.searchQuery.collectAsState()
    var localQuery by remember { mutableStateOf(searchQuery) }
    val isInvalidSearch by stockViewModel.isInvalidSearch.collectAsState()

    val stockItems by stockViewModel.filteredItems.collectAsState()
    val filteredItems by stockViewModel.filteredItems.collectAsState()

    LaunchedEffect(apiUrl) {
        if (apiUrl.isNotBlank()) {
            stockViewModel.fetchStockItems()
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        cursorColor = Color.Black,
        focusedBorderColor = Color(0xFFE0E0E0),
        unfocusedBorderColor = Color(0xFFE0E0E0)
    )

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
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Stock List", fontSize = 20.sp, color = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Outlined.Settings, "Settings", tint = Color.White)
                    }
                    IconButton(onClick = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Outlined.ExitToApp, "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Red)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(10.dp))

            // SEARCH ROW
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
                            stockViewModel.onSearchQueryChange(query)
                            // Clear TextField after Enter
                            //localQuery = ""

                            // Delay clearing so debug has time to run
                            CoroutineScope(Dispatchers.Main).launch {
                                kotlinx.coroutines.delay(50) // small delay to let debug run
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
                                stockViewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, "Clear", tint = Color.Black)
                            }
                        }
                    }
                )

                // Scanner Button
                Button(
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // CONTENT AREA
            if (apiUrl.isBlank()) {
                // If API is not set, show the message where the list would be
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Enter API URL in Setting Screen",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                // If API is set, show the list
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),

                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (searchQuery.isNotBlank()) {  // Only show anything if user typed
                        if (isInvalidSearch) {
                            // Show no matching item message
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
                        } else {
                            // Show filtered items
                            filteredItems.forEach { item ->
                                StockItemRow(item = item) {
                                    navController.currentBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("selectedItem", item)
                                    navController.navigate("stockDetail")

                                    localQuery = ""
                                    stockViewModel.onSearchQueryChange("")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // QR Scanner Overlay
    QRCodeScanner(
        scanning = scanning,
        onResult = { scannedValue ->
            // Fill the search field with the scanned value
            stockViewModel.onSearchQueryChange(scannedValue)
            // Stop scanning
            scanning = false
        },
        onScanFinished = { scanning = false }
    )
}

@Composable
fun StockItemRow(item: StockItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.itemCode,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = item.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (!item.desc2.isNullOrBlank()) item.desc2 else "",
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
                Text(
                    text = "Total Stock",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}