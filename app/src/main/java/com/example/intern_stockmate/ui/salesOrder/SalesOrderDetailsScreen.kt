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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.intern_stockmate.model.StockItem
import com.example.intern_stockmate.scanner.QRCodeScanner
import com.example.intern_stockmate.viewModel.SalesOrderViewModel
import com.example.intern_stockmate.viewModel.StockViewModel
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.DisposableEffect
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

    var localQuery by remember { mutableStateOf("") }
    var searchError by remember { mutableStateOf<String?>(null) }

    val selectedLocation by salesOrderViewModel.selectedLocation.collectAsState()
    val locations by salesOrderViewModel.locations.collectAsState(initial = emptyList())
    val debtors by salesOrderViewModel.debtors.collectAsState(initial = emptyList())
    val allItems by stockViewModel.allItems.collectAsState()

    val selectedHeaderKey = selectedHeader?.soNo
    var debtor by remember(selectedHeaderKey) { mutableStateOf(selectedHeader?.debtor ?: "") }
    var soNo by remember(selectedHeaderKey) { mutableStateOf(selectedHeader?.soNo ?: "") }
    var date by remember(selectedHeaderKey) { mutableStateOf(selectedHeader?.date ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = androidx.compose.material3.rememberDatePickerState()

    val context = LocalContext.current
    var scanning by remember { mutableStateOf(false) }

    var showStockPicker by remember { mutableStateOf(false) }
    var stockPickerQuery by remember { mutableStateOf("") }
    val manuallySelectedCodes = remember { mutableStateListOf<String>() }
    val sessionTrackedCodes = remember { mutableStateListOf<String>() }
    var pickerSelectedCode by remember { mutableStateOf<String?>(null) }
    var previousLocation by remember { mutableStateOf(selectedLocation) }
    val focusManager = LocalFocusManager.current

    fun addItemFromQuery(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return

        val locationFilteredItems = allItems.filter { stockItem ->
            selectedLocation.isBlank() ||
                    stockItem.locationList.any { locationInfo -> locationInfo.location == selectedLocation }
        }
        val matchedItem = locationFilteredItems.firstOrNull {
            it.itemCode.equals(trimmedQuery, ignoreCase = true)
        }

        if (matchedItem == null) {
            searchError = "No exact item code match found."
            return
        }

        val defaultUom = matchedItem.uomList.firstOrNull()?.uom ?: matchedItem.uom
        val defaultPrice = matchedItem.uomList.firstOrNull()?.price1 ?: matchedItem.price

        if (!sessionTrackedCodes.contains(matchedItem.itemCode)) {
            sessionTrackedCodes.add(matchedItem.itemCode)
        }
        salesOrderViewModel.addOrIncrementItem(
            itemCode = matchedItem.itemCode,
            defaultUom = defaultUom,
            defaultUnitPrice = defaultPrice
        )
        stockViewModel.onSearchQueryChange("")
        localQuery = ""
        searchError = null
        pickerSelectedCode = matchedItem.itemCode
    }

    LaunchedEffect(locations, selectedLocation) {
        if (selectedHeader == null && locations.isNotEmpty() && selectedLocation.isBlank()) {
            salesOrderViewModel.onLocationSelected(locations.first())
        }
    }

    LaunchedEffect(debtors, selectedHeader) {
        if (debtor.isBlank() && debtors.isNotEmpty() && selectedHeader == null) {
            debtor = debtors.first()
        }
    }

    LaunchedEffect(selectedLocation) {
        if (previousLocation.isNotBlank() && previousLocation != selectedLocation) {
            val matchingCodes = allItems
                .filter { stockItem ->
                    stockItem.locationList.any { locationInfo -> locationInfo.location == selectedLocation }
                }
                .map { it.itemCode }
                .toSet()
            manuallySelectedCodes.removeAll { it !in matchingCodes }
            sessionTrackedCodes.removeAll { it !in matchingCodes }
            pickerSelectedCode = null
        }
        previousLocation = selectedLocation
    }

    DisposableEffect(Unit) {
        onDispose {
            stockViewModel.clearLocationSelection()
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
                }) { Text("OK", color = Color(0xFFEF3636)) }
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF3636))
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
                .padding(padding)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus() // hides keyboard
                },
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
                        onValueChange = {
                            localQuery = it
                            searchError = null
                        },
                        placeholder = {
                            Text("Enter Item Code...", color = Color.Gray, fontSize = 14.sp)
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
                                        contentDescription = "Select item",
                                        tint = Color(0xFFEF3636)
                                    )
                                }
                                IconButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan",
                                        tint = Color(0xFFEF3636)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                addItemFromQuery(localQuery)
                            }
                        )
                    )

                    if (searchError != null) {
                        Text(
                            text = searchError.orEmpty(),
                            color = Color.Red,
                            fontSize = 13.sp
                        )
                    }
                }
            }


            val pinnedItems = allItems.filter { stockItem ->
                val qtyText = salesOrderViewModel.selectedItems[stockItem.itemCode]?.qty.orEmpty()
                val qty = qtyText.toDoubleOrNull() ?: 0.0
                val hasSelectedQty = qty > 0.0
                val isSessionTracked = sessionTrackedCodes.contains(stockItem.itemCode) && hasSelectedQty
                hasSelectedQty || isSessionTracked
            }
            val itemsToShow = pinnedItems

            if (itemsToShow.isEmpty()) {
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
                    salesOrderViewModel = salesOrderViewModel,
                    onTrackItemInSession = { itemCode ->
                        if (!sessionTrackedCodes.contains(itemCode)) {
                            sessionTrackedCodes.add(itemCode)
                        }
                    },
                    onDeleteItem = { itemCode ->
                        salesOrderViewModel.removeSelectedItem(itemCode)
                        sessionTrackedCodes.remove(itemCode)
                        manuallySelectedCodes.remove(itemCode)
                    }
                )
            }
            item {
                val grandSubtotal = itemsToShow.sumOf { stockItem ->
                    val selected = salesOrderViewModel.selectedItems[stockItem.itemCode]
                    val qty = selected?.qty.orEmpty().toDoubleOrNull() ?: 0.0
                    qty * (selected?.unitPrice ?: 0.0)
                }
                Text(
                    text = "Subtotal: %.2f".format(grandSubtotal),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .wrapContentWidth(Alignment.End)
                )
            }
        }
    }

    if (scanning) {
        QRCodeScanner(
            scanning = scanning,
            onResult = { code ->
                addItemFromQuery(code)
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
                        text = "Select Sales Item",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    OutlinedTextField(
                        value = stockPickerQuery,
                        onValueChange = { stockPickerQuery = it },
                        placeholder = { Text("Search item code...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search item"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pickerItems) { pickerItem ->
                            SalesOrderFilterItemRow(
                                item = pickerItem,
                                isSelected = pickerSelectedCode == pickerItem.itemCode
                            ) {
                                val defaultUom = pickerItem.uomList.firstOrNull()?.uom ?: pickerItem.uom
                                val defaultPrice = pickerItem.uomList.firstOrNull()?.price1 ?: pickerItem.price
                                pickerSelectedCode = pickerItem.itemCode
                                if (!manuallySelectedCodes.contains(pickerItem.itemCode)) {
                                    manuallySelectedCodes.add(pickerItem.itemCode)
                                }
                                if (!sessionTrackedCodes.contains(pickerItem.itemCode)) {
                                    sessionTrackedCodes.add(pickerItem.itemCode)
                                }
                                salesOrderViewModel.addOrIncrementItem(
                                    itemCode = pickerItem.itemCode,
                                    defaultUom = defaultUom,
                                    defaultUnitPrice = defaultPrice
                                )
                                stockViewModel.onSearchQueryChange("")
                                localQuery = ""
                                stockPickerQuery = ""
                                showStockPicker = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesOrderFilterItemRow(
    item: StockItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, if (isSelected) Color.Red else Color.LightGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.itemCode,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = item.description,
                    color = Color.DarkGray,
                    fontSize = 13.sp
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
private fun SalesOrderItemRow(
    item: StockItem,
    selectedLocation: String,
    salesOrderViewModel: SalesOrderViewModel,
    onTrackItemInSession: (String) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    var selectedUom by remember { mutableStateOf(item.uomList.firstOrNull()?.uom ?: item.uom) }
    var qtyInput by remember { mutableStateOf("") }
    var unitPriceInput by remember { mutableStateOf("%.2f".format(item.uomList.firstOrNull()?.price1 ?: item.price)) }
    val selectedItemState = salesOrderViewModel.selectedItems[item.itemCode]

    LaunchedEffect(
        selectedLocation,
        item.itemCode,
        selectedItemState?.qty,
        selectedItemState?.uom,
        selectedItemState?.unitPrice
    ) {
        val existing = selectedItemState
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        selectedLocation,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )

                    FloatingActionButton(
                        onClick = { onDeleteItem(item.itemCode) },
                        containerColor = Color.White,
                        contentColor = Color(0xFFD32F2F),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete item"
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            val pricingScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(pricingScrollState)
                    .padding(top = 8.dp, bottom = 16.dp),
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
                        onTrackItemInSession(item.itemCode)
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
                            onTrackItemInSession(item.itemCode)
                            salesOrderViewModel.updateSelectedItem(
                                itemCode = item.itemCode,
                                qty = qtyInput,
                                uom = selectedUom,
                                unitPrice = unitPriceInput.toDoubleOrNull() ?: 0.0
                            )
                        }
                    },
                    color = Color.Black,
                    isNumber = true,
                    numberSuffix = selectedUom.ifBlank { null }
                )
                SalesOrderEditableBox(
                    label = "Unit Price",
                    value = unitPriceInput,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*\\.?\\d*$"))) {
                            unitPriceInput = input
                            onTrackItemInSession(item.itemCode)
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
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total : ",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "RM ${"%.2f".format(subtotalValue)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
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
    minWidth: Dp = 90.dp,
    isNumber: Boolean = false,
    numberSuffix: String? = null
) {
    var showNumberPad by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.Start, modifier = modifier.widthIn(min = minWidth)) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Box(
            modifier = Modifier
                .height(35.dp)
                .width(IntrinsicSize.Min)
                .widthIn(min = minWidth)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp)
                .then(
                    if (isNumber) {
                        Modifier.clickable { showNumberPad = true }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = if (value.isBlank()) "0" else value,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                ),

                modifier = Modifier.wrapContentWidth()
            )
        }
    }

    if (showNumberPad && isNumber) {
        SalesOrderNumberPadDialog(
            title = label,
            initialValue = value,
            suffix = numberSuffix,
            onDismiss = { showNumberPad = false },
            onConfirm = {
                onValueChange(it)
                showNumberPad = false
            }
        )
    }
}

private val PadButtonGray = Color(0xFFF5F5F5)
private val PadButtonRed = Color(0xFFE53935)
private val PadButtonGreen = Color(0xFF43A047)
private val PadTextRed = Color(0xFFE53935)

@Composable
fun SalesOrderNumberPadDialog(
    title: String,
    initialValue: String,
    suffix: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var draft by remember(initialValue) { mutableStateOf(initialValue) }

    fun appendValue(input: String) {
        if (input == "." && draft.contains(".")) return
        draft = if (draft == "0" && input != ".") input else draft + input
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Calculator",
                        tint = Color(0xFFEF3636),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 18.sp
                    )
                }
                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (draft.isBlank()) "0" else draft,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                        if (!suffix.isNullOrBlank()) {
                            Text(text = "UOM ($suffix)", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SalesOrderNumberPadKey("1", Modifier.weight(1f)) { appendValue("1") }
                        SalesOrderNumberPadKey("2", Modifier.weight(1f)) { appendValue("2") }
                        SalesOrderNumberPadKey("3", Modifier.weight(1f)) { appendValue("3") }
                        SalesOrderNumberPadIconKey(
                            icon = Icons.Default.Close,
                            containerColor = PadButtonRed,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f)
                        ) { onDismiss() }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SalesOrderNumberPadKey("4", Modifier.weight(1f)) { appendValue("4") }
                        SalesOrderNumberPadKey("5", Modifier.weight(1f)) { appendValue("5") }
                        SalesOrderNumberPadKey("6", Modifier.weight(1f)) { appendValue("6") }
                        SalesOrderNumberPadIconKey(Icons.Default.Backspace, Modifier.weight(1f)) {
                            if (draft.isNotEmpty()) draft = draft.dropLast(1)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SalesOrderNumberPadKey("7", Modifier.weight(1f)) { appendValue("7") }
                        SalesOrderNumberPadKey("8", Modifier.weight(1f)) { appendValue("8") }
                        SalesOrderNumberPadKey("9", Modifier.weight(1f)) { appendValue("9") }
                        SalesOrderNumberPadKey(
                            text = "C",
                            textColor = PadTextRed,
                            modifier = Modifier.weight(1f)
                        ) { draft = "" }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SalesOrderNumberPadKey(".", Modifier.weight(1f)) { appendValue(".") }
                        SalesOrderNumberPadKey("0", Modifier.weight(1f)) { appendValue("0") }
                        SalesOrderNumberPadIconKey(
                            icon = Icons.Default.CheckCircle,
                            containerColor = PadButtonGreen,
                            contentColor = Color.White,
                            modifier = Modifier
                                .weight(2f)
                                .height(50.dp)
                        ) { onConfirm(draft) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesOrderNumberPadKey(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PadButtonGray,
            contentColor = textColor
        ),
        modifier = modifier.height(50.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun SalesOrderNumberPadIconKey(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = PadButtonGray,
    contentColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = modifier.height(50.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(25.dp))
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