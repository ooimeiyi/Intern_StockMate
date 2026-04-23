package com.example.intern_stockmate.ui.configuration

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.intern_stockmate.data.AccountBookContext
import com.example.intern_stockmate.data.DocumentNumberFormatStore
import com.example.intern_stockmate.viewModel.LoginViewModel
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.intern_stockmate.viewModel.CompanyListUiState
import com.example.intern_stockmate.model.StockAccessRights
import com.example.intern_stockmate.viewModel.ConfigurationViewModel
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    innerPadding: PaddingValues,
    loginViewModel: LoginViewModel,
    scope: CoroutineScope
) {
    val configurationViewModel: ConfigurationViewModel = viewModel()
    val companyListState by configurationViewModel.companyListState.collectAsState()
    val selectedCompanyId by configurationViewModel.selectedCompanyId.collectAsState()
    val selectedAccountBookId by AccountBookContext.selectedAccountBookId.collectAsState()
    val savedSalesOrderFormat by configurationViewModel.salesOrderFormat.collectAsState()
    val savedStockAdjustmentFormat by configurationViewModel.stockAdjustmentFormat.collectAsState()

    var companyExpanded by remember { mutableStateOf(false) }
    var salesOrderFormat by remember(savedSalesOrderFormat) { mutableStateOf(savedSalesOrderFormat) }
    var adjustmentFormat by remember(savedStockAdjustmentFormat) { mutableStateOf(savedStockAdjustmentFormat) }
    val savedAdminPassword by configurationViewModel.adminPassword.collectAsState()
    val savedStockPassword by configurationViewModel.stockPassword.collectAsState()
    val enabledStockAccessRoutes by configurationViewModel.enabledStockAccessRoutes.collectAsState()
    var adminPassword by remember(savedAdminPassword) { mutableStateOf(savedAdminPassword) }
    var stockPassword by remember(savedStockPassword) { mutableStateOf(savedStockPassword) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current


    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        cursorColor = Color.Black,
        unfocusedContainerColor = Color(0xFFF5F5F5),
        focusedContainerColor = Color(0xFFF5F5F5),
        unfocusedBorderColor = Color(0xFFE0E0E0),
        focusedBorderColor = Color.Red
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            },
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ManagementCard(title = "Company", icon = Icons.Default.Business) {
            val companyOptions = when (val state = companyListState) {
                is CompanyListUiState.Success -> state.companies
                else -> emptyList()
            }
            val selectedDisplay = companyOptions
                .firstOrNull { it.id == selectedCompanyId }
                ?.displayName
                .orEmpty()

            ExposedDropdownMenuBox(
                expanded = companyExpanded,
                onExpandedChange = { companyExpanded = !companyExpanded }
            ) {
                OutlinedTextField(
                    value = selectedDisplay,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(8.dp)
                )

                ExposedDropdownMenu(
                    expanded = companyExpanded,
                    onDismissRequest = { companyExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    companyOptions.forEach { company ->
                        DropdownMenuItem(
                            text = { Text(company.displayName, color = Color.Black) },
                            onClick = {
                                configurationViewModel.selectCompany(company.id)
                                companyExpanded = false
                                Toast.makeText(
                                    context,
                                    "Company changed to ${company.displayName}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }

            when (companyListState) {
                is CompanyListUiState.Loading -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loading companies...", color = Color.Gray, fontSize = 12.sp)
                }
                is CompanyListUiState.Error -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (companyListState as CompanyListUiState.Error).message,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
                else -> Unit
            }
        }

        ManagementCard(title = "Account Book", icon = Icons.Default.MenuBook) {
            Text(
                text = if (selectedAccountBookId.isBlank()) "No account book selected"
                else selectedAccountBookId,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }

        ManagementCard(title = "Stock Access Rights", icon = Icons.Default.Settings) {
            Text(
                text = "When a user logs in with the 'stock' profile, they only have access to Stock List, Stock Take, and Sales Order by default. You can enable additional modules for them below: ",
                fontSize = 14.sp,
                color = Color.Gray,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            StockAccessRights.configurableRights.forEach { option ->
                PermissionItem(
                    title = option.title,
                    subtitle = option.subtitle,
                    isEnabled = enabledStockAccessRoutes.contains(option.route),
                    onCheckedChange = { enabled ->
                        val result = configurationViewModel.updateStockAccessRight(option.route, enabled)
                        if (result.isFailure) {
                            Toast.makeText(
                                context,
                                result.exceptionOrNull()?.message ?: "Unable to update access right",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }


        ManagementCard(title = "Login Passwords", icon = Icons.Outlined.PersonOutline) {
            Text(
                text = "Update the passwords required to access the Admin and Stock profiles.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            PasswordInputRow(
                label = "Admin Password",
                value = adminPassword,
                onValueChange = { adminPassword = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PasswordInputRow(
                label = "Stock User Password",
                value = stockPassword,
                onValueChange = { stockPassword = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val result = configurationViewModel.saveAccessPasswords(
                        adminPassword = adminPassword,
                        stockPassword = stockPassword
                    )

                    if (result.isSuccess) {
                        Toast.makeText(context, "Passwords Saved", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context,
                            result.exceptionOrNull()?.message ?: "Unable to save passwords",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF3636))
            ) {
                Text(
                    text = "Save Passwords",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        ManagementCard(title = "Document Number Format", icon = Icons.Default.Settings) {
            Text(
                text = "Set the format for generated document numbers (e.g., Sales Orders). " +
                        "Use placeholders like {SM} , {SO}, and {00000} for auto-incrementing numbers.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "Sales Order Number Format",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = salesOrderFormat,
                onValueChange = { salesOrderFormat = it },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .background(Color(0xFFF9FAFB), RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Preview: ${DocumentNumberFormatStore.formatPreview(salesOrderFormat)}",
                    fontSize = 12.sp,
                    color = Color(0xFF2563EB)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 20.dp),
                thickness = 1.dp,
                color = Color(0xFFF0F0F0)
            )

            Text(
                text = "Stock Take Number Format",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = adjustmentFormat,
                onValueChange = { adjustmentFormat = it },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .background(Color(0xFFF9FAFB), RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Preview: ${DocumentNumberFormatStore.formatPreview(adjustmentFormat)}",
                    fontSize = 12.sp,
                    color = Color(0xFF2563EB)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val result = configurationViewModel.saveDocumentFormats(
                        salesOrderFormat = salesOrderFormat,
                        stockAdjustmentFormat = adjustmentFormat
                    )

                    if (result.isSuccess) {
                        Toast.makeText(context, "Document Format Saved", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context,
                            result.exceptionOrNull()?.message ?: "Unable to save formats",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF3636))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Save Format",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onCheckedChange,

            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFEF3636),

                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray,

                checkedBorderColor = Color(0xFFEF3636),
                uncheckedBorderColor = Color.LightGray
        )
        )
    }
}

@Composable
private fun ManagementCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
            content()
        }
    }
}

@Composable
private fun PasswordInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        cursorColor = Color.Black
    )

    Column(modifier = Modifier.fillMaxWidth()) {

        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text("Enter password", color = Color.LightGray)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.LightGray
                )
            },
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.LightGray
                    )
                }
            },
            visualTransformation = if (visible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors,
            singleLine = true
        )
    }
}