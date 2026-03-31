package com.example.intern_stockmate.ui.salesOverview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.intern_stockmate.viewModel.SalesOverviewUiState
import com.example.intern_stockmate.viewModel.SalesOverviewViewModel
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

private data class ChartSegment(
    val name: String,
    val value: Double,
    val percent: Float,
    val color: Color
)

@Composable
fun SalesOverviewScreenContainer(
    viewModel: SalesOverviewViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(viewModel.selectedTabIndex.intValue) }

    val successState = state as? SalesOverviewUiState.Success
    val total = successState?.todayData?.totalSalesToday ?: successState?.summaryData?.total ?: 0.0
    val cash = successState?.todayData?.cashSales ?: successState?.summaryData?.cashSale ?: 0.0
    val invoice = successState?.todayData?.invoiceSales ?: successState?.summaryData?.invoice ?: 0.0
    val pos = successState?.todayData?.posSales ?: successState?.summaryData?.pos ?: 0.0
    val lastUpdate = successState?.todayData?.lastUpdate ?: successState?.summaryData?.lastUpdate ?: ""

    Column(modifier = Modifier.fillMaxSize()) {
        TabHeader(selectedTabIndex = selectedTabIndex) { index ->
            selectedTabIndex = index
            viewModel.selectTab(index)
        }

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7F9))) {
            SalesDashboardScreen(
                total = total,
                cash = cash,
                invoice = invoice,
                pos = pos,
                lastUpdate = if (lastUpdate.isBlank()) "N/A" else lastUpdate,
                tabTitle = SalesOverviewViewModel.TABS[selectedTabIndex]
            )

            if (state is SalesOverviewUiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun TabHeader(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEEEEEE))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SalesOverviewViewModel.TABS.forEachIndexed { index, tab ->
            val isSelected = selectedTabIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .then(
                        if (isSelected) {
                            Modifier
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab,
                    color = if (isSelected) Color.Red else Color.Black,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SalesDashboardScreen(
    total: Double,
    cash: Double,
    invoice: Double,
    pos: Double,
    lastUpdate: String,
    tabTitle: String
) {
    val scrollState = rememberScrollState()

    val segments = listOf(
        ChartSegment("POS", pos, if (total > 0) (pos / total * 100).toFloat() else 0f, Color(0xFFFFC107)),
        ChartSegment("Invoice", invoice, if (total > 0) (invoice / total * 100).toFloat() else 0f, Color(0xFF2196F3)),
        ChartSegment("Cash Sales", cash, if (total > 0) (cash / total * 100).toFloat() else 0f, Color(0xFF4CAF50))
    )

    var activeIndex by remember { mutableIntStateOf(-1) }
    var interactionOffset by remember { mutableStateOf(Offset.Zero) }
    var isLocked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F9))
            .pointerInput(Unit) {
                detectTapGestures {
                    if (isLocked) {
                        isLocked = false
                        activeIndex = -1
                    }
                }
            }
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TotalSalesCard(totalSales = total, lastUpdate = lastUpdate, tabTitle = tabTitle)

        DistributionChartCard(
            totalSales = total,
            segments = segments,
            activeIndex = activeIndex,
            onActiveIndexChange = { activeIndex = it },
            interactionOffset = interactionOffset,
            onOffsetChange = { interactionOffset = it },
            isLocked = isLocked,
            onLockChange = { isLocked = it }
        )

        Text(
            text = "BREAKDOWN",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(top = 8.dp)
        )

        segments.forEach { segment ->
            DetailedBreakdownCard(
                title = segment.name,
                revenue = segment.value,
                percentage = segment.percent.roundToInt(),
                color = segment.color,
                icon = when (segment.name) {
                    "Invoice" -> Icons.Default.Description
                    "Cash Sales" -> Icons.Default.Payments
                    else -> Icons.Default.PointOfSale
                }
            )
        }
    }
}

@Composable
private fun TotalSalesCard(totalSales: Double, lastUpdate: String, tabTitle: String) {
    val title = when (tabTitle) {
        "Today" -> "TOTAL SALES TODAY"
        "Week" -> "TOTAL SALES THIS WEEK"
        "Month" -> "TOTAL SALES THIS MONTH"
        "Year" -> "TOTAL SALES THIS YEAR"
        else -> "TOTAL SALES"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF3636))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.AttachMoney, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                }
            }
            Text(
                "RM ${String.format("%,.2f", totalSales)}",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(color = Color.Black.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Last Update: ", color = Color.White, fontSize = 13.sp)
                    Text(lastUpdate, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DistributionChartCard(
    totalSales: Double,
    segments: List<ChartSegment>,
    activeIndex: Int,
    onActiveIndexChange: (Int) -> Unit,
    interactionOffset: Offset,
    onOffsetChange: (Offset) -> Unit,
    isLocked: Boolean,
    onLockChange: (Boolean) -> Unit
) {
    val strokeWidthPx = 60f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Sales Distribution",
                modifier = Modifier.align(Alignment.Start),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(28.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(segments, isLocked) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (isLocked) continue
                                    if (event.type == PointerEventType.Move) {
                                        val pos = event.changes.first().position
                                        val hit = calculateHitIndex(pos, size.width.toFloat(), segments, strokeWidthPx)
                                        onActiveIndexChange(hit)
                                        onOffsetChange(pos)
                                    } else if (event.type == PointerEventType.Exit) {
                                        onActiveIndexChange(-1)
                                    }
                                }
                            }
                        }
                        .pointerInput(segments) {
                            detectTapGestures { offset ->
                                val hit = calculateHitIndex(offset, size.width.toFloat(), segments, strokeWidthPx)
                                if (hit != -1) {
                                    onActiveIndexChange(hit)
                                    onOffsetChange(offset)
                                    onLockChange(true)
                                } else {
                                    onActiveIndexChange(-1)
                                    onLockChange(false)
                                }
                            }
                        }
                ) {
                    val radius = size.minDimension / 2.5f
                    var startAngle = -90f

                    segments.forEachIndexed { index, segment ->
                        val sweepAngle = (segment.percent / 100f) * 360f
                        val isActive = activeIndex == index

                        drawArc(
                            color = segment.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = if (isActive) strokeWidthPx + 15f else strokeWidthPx),
                            size = Size(radius * 2, radius * 2),
                            topLeft = Offset(center.x - radius, center.y - radius),
                            alpha = if (activeIndex == -1 || isActive) 1f else 0.3f
                        )
                        startAngle += sweepAngle
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Sales", fontSize = 15.sp, color = Color.Black)
                    Text(
                        "RM ${String.format("%,.2f", totalSales)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }

                if (activeIndex != -1) {
                    val active = segments[activeIndex]
                    Popup(offset = IntOffset(interactionOffset.x.toInt() - 100, interactionOffset.y.toInt() - 300)) {
                        Surface(
                            shadowElevation = 10.dp,
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(active.name, fontWeight = FontWeight.Bold, color = active.color, fontSize = 13.sp)
                                Text(
                                    "RM ${String.format("%,.2f", active.value)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                                Text(
                                    "${String.format("%.1f", active.percent)}% of total",
                                    fontSize = 11.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                segments.forEach { ChartLegend(it.color, it.name, it.percent.roundToInt()) }
            }
        }
    }
}

private fun calculateHitIndex(
    offset: Offset,
    canvasSize: Float,
    segments: List<ChartSegment>,
    strokeWidth: Float
): Int {
    val centerX = canvasSize / 2
    val centerY = canvasSize / 2
    val dx = offset.x - centerX
    val dy = offset.y - centerY
    val dist = sqrt(dx * dx + dy * dy)

    val midRadius = canvasSize / 2.5f
    val innerBound = midRadius - (strokeWidth / 2)
    val outerBound = midRadius + (strokeWidth / 2)

    if (dist < innerBound || dist > outerBound) return -1

    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    angle = (angle + 90f + 360f) % 360f

    var currentAngle = 0f
    segments.forEachIndexed { index, segment ->
        val sweep = (segment.percent / 100f) * 360f
        if (angle >= currentAngle && angle <= currentAngle + sweep) return index
        currentAngle += sweep
    }
    return -1
}

@Composable
private fun ChartLegend(color: Color, label: String, percent: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = Color.Black)
        Text(" ($percent%)", fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun DetailedBreakdownCard(
    title: String,
    revenue: Double,
    percentage: Int,
    color: Color,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.fillMaxHeight().width(5.dp).background(color))

            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.1f)) {
                            Icon(icon, null, modifier = Modifier.padding(8.dp).size(20.dp), tint = color)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE3F2FD)) {
                        Text(
                            text = "$percentage%",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "REVENUE",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "RM ${String.format("%,.2f", revenue)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            }
        }
    }
}