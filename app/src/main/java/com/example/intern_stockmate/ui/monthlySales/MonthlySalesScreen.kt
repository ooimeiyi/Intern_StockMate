package com.example.intern_stockmate.ui.monthlySales

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.intern_stockmate.model.MonthlySales
import com.example.intern_stockmate.viewModel.MonthlySalesUiState
import com.example.intern_stockmate.viewModel.MonthlySalesViewModel
import java.time.Year
import kotlin.compareTo
import kotlin.div
import kotlin.text.toFloat

private val monthLabels = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

@Composable
fun MonthlySalesScreenContainer(viewModel: MonthlySalesViewModel = viewModel()) {
    val state by viewModel.monthlySalesState.collectAsState()
    val selectedTabIndex = viewModel.selectedTabIndex.intValue
    val successState = state as? MonthlySalesUiState.Success
    val year = successState?.year ?: Year.now()
    val lastUpdate = successState?.lastUpdate.orEmpty()
    val rawData = successState?.data.orEmpty()

    val processedData = remember(rawData, selectedTabIndex) {
        MonthlySales.mergeWithFullYear(rawData).map { sales ->
            val amount = when (selectedTabIndex) {
                0 -> sales.posSales + sales.invoiceSales + sales.cashSales
                1 -> sales.posSales
                2 -> sales.invoiceSales
                3 -> sales.cashSales
                else -> 0.0
            }
            sales.copy(amount = amount)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F9))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MonthlyTabHeader(selectedTabIndex = selectedTabIndex, onTabSelected = viewModel::selectTab)
            MonthlySalesScreen(processedData, selectedTabIndex, year, lastUpdate)
        }

        when (state) {
            is MonthlySalesUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.Red)
                }
            }
            is MonthlySalesUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFCDD2))
                        .padding(12.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Text(
                        text = (state as MonthlySalesUiState.Error).message,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            is MonthlySalesUiState.Success -> Unit
        }
    }
}

@Composable
private fun MonthlyTabHeader(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("All", "POS", "Invoice", "Cash Sales")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEEEEEE))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTabIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .then(
                        if (isSelected) Modifier.background(Color.White, RoundedCornerShape(12.dp)).padding(2.dp)
                        else Modifier
                    )
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) Color.Red else Color.Black,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun MonthlySalesScreen(
    salesData: List<MonthlySales>,
    selectedTabIndex: Int,
    year: Year,
    lastUpdate: String
) {
    val scrollState = rememberScrollState()
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Monthly Sales - ${year.value}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                if (lastUpdate.isNotBlank()) {
                    Text(
                        text = "Last update: $lastUpdate",
                        fontSize = 12.sp,
                        color = Color(0xFF5A6B82)
                    )
                }
            }
        }

        MonthlyTrendCard(
            data = salesData,
            selectedTabIndex = selectedTabIndex,
            activeIndex = activeIndex,
            onActiveIndexChange = { activeIndex = it },
            interactionOffset = interactionOffset,
            onOffsetChange = { interactionOffset = it },
            isLocked = isLocked,
            onLockChange = { isLocked = it }
        )

        Text("DETAILED LOG", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        MonthlyDetailedLogSection(data = salesData, selectedTabIndex = selectedTabIndex)
    }
}

@Composable
private fun MonthlyTrendCard(
    data: List<MonthlySales>,
    selectedTabIndex: Int,
    activeIndex: Int,
    onActiveIndexChange: (Int) -> Unit,
    interactionOffset: Offset,
    onOffsetChange: (Offset) -> Unit,
    isLocked: Boolean,
    onLockChange: (Boolean) -> Unit
) {
    var canvasWidth by remember { mutableStateOf(0f) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val yAxisLabelStyle = TextStyle(color = Color.Black, fontSize = 12.sp)
    val xAxisLabelStyle = TextStyle(color = Color.Black, fontSize = 10.sp)
    val maxValueInData = data.maxOfOrNull { it.amount.toFloat() } ?: 0f
    val yAxisMax = if (maxValueInData <= 0f) 100f else maxValueInData * 1.15f

    val posColor = Color(0xFFFFC107)
    val invoiceColor = Color(0xFF2196F3)
    val cashColor = Color(0xFF4CAF50)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Monthly Sales Trend", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { canvasWidth = it.width.toFloat() }
                        .pointerInput(data, isLocked) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (isLocked) continue
                                    val pos = event.changes.first().position
                                    val paddingLeft = with(density) { 45.dp.toPx() }
                                    val paddingBottom = with(density) { 40.dp.toPx() }
                                    val chartBottom = size.height - paddingBottom

                                    if (event.type == PointerEventType.Move) {
                                        if (pos.y > chartBottom || pos.x < paddingLeft || pos.x > size.width) {
                                            onActiveIndexChange(-1)
                                        } else {
                                            onActiveIndexChange(calculateBarIndex(pos, canvasWidth, data.size, paddingLeft))
                                            onOffsetChange(pos)
                                        }
                                    } else if (event.type == PointerEventType.Exit) {
                                        onActiveIndexChange(-1)
                                    }
                                }
                            }
                        }
                        .pointerInput(data) {
                            detectTapGestures { offset ->
                                val hit = calculateBarIndex(offset, canvasWidth, data.size, with(density) { 45.dp.toPx() })
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
                    val paddingLeft = 45.dp.toPx()
                    val paddingBottom = 40.dp.toPx()
                    val chartWidth = size.width - paddingLeft
                    val chartHeight = size.height - paddingBottom
                    val step = yAxisMax / 4
                    val intervals = listOf(0f, step, step * 2, step * 3, yAxisMax)
                    intervals.forEach { value ->
                        val yPos = chartHeight - (value / yAxisMax * chartHeight)
                        drawLine(
                            color = Color.Black.copy(alpha = 0.25f),
                            start = Offset(paddingLeft, yPos),
                            end = Offset(size.width, yPos),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        val labelText = if (value >= 1000) String.format("%.1fk", value / 1000) else value.toInt().toString()
                        drawText(textMeasurer, labelText, Offset(0f, yPos - 8.dp.toPx()), yAxisLabelStyle)
                    }

                    val barWidth = (chartWidth / data.size) * 0.65f
                    val spaceBetween = (chartWidth / data.size) * 0.35f

                    if (activeIndex != -1) {
                        val xPosHighlight = paddingLeft + (activeIndex * (barWidth + spaceBetween))
                        drawRect(
                            color = Color.Black.copy(alpha = 0.05f),
                            topLeft = Offset(xPosHighlight, 0f),
                            size = Size(barWidth + spaceBetween, chartHeight)
                        )
                    }

                    data.forEachIndexed { index, sales ->
                        val xPos = paddingLeft + (index * (barWidth + spaceBetween)) + (spaceBetween / 2)
                        val barColor = when (selectedTabIndex) {
                            1 -> posColor
                            2 -> invoiceColor
                            3 -> cashColor
                            else -> Color.Gray
                        }

                        if (selectedTabIndex == 0) {
                            val segments = listOf(
                                sales.posSales.toFloat() to posColor,
                                sales.invoiceSales.toFloat() to invoiceColor,
                                sales.cashSales.toFloat() to cashColor
                            )
                            var accumulatedHeight = 0f
                            segments.forEach { (value, color) ->
                                if (value <= 0f) return@forEach
                                val segmentHeight = (value / yAxisMax) * chartHeight
                                val top = chartHeight - accumulatedHeight - segmentHeight
                                drawRoundRect(
                                    color = if (activeIndex == index) color.copy(alpha = 0.7f) else color,
                                    topLeft = Offset(xPos, top),
                                    size = Size(barWidth, segmentHeight),
                                    cornerRadius = CornerRadius(0f, 0f)
                                )
                                accumulatedHeight += segmentHeight
                            }
                        } else {
                            val barHeight = (sales.amount.toFloat() / yAxisMax) * chartHeight
                            drawRoundRect(
                                color = if (activeIndex == index) barColor.copy(alpha = 0.7f) else barColor,
                                topLeft = Offset(xPos, chartHeight - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                        }

                        val monthLabel = if (sales.month in 2..12 step 2) monthLabels[sales.month - 1] else ""
                        if (monthLabel.isNotEmpty()) {
                            val textLayout = textMeasurer.measure(monthLabel, xAxisLabelStyle)
                            drawText(
                                textMeasurer,
                                monthLabel,
                                Offset(xPos - textLayout.size.width / 2, chartHeight + 12.dp.toPx()),
                                xAxisLabelStyle
                            )
                        }
                    }
                }

                if (activeIndex != -1 && activeIndex < data.size) {
                    val active = data[activeIndex]
                    val popupWidth = 150
                    val popupHeight = if (selectedTabIndex == 0) 130 else 80
                    val popupX = (interactionOffset.x.toInt() + 50).coerceIn(0, canvasWidth.toInt().coerceAtLeast(popupWidth) - popupWidth)
                    val popupY = (interactionOffset.y.toInt() - popupHeight - 90).coerceAtLeast(0)
                    val monthLabel = monthLabels.getOrElse(active.month - 1) { "N/A" }
                    val (labelText, labelColor) = when (selectedTabIndex) {
                        1 -> "POS: RM ${String.format("%,.0f", active.amount)}" to posColor
                        2 -> "Invoice: RM ${String.format("%,.0f", active.amount)}" to invoiceColor
                        3 -> "Cash Sales: RM ${String.format("%,.0f", active.amount)}" to cashColor
                        else -> "Total: RM ${String.format("%,.0f", active.amount)}" to Color.Red
                    }

                    Popup(offset = IntOffset(popupX, popupY)) {
                        Surface(shadowElevation = 8.dp, shape = RoundedCornerShape(8.dp), color = Color.White) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(monthLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(labelText, fontSize = 14.sp, color = labelColor, fontWeight = FontWeight.Bold)
                                if (selectedTabIndex == 0) {
                                    Text("POS : RM ${String.format("%,.0f", active.posSales)}", fontSize = 12.sp, color = posColor, fontWeight = FontWeight.SemiBold)
                                    Text("Invoice : RM ${String.format("%,.0f", active.invoiceSales)}", fontSize = 12.sp, color = invoiceColor, fontWeight = FontWeight.SemiBold)
                                    Text("Cash : RM ${String.format("%,.0f", active.cashSales)}", fontSize = 12.sp, color = cashColor, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateBarIndex(offset: Offset, canvasWidth: Float, totalBars: Int, paddingLeft: Float): Int {
    if (totalBars == 0 || offset.x < paddingLeft) return -1
    val chartWidth = canvasWidth - paddingLeft
    val index = ((offset.x - paddingLeft) / (chartWidth / totalBars)).toInt()
    return if (index in 0 until totalBars) index else -1
}

@Composable
private fun MonthlyChartLegend(color: Color, label: String, amount: Double, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text("$label: ${String.format("%,.0f", amount)}", fontSize = 12.sp, color = Color(0xFF5A6B82))
    }
}

@Composable
fun MonthlyDetailedLogSection(data: List<MonthlySales>, selectedTabIndex: Int) {
    val maxSales = data.maxOfOrNull { it.amount } ?: 1.0

    val posColor = Color(0xFFFFC107)
    val invoiceColor = Color(0xFF2196F3)
    val cashColor = Color(0xFF4CAF50)
    val defaultBarColor = when (selectedTabIndex) {
        1 -> posColor
        2 -> invoiceColor
        3 -> cashColor
        else -> invoiceColor
    }

    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        data.forEach { sales ->
            val monthLabel = monthNames.getOrElse(sales.month - 1) { "N/A" }
            val totalForAll = sales.posSales + sales.invoiceSales + sales.cashSales

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFE3F2FD).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = monthLabel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color(0xFF1976D2),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "RM ${String.format("%,.0f", sales.amount)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp))
                    ) {
                        val progressFraction = (sales.amount / maxSales).toFloat().coerceIn(0f, 1f)

                        if (selectedTabIndex == 0 && totalForAll > 0.0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                            ) {
                                val posFraction = (sales.posSales / totalForAll).toFloat()
                                val invoiceFraction = (sales.invoiceSales / totalForAll).toFloat()
                                val cashFraction = (sales.cashSales / totalForAll).toFloat()

                                val hasPos = sales.posSales > 0
                                val hasInvoice = sales.invoiceSales > 0
                                val hasCash = sales.cashSales > 0
                                val firstSegment = when {
                                    hasPos -> "pos"
                                    hasInvoice -> "invoice"
                                    else -> "cash"
                                }
                                val lastSegment = when {
                                    hasCash -> "cash"
                                    hasInvoice -> "invoice"
                                    else -> "pos"
                                }
                                val segmentShape = { key: String ->
                                    RoundedCornerShape(
                                        topStart = if (key == firstSegment) 4.dp else 0.dp,
                                        bottomStart = if (key == firstSegment) 4.dp else 0.dp,
                                        topEnd = if (key == lastSegment) 4.dp else 0.dp,
                                        bottomEnd = if (key == lastSegment) 4.dp else 0.dp
                                    )
                                }
                                if (hasPos) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(posFraction)
                                            .background(posColor, segmentShape("pos"))
                                    )
                                }
                                if (hasInvoice) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(invoiceFraction)
                                            .background(invoiceColor, segmentShape("invoice"))
                                    )
                                }
                                if (hasCash) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(cashFraction)
                                            .background(cashColor, segmentShape("cash"))
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .fillMaxHeight()
                                    .background(defaultBarColor, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                    if (selectedTabIndex == 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MonthlyChartLegend(
                                color = posColor,
                                label = "POS",
                                amount = sales.posSales,
                                modifier = Modifier.weight(1f)
                            )
                            MonthlyChartLegend(
                                color = invoiceColor,
                                label = "Inv",
                                amount = sales.invoiceSales,
                                modifier = Modifier.weight(1f)
                            )
                            MonthlyChartLegend(
                                color = cashColor,
                                label = "Cash",
                                amount = sales.cashSales,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}