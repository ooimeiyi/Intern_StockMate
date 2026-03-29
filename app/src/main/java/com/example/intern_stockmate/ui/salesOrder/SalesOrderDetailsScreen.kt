package com.example.intern_stockmate.ui.salesOrder

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.navigation.NavHostController
import com.example.intern_stockmate.model.StockItem
import com.example.intern_stockmate.scanner.QRCodeScanner
import com.example.intern_stockmate.viewModel.SalesOrderViewModel
import com.example.intern_stockmate.viewModel.StockViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesOrderDetailsScreen(
    navController: NavHostController,
    stockViewModel: StockViewModel,
    salesOrderViewModel: SalesOrderViewModel
) {
    val selectedHeader by salesOrderViewModel.selectedHeader.collectAsState()

    val searchQuery by stockViewModel.searchQuery.collectAsState()
    var localQuery by remember { mutableStateOf(searchQuery) }
    val isInvalidSearch by stockViewModel.isInvalidSearch.collectAsState()

    val selectedLocation by salesOrderViewModel.selectedLocation.collectAsState()
    val locations by salesOrderViewModel.locations.collectAsState(initial = emptyList())
    val debtors by salesOrderViewModel.debtors.collectAsState(initial = emptyList())
    val filteredItems by stockViewModel.filteredItems.collectAsState()

    var debtor by remember(selectedHeader) { mutableStateOf(selectedHeader?.debtor ?: "") }
    var soNo by remember(selectedHeader) { mutableStateOf(selectedHeader?.soNo ?: "") }
    var date by remember(selectedHeader) { mutableStateOf(selectedHeader?.date ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = androidx.compose.material3.rememberDatePickerState()

    val context = LocalContext.current
    var scanning by remember { mutableStateOf(false) }

    LaunchedEffect(locations, selectedLocation) {
        if (locations.isNotEmpty() && selectedLocation.isBlank()) {
            salesOrderViewModel.onLocationSelected(locations.first())
        }
    }

    LaunchedEffect(debtors, selectedHeader) {
        if (debtor.isBlank() && debtors.isNotEmpty() && selectedHeader == null) {
            debtor = debtors.first()
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        salesOrderViewModel.updateHeaderFields(
                            debtor = debtor,
                            date = date,
                            soNo = soNo,
                            location = selectedLocation
                        )
                        salesOrderViewModel.saveCurrentAsKiv { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (success) navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text("Save KIV", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        salesOrderViewModel.updateHeaderFields(
                            debtor = debtor,
                            date = date,
                            soNo = soNo,
                            location = selectedLocation
                        )
                        salesOrderViewModel.submitCurrentSalesOrder { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (success) navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
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
                            "Sales Order Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1.2f)) {
                                SalesOrderDropdown(
                                    label = "Debtor",
                                    selected = debtor,
                                    options = debtors,
                                    onSelect = { debtor = it }
                                )
                            }
                            Box(modifier = Modifier.weight(0.8f)) {
                                SalesOrderInputField(
                                    label = "Date",
                                    value = date,
                                    onValueChange = {},
                                    placeholder = "DD-MM-YYYY",
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true
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
                            SalesOrderInputField(
                                label = "SO",
                                value = soNo,
                                onValueChange = {},
                                placeholder = "Auto-generated",
                                modifier = Modifier.weight(1.2f),
                                readOnly = true
                            )
                            Box(modifier = Modifier.weight(0.8f)) {
                                SalesOrderDropdown(
                                    label = "Location",
                                    selected = selectedLocation,
                                    options = locations,
                                    onSelect = { salesOrderViewModel.onLocationSelected(it) }
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
                            Text("Enter Item Code or Barcode...", color = Color.Gray, fontSize = 14.sp)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = textFieldColors,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            IconButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan",
                                    tint = Color.Red
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                val query = localQuery.trim()
                                stockViewModel.onSearchQueryChange(query)
                                localQuery = ""
                            }
                        )
                    )

                    Button(
                        onClick = {
                            val query = localQuery.trim()
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
                    val selectedItemCodes = salesOrderViewModel.selectedItems.keys
                    stockViewModel.allItems.value.filter { selectedItemCodes.contains(it.itemCode) }
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
                                text = "Search item to add sales order",
                                color = Color.Gray
                            )
                        }
                    }
                }

                items(itemsToShow) { item ->
                    SalesOrderItemRow(
                        item = item,
                        selectedLocation = selectedLocation,
                        salesOrderViewModel = salesOrderViewModel
                    )
                }
            }
        }
    }

    if (scanning) {
        QRCodeScanner(
            scanning = scanning,
            onResult = { stockViewModel.onSearchQueryChange(it); scanning = false },
            onScanFinished = { scanning = false }
        )
    }
}

@Composable
private fun SalesOrderItemRow(
    item: StockItem,
    selectedLocation: String,
    salesOrderViewModel: SalesOrderViewModel
) {
    var selectedUom by remember { mutableStateOf(item.uomList.firstOrNull()?.uom ?: item.uom) }
    var qtyInput by remember { mutableStateOf("") }
    var unitPriceInput by remember { mutableStateOf("%.2f".format(item.uomList.firstOrNull()?.price1 ?: item.price)) }

    LaunchedEffect(selectedLocation, item.itemCode) {
        val existing = salesOrderViewModel.selectedItems[item.itemCode]
        if (existing != null) {
            selectedUom = existing.uom
            qtyInput = existing.qty
            unitPriceInput = "%.2f".format(existing.unitPrice)
        } else {
            selectedUom = item.uomList.firstOrNull()?.uom ?: item.uom
            qtyInput = ""
            unitPriceInput = "%.2f".format(item.uomList.firstOrNull()?.price1 ?: item.price)
        }
    }

    val subtotalValue = (qtyInput.toDoubleOrNull() ?: 0.0) * (unitPriceInput.toDoubleOrNull() ?: 0.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.itemCode, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(item.description, fontSize = 14.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.desc2 ?: "", fontSize = 14.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(selectedLocation, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            }
            Spacer(Modifier.height(12.dp))

            val pricingScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(pricingScrollState)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SalesOrderDropdownUom(
                    label = "UOM",
                    selected = selectedUom,
                    options = item.uomList.map { it.uom },
                    onSelect = { newUom ->
                        selectedUom = newUom
                        val uomInfo = item.uomList.find { it.uom == newUom }
                        val newPrice = uomInfo?.price1 ?: item.price
                        unitPriceInput = "%.2f".format(newPrice)
                        salesOrderViewModel.updateSelectedItem(
                            itemCode = item.itemCode,
                            qty = qtyInput,
                            uom = selectedUom,
                            unitPrice = unitPriceInput.toDoubleOrNull() ?: 0.0
                        )
                    }
                )
                SalesOrderEditableBox(
                    label = "Quantity",
                    value = qtyInput,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*\\.?\\d*$"))) {
                            qtyInput = input
                            salesOrderViewModel.updateSelectedItem(
                                itemCode = item.itemCode,
                                qty = qtyInput,
                                uom = selectedUom,
                                unitPrice = unitPriceInput.toDoubleOrNull() ?: 0.0
                            )
                        }
                    },
                    color = Color.Black,
                    isNumber = true
                )
                SalesOrderEditableBox(
                    label = "Unit Price",
                    value = unitPriceInput,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*\\.?\\d*$"))) {
                            unitPriceInput = input
                            salesOrderViewModel.updateSelectedItem(
                                itemCode = item.itemCode,
                                qty = qtyInput,
                                uom = selectedUom,
                                unitPrice = unitPriceInput.toDoubleOrNull() ?: 0.0
                            )
                        }
                    },
                    color = Color.Black,
                    isNumber = true
                )
                SalesOrderDisplayBox(
                    label = "Subtotal",
                    value = "%.2f".format(subtotalValue),
                    color = if (subtotalValue >= 0) Color(0xFF2E7D32) else Color.Red
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesOrderDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
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
        Text(
            label,
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(45.dp),
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
                    DropdownMenuItem(
                        text = {
                            Text(text = option, fontSize = 13.sp, color = Color.Black)
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesOrderDropdownUom(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    width: Dp = 100.dp,
    height: Dp = 45.dp
) {
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
        Text(label, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().width(width).height(height),
                shape = RoundedCornerShape(4.dp),
                textStyle = TextStyle(fontSize = 11.sp),
                colors = textFieldColors
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(width).background(Color.White)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option, fontSize = 13.sp, color = Color.Black) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SalesOrderEditableBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    minWidth: Dp = 60.dp,
    isNumber: Boolean = false
) {
    Column(horizontalAlignment = Alignment.Start, modifier = modifier.widthIn(min = minWidth)) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Box(
            modifier = Modifier
                .height(35.dp)
                .width(IntrinsicSize.Min)
                .widthIn(min = minWidth)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text
                ),
                modifier = Modifier.wrapContentWidth()
            )
        }
    }
}

@Composable
private fun SalesOrderDisplayBox(
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

@Composable
private fun SalesOrderInputField(
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