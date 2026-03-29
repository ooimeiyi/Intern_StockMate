package com.example.intern_stockmate.ui.stockLIst

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.intern_stockmate.R
import com.example.intern_stockmate.model.StockItem
import com.example.intern_stockmate.utils.base64ToBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    navController: NavHostController,
    item: StockItem
) {
    val scrollState = rememberScrollState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                navController.popBackStack()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stock Details", fontSize = 20.sp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Red)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(scrollState)
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .height(220.dp)
                    .background(Color.White)
            ) {
                val painter = when {
                    selectedImageUri != null -> rememberAsyncImagePainter(selectedImageUri)
                    !item.itemPhoto.isNullOrBlank() -> {
                        runCatching { base64ToBitmap(item.itemPhoto).asImageBitmap() }.getOrNull()
                            ?.let { androidx.compose.ui.graphics.painter.BitmapPainter(it) }
                            ?: painterResource(id = R.drawable.stock_mate_logo)
                    }

                    else -> painterResource(id = R.drawable.stock_mate_logo)
                }

                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.Center)
                        .background(Color.White),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart),
                    color = Color.LightGray,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (item.itemGroup.isNullOrBlank()) "N/A" else item.itemGroup,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black)
                }

                Surface(
                    modifier = Modifier.padding(16.dp).align(Alignment.BottomEnd),
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.uom.uppercase(),
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

            Column(modifier = Modifier.padding(20.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Item Code: ${item.itemCode.ifBlank { "-" }}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Desc 1: ${item.description.ifBlank { "-" }}",
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "Desc 2: ${item.desc2?.ifBlank { "-" } ?: "-"}",
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        icon = Icons.Default.Inventory2,
                        label = "Stock Balance",
                        value = item.balQty.toString(),
                    )

                    InfoCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        icon = Icons.Default.LocationOn,
                        label = "Shelf",
                        value = "",
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Shelf   : ${item.shelf?.ifBlank { "N/A" } ?: "N/A"}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Active : ${item.isActive ?: "Yes"}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val pricingScrollState = rememberScrollState()

                SectionHeader(icon = Icons.Default.Inventory2, title = "UOM & Pricing")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(0.5.dp, Color.LightGray)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(pricingScrollState)
                    ) {
                        Row(
                            modifier = Modifier
                                .width(650.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "UOM",
                                modifier = Modifier.width(70.dp),
                                fontSize = 13.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "RATE",
                                modifier = Modifier.width(60.dp),
                                fontSize = 11.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "PRICE 1",
                                modifier = Modifier.width(85.dp),
                                fontSize = 11.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "PRICE 2",
                                modifier = Modifier.width(85.dp),
                                fontSize = 11.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "PRICE 3",
                                modifier = Modifier.width(85.dp),
                                fontSize = 11.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "PRICE 4",
                                modifier = Modifier.width(85.dp),
                                fontSize = 11.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "PRICE 5",
                                modifier = Modifier.width(85.dp),
                                fontSize = 11.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "PRICE 6",
                                modifier = Modifier.width(85.dp),
                                fontSize = 11.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

                        item.uomList.forEach { uomInfo ->
                            Row(
                                modifier = Modifier
                                    .width(650.dp)
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    uomInfo.uom,
                                    modifier = Modifier.width(70.dp),
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    uomInfo.rate.toInt().toString(),
                                    modifier = Modifier.width(60.dp),
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )

                                PriceCell(uomInfo.price1)
                                PriceCell(uomInfo.price2)
                                PriceCell(uomInfo.price3)
                                PriceCell(uomInfo.price4)
                                PriceCell(uomInfo.price5)
                                PriceCell(uomInfo.price6)
                            }
                            HorizontalDivider(
                                modifier = Modifier.width(650.dp),
                                thickness = 0.5.dp,
                                color = Color.LightGray.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                SectionHeader(icon = Icons.Default.LocationOn, title = "Stock by Location")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(0.5.dp, Color.LightGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text(
                                "LOCATION",
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "QTY",
                                modifier = Modifier.width(60.dp),
                                fontSize = 12.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

                        item.locationList.forEach { locInfo ->
                            LocationDetailRow(
                                location = locInfo.location,
                                qty = locInfo.qty.toString()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun InfoCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            content?.invoke(this)
        }
    }
}

@Composable
fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color.Black)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
    }
}

@Composable
fun LocationDetailRow(location: String, qty: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = location,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black)
        Text(text = qty,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Red,
            modifier = Modifier.weight(0.25f))
    }
    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
}

@Composable
fun PriceCell(price: Double, isRed: Boolean = false) {
    Text(
        text = String.format("%.2f", price),
        modifier = Modifier.width(85.dp),
        fontSize = 14.sp,
        color = if (isRed) Color.Red else Color.Black,
        fontWeight = if (isRed) FontWeight.Bold else FontWeight.Normal
    )
}