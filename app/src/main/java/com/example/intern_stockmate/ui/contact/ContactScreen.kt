package com.example.intern_stockmate.ui.contact

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContactScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val supportNumbers = listOf(
        "+60164159488", "+60164150488",
        "+60164459488", "+60162169488",
        "+60165539488", "+60166159488",
        "+60162189488"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(scrollState)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEF3636)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SHL",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "BUSINESS SOLUTIONS SDN. BHD.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.5f))
            ) {
                Column {
                    ContactItem(
                        Icons.Default.Person,
                        "SHL Business Solutions Sdn. Bhd."
                    )
                    { }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    //phone number
                    ContactItem(Icons.Default.Phone, "+6044100483")
                    {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+6044100483"))
                        context.startActivity(intent)
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    ContactItem(
                        Icons.Default.LocationOn,
                        "No.B75, Taman Perindustrian Ringan Tandop Utama, Jalan Tandop 5, 05400 Alor Setar, Kedah"
                    )
                    {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("geo:0,0?q=SHL+Business+Solutions")
                        )
                        context.startActivity(intent)
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    ContactItem(
                        Icons.Default.Public, "https://www.shl-bsp.com/"
                    )
                    {
                        val intent =
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.shl-bsp.com/"))
                        context.startActivity(intent)
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    ContactItem(Icons.Default.Email, "support@shl-bsp.com")
                    {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@shl-bsp.com")
                            // Optional: Pre-fill the subject line
                            putExtra(Intent.EXTRA_SUBJECT, "Inquiry from SalesMate App")
                        }
                        context.startActivity(intent)
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    ContactItem(
                        Icons.Default.PlayCircle, "SHL Software Channel"
                    )
                    {
                        val youtubeUrl = "https://youtube.com/@shlsoftware?si=6NxLwBqs0N-n7Ppq"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))

                        intent.setPackage("com.google.android.youtube")

                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
                            context.startActivity(browserIntent)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SUPPORT HOTLINE",
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Card (
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    supportNumbers.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { number ->
                                HotlineButton(
                                    number = number,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val intent =
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Copyright 2026 @ SHL Business Solutions Sdn Bhd",
                fontSize = 12.sp,
                color = Color.Gray.copy(alpha = 0.8f),
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }
}

@Composable
fun ContactItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFF232F3E),
            lineHeight = 20.sp
        )
    }
}

@Composable
fun HotlineButton(number: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(55.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
        color = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = number, color = Color(0xFF007AFF), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}