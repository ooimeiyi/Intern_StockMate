package com.example.intern_stockmate.ui.accountBook

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.intern_stockmate.data.AccountBookContext
import com.example.intern_stockmate.model.AccountBook
import com.example.intern_stockmate.viewModel.AccountBookUiState
import com.example.intern_stockmate.viewModel.AccountBookViewModel


@Composable
fun AccountBookScreen(
    onConfirm: () -> Unit,
    accountBookViewModel: AccountBookViewModel = viewModel()
) {
    val uiState by accountBookViewModel.uiState.collectAsState()
    val savedAccountBookId by AccountBookContext.selectedAccountBookId.collectAsState()
    val context = LocalContext.current
    var selectedBookId by rememberSaveable(savedAccountBookId) {
        mutableStateOf(savedAccountBookId.ifBlank { null })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFDDE3F0), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Business,
                contentDescription = null,
                tint = Color.Blue,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to StockMate!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1C1E)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Please select your Company Account Book to continue. This will be saved for future logins.",
            fontSize = 14.sp,
            color = Color(0xFF74777F),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (val state = uiState) {
            AccountBookUiState.Loading -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is AccountBookUiState.Error -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            is AccountBookUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.books) { book ->
                        AccountBookItem(
                            book = book,
                            isSelected = selectedBookId == book.id,
                            onSelect = { selectedBookId = book.id }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val selectedId = selectedBookId ?: return@Button
                accountBookViewModel.saveSelectedAccountBook(
                    context = context,
                    accountBookId = selectedId,
                    onSuccess = onConfirm,
                    onError = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            enabled = selectedBookId != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEF3636),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE0E0E0),
                disabledContentColor = Color(0xFF9E9E9E)
            )
        ) {
            Text(
                text = "Confirm",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AccountBookItem(
    book: AccountBook,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) Color.Red else Color.Transparent

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(if (isSelected) 2.dp else 0.dp, borderColor), RoundedCornerShape(16.dp))
            .clickable { onSelect() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = book.id,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}