package com.example.intern_stockmate.ui.configuration

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.Composable
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
import com.example.intern_stockmate.viewModel.LoginViewModel
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.intern_stockmate.viewModel.CompanyListUiState
import com.example.intern_stockmate.viewModel.ConfigurationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

    var companyExpanded by remember { mutableStateOf(false) }

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var resetCode by remember { mutableStateOf("") }
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var resetCodeVisible by remember { mutableStateOf(false) }
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
                ?: selectedCompanyId

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

        ManagementCard(title = "Change Password", icon = Icons.Default.Lock) {

            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                placeholder = { Text("Old Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = textFieldColors,
                trailingIcon = {
                    IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                        Icon(
                            imageVector = if (oldPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                placeholder = { Text("New Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = textFieldColors,
                trailingIcon = {
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(
                            imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        val result = loginViewModel.changePassword(
                            oldPassword = oldPassword,
                            newPassword = newPassword
                        )

                        if (result.isSuccess) {
                            Toast.makeText(context, "Password changed.", Toast.LENGTH_SHORT).show()
                            oldPassword = ""
                            newPassword = ""
                        } else {
                            val errorMsg = result.exceptionOrNull()?.message ?: "Unable to change password"
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF3636))
            ) {
                Text(text = "Update Password", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        ManagementCard(title = "Reset Admin Password", icon = Icons.Default.Lock) {
            OutlinedTextField(
                value = resetCode,
                onValueChange = { resetCode = it },
                placeholder = { Text("Enter reset Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (resetCodeVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = textFieldColors,
                trailingIcon = {
                    IconButton(onClick = { resetCodeVisible = !resetCodeVisible }) {
                        Icon(
                            imageVector = if (resetCodeVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        val result = loginViewModel.resetAdminPassword(resetCode)
                        if (result.isSuccess) {
                            Toast.makeText(context, "Admin Password Reset to 'Admin'.", Toast.LENGTH_SHORT).show()
                            resetCode = ""
                        } else {
                            val errorMsg = result.exceptionOrNull()?.message ?: "Unable to Reset password"
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF3636))
            ) {
                Text(text = "Reset Password", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
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