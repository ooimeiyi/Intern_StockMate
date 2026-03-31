package com.example.intern_stockmate.ui.stockAdjustment

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.intern_stockmate.model.StockItem
import com.example.intern_stockmate.scanner.QRCodeScanner
import com.example.intern_stockmate.ui.stockLIst.StockItemRow
import com.example.intern_stockmate.viewModel.StockAdjustmentViewModel
import com.example.intern_stockmate.viewModel.StockViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustmentItemsScreen(
    navController: NavHostController,
    stockViewModel: StockViewModel,
    stockAdjustmentViewModel: StockAdjustmentViewModel
) {
    val physicalCounts = stockAdjustmentViewModel.physicalCounts
    val diffCounts = stockAdjustmentViewModel.diffCounts
    val selectedHeader by stockAdjustmentViewModel.selectedHeader.collectAsState()

    val searchQuery by stockViewModel.searchQuery.collectAsState()
    var localQuery by remember { mutableStateOf(searchQuery) }
    val isInvalidSearch by stockViewModel.isInvalidSearch.collectAsState()

    val selectedLocation by stockViewModel.selectedLocation.collectAsState()
    val locations by stockAdjustmentViewModel.locations.collectAsState(initial = emptyList())
    val filteredItems by stockViewModel.filteredItems.collectAsState()
    val allItems by stockViewModel.allItems.collectAsState()

    var description by remember(selectedHeader) { mutableStateOf(selectedHeader?.description ?: "") }
    var stockTakeNo by remember(selectedHeader) { mutableStateOf(selectedHeader?.stockTakeNo ?: "") }
    var date by remember(selectedHeader) { mutableStateOf(selectedHeader?.date ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = androidx.compose.material3.rememberDatePickerState()

    val context = LocalContext.current
    var scanning by remember { mutableStateOf(false) }

    var showStockPicker by remember { mutableStateOf(false) }
    var stockPickerQuery by remember { mutableStateOf("") }
    val manuallySelectedCodes = remember { mutableStateListOf<String>() }
    var pickerSelectedCode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(locations, selectedLocation, selectedHeader) {
        if (selectedHeader == null && locations.isNotEmpty() && selectedLocation.isBlank()) {
            stockAdjustmentViewModel.onLocationSelected(locations.first())
        }
    }

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
        focusedBorderColor = Color.Red,
        unfocusedBorderColor = Color(0xFFE0E0E0),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis
                    if (selectedDate != null) {
                        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        date = formatter.format(Date(selectedDate))
                    }
                    showDatePicker = false
                }) { Text("OK", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AdjustmentBottomBar(
                description = description,
                stockTakeNo = stockTakeNo,
                date = date,
                selectedLocation = selectedLocation,
                context = context,
                navController = navController,
                stockAdjustmentViewModel = stockAdjustmentViewModel
            )
        },
        containerColor = Color(0xFFF8F8F8)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Adjustment Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AdjustmentInputField(
                                label = "Description",
                                value = description,
                                onValueChange = { description = it },
                                placeholder = "Enter description",
                                modifier = Modifier.weight(1.2f)
                            )
                            Box(modifier = Modifier.weight(0.8f)) {
                                AdjustmentInputField(
                                    label = "Date",
                                    value = date,
                                    onValueChange = {},
                                    placeholder = "DD-MM-YYYY",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .padding(top = 15.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { showDatePicker = true }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                            AdjustmentInputField(
                                label = "Stock Take No",
                                value = stockTakeNo,
                                onValueChange = {},
                                placeholder = "Auto-generated",
                                modifier = Modifier.weight(1.2f),
                                readOnly = true
                            )
                            Box(modifier = Modifier.weight(0.8f)) {
                                DropdownLocation(
                                    label = "Location",
                                    selected = selectedLocation,
                                    options = locations,
                                    onSelect = { stockViewModel.onLocationSelected(it) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = localQuery,
                        onValueChange = { localQuery = it },
                        placeholder = {
                            Text("Enter Item Code or...", color = Color.Gray, fontSize = 14.sp)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = textFieldColors,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showStockPicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = "Show stock list",
                                        tint = Color.Red
                                    )
                                }
                                IconButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan",
                                        tint = Color.Red
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                val query = localQuery.trim()
                                pickerSelectedCode = null
                                stockViewModel.onSearchQueryChange(query)
                                localQuery = ""
                            }
                        )
                    )

                    Button(
                        onClick = {
                            val query = localQuery.trim()
                            pickerSelectedCode = null
                            stockViewModel.onSearchQueryChange(query)
                            localQuery = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Search Item", color = Color.White)
                    }
                }
            }

            val isSearching = searchQuery.isNotBlank()
            if (isSearching && isInvalidSearch) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
                        Text("No matching item found. Please try again", color = Color.Red)
                    }
                }
            } else {
                val itemsToShow = if (isSearching) {
                    filteredItems
                } else {
                    val selectedCodes = physicalCounts.keys + manuallySelectedCodes
                    allItems.filter { selectedCodes.contains(it.itemCode) }
                }

                if (!isSearching && itemsToShow.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Search item to add stock adjustment",
                                color = Color.Gray
                            )
                        }
                    }
                }

                items(itemsToShow) { item ->
                    StockItemRowLogic(item, selectedLocation, physicalCounts, diffCounts)
                }
            }
        }
    }

    if (scanning) {
        QRCodeScanner(
            scanning = scanning,
            onResult = {
                localQuery = it
                stockViewModel.onSearchQueryChange(it)
                scanning = false
            },
            onScanFinished = { scanning = false }
        )
    }

    if (showStockPicker) {
        val pickerItems = allItems.filter {
            val query = stockPickerQuery.trim()
            val matchesLocation = selectedLocation.isBlank() ||
                    it.locationList.any { locationInfo -> locationInfo.location == selectedLocation }
            val matchesQuery = query.isBlank() ||
                    it.itemCode.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.desc2.orEmpty().contains(query, ignoreCase = true)
            matchesLocation && matchesQuery
        }

        Dialog(onDismissRequest = { showStockPicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(550.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select Stock Item",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pickerItems) { pickerItem ->
                            FilterItemRow(item = pickerItem) {
                                manuallySelectedCodes.add(pickerItem.itemCode)
                                if (!physicalCounts.containsKey(pickerItem.itemCode)) {
                                    physicalCounts[pickerItem.itemCode] = ""

                                    diffCounts[pickerItem.itemCode] = 0
                                }
                                stockViewModel.onSearchQueryChange("")
                                localQuery = ""
                                showStockPicker = false
                                stockPickerQuery = ""
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterItemRow(item: StockItem, onClick: () -> Unit) {
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
        }
    }
}

@Composable
fun AdjustmentBottomBar(
    description: String,
    stockTakeNo: String,
    date: String,
    selectedLocation: String,
    context: android.content.Context,
    navController: NavHostController,
    stockAdjustmentViewModel: StockAdjustmentViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                if (description.isBlank() || stockTakeNo.isBlank() || date.isBlank() || selectedLocation.isBlank()) {
                    Toast.makeText(context, "Please fill all header fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                stockAdjustmentViewModel.updateSelectedHeaderFields(
                    description,
                    date,
                    stockTakeNo,
                    selectedLocation
                )

                val success = stockAdjustmentViewModel.saveCurrentAsKiv()

                if (success) {
                    Toast.makeText(context, "Saved to KIV locally", Toast.LENGTH_SHORT).show()
                }

                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("selectedTab", 2)

                navController.popBackStack()
            },
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
        ) {
            Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save KIV", color = Color.White, fontSize = 14.sp)
        }

        Button(
            onClick = {
                if (description.isBlank() || stockTakeNo.isBlank() || date.isBlank() || selectedLocation.isBlank()) {
                    Toast.makeText(context, "Please fill all header fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                stockAdjustmentViewModel.updateSelectedHeaderFields(
                    description,
                    date,
                    stockTakeNo,
                    selectedLocation
                )

                stockAdjustmentViewModel.submitCurrentAdjustment { success, message ->
                    Toast.makeText(
                        context,
                        if (success) "Adjustment submitted" else "Submit failed: $message",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (success) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("selectedTab", 2)

                        navController.popBackStack()
                    }
                }
            },
            modifier = Modifier
                .weight(1.6f)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Submit Adjustment", color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
fun StockItemRowLogic(
    stockItem: StockItem,
    selectedLocation: String,
    physicalCounts: MutableMap<String, String>,
    diffCounts: MutableMap<String, Int>
) {
    val key = stockItem.itemCode
    StockItemFilterRow(
        item = stockItem,
        selectedLocation = selectedLocation,
        physicalValue = physicalCounts[key] ?: "",
        diffValue = diffCounts[key] ?: 0,
        onPhysicalChange = { newValue ->
            if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                physicalCounts[key] = newValue
                val onHand = stockItem.locationList.find { it.location == selectedLocation }?.qty ?: 0
                diffCounts[key] = (newValue.toIntOrNull() ?: 0) - onHand
            }
        }
    )
}

@Composable
fun StockItemFilterRow(
    item: StockItem,
    selectedLocation: String,
    physicalValue: String,
    diffValue: Int,
    onPhysicalChange: (String) -> Unit
) {
    val onHandQty = item.locationList.find { it.location == selectedLocation }?.qty ?: 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.itemCode, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(
                        item.description,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        item.desc2 ?: "",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    selectedLocation,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            }
            Spacer(Modifier.height(12.dp))

            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QtyDisplayBox(
                    label = "OnHand Qty",
                    value = onHandQty.toString(),
                    modifier = Modifier.width(100.dp),
                    color = Color.Black
                )

                EditableQtyBox(
                    label = "Physical Qty",
                    value = physicalValue,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*$"))) {
                            onPhysicalChange(input)
                        }
                    },
                    isNumber = true,
                    minWidth = 90.dp,
                    color = Color.Black
                )

                QtyDisplayBox(
                    label = "Diff Qty",
                    value = diffValue.toString(),
                    modifier = Modifier.width(100.dp),
                    color = if (diffValue < 0) Color.Red else Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
fun EditableQtyBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    minWidth: Dp = 60.dp,
    isNumber: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.widthIn(min = minWidth)
    ) {
        Text(label, fontSize = 10.sp, color = Color.Gray)

        Box(
            modifier = Modifier
                .height(35.dp)
                .width(IntrinsicSize.Min)
                .widthIn(min = minWidth)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text
                ),
                modifier = Modifier.wrapContentWidth(),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.Center) {
                        if (value.isEmpty()) {
                            Text("0", color = Color.LightGray, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
fun QtyDisplayBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color,
    minWidth: Dp = 80.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.widthIn(min = minWidth)
    ) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Box(
            modifier = Modifier
                .height(35.dp)
                .widthIn(min = 75.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownLocation(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        cursorColor = Color.Black,
        focusedBorderColor = Color(0xFFE53935),
        unfocusedBorderColor = Color(0xFFE0E0E0),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    Column {
        Text(label, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth().height(45.dp),
                shape = RoundedCornerShape(4.dp),
                textStyle = TextStyle(fontSize = 11.sp),
                colors = textFieldColors
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                options.forEach { option ->
                    val isSelected = option == selected
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 13.sp, color = Color.Black) },
                        onClick = { onSelect(option); expanded = false },
                        modifier = Modifier.background(if (isSelected) Color(0xFFBBDEFB) else Color.White)
                    )
                }
            }
        }
    }
}

@Composable
fun AdjustmentInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    Column(modifier) {
        if (label.isNotEmpty()) {
            Text(label, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            readOnly = readOnly,
            textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
                .height(45.dp)
                .padding(horizontal = 8.dp),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = TextStyle(fontSize = 13.sp, color = Color.LightGray))
                    }
                    innerTextField()
                }
            }
        )
    }
}