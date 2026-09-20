package com.lanu.notdefteri.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.data.model.NoteWithTags
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsSheet(
    items: List<NoteWithTags>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val notes = items.map { it.note }
    // 1. Core aggregates
    val totalNotes = notes.size
    val pinnedNotesCount = notes.count { it.isPinned }
    val remindersCount = notes.count { it.reminderTime != null && it.reminderTime > System.currentTimeMillis() }
    val categorizedNotesCount = items.count { it.tags.isNotEmpty() }

    // 2. Compute last 6 months note creation speed
    val monthlyData = remember(notes) {
        val calendar = Calendar.getInstance()
        val dataList = mutableListOf<MonthData>()
        val sdf = SimpleDateFormat("MMM", Locale("tr", "TR"))

        for (i in 5 downTo 0) {
            val targetCal = Calendar.getInstance()
            targetCal.add(Calendar.MONTH, -i)
            val monthYearKey = "${targetCal.get(Calendar.YEAR)}-${targetCal.get(Calendar.MONTH)}"
            val displayName = sdf.format(targetCal.time).replaceFirstChar { it.uppercase() }
            
            // Count notes matching this month & year
            val count = notes.count { note ->
                calendar.timeInMillis = note.createdAt
                calendar.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                        calendar.get(Calendar.MONTH) == targetCal.get(Calendar.MONTH)
            }
            dataList.add(MonthData(displayName, count))
        }
        dataList
    }

    // 3. Tag Usage distribution percentages
    val tagDistribution = remember(items) {
        val totalWithTag = items.size
        if (totalWithTag == 0) return@remember emptyList<TagShare>()

        val rawCounts = mutableMapOf<String, Int>()
        items.forEach { item ->
            if (item.tags.isEmpty()) {
                rawCounts["Etiketsiz"] = (rawCounts["Etiketsiz"] ?: 0) + 1
            } else {
                item.tags.forEach { tag ->
                    rawCounts[tag.name] = (rawCounts[tag.name] ?: 0) + 1
                }
            }
        }

        val colors = listOf(
            Color(0xFF6366F1), // Indigo
            Color(0xFF3B82F6), // Blue
            Color(0xFF10B981), // Emerald
            Color(0xFFF59E0B), // Amber
            Color(0xFFEF4444), // Rose
            Color(0xFF8B5CF6), // Violet
            Color(0xFFEC4899), // Pink
            Color(0xFF14B8A6), // Teal
            Color(0xFF6B7280)  // Gray
        )

        rawCounts.entries.sortedByDescending { it.value }.mapIndexed { index, entry ->
            val share = (entry.value.toFloat() / totalWithTag) * 100f
            val col = colors[index % colors.size]
            TagShare(name = entry.key, count = entry.value, percentage = share, color = col)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
                .testTag("analytics_bottom_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Not Analizleri & İstatistikler",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary KPI Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiCard(
                        title = "Toplam Not",
                        value = totalNotes.toString(),
                        icon = Icons.Default.Description,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Sabitlenen",
                        value = pinnedNotesCount.toString(),
                        icon = Icons.Default.PushPin,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiCard(
                        title = "Aktif Alarm",
                        value = remindersCount.toString(),
                        icon = Icons.Default.NotificationsActive,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Etiketliler",
                        value = categorizedNotesCount.toString(),
                        icon = Icons.Default.Label,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 1. Monthly velocity chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = borderStroke()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Aylık Not Üretim Hızı (Son 6 Ay)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ay bazında yeni oluşturulan sesli ve yazılı not sayıları",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        MonthlyVelocityChart(monthlyData)
                    }
                }

                // 2. Tag Usage ratios donut chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = borderStroke()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Etiket Kullanım Oranları (%)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Kategorilerin toplam not kütüphanesindeki dağılımı",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (notes.isEmpty()) {
                            Text(
                                text = "İstatistik oluşturulması için henüz hiç not bulunmuyor.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                            )
                        } else {
                            TagDistributionChart(tagDistribution)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

data class MonthData(val month: String, val count: Int)
data class TagShare(val name: String, val count: Int, val percentage: Float, val color: Color)

@Composable
fun borderStroke() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
)

@Composable
fun KpiCard(
    title: String,
    value: String,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun MonthlyVelocityChart(data: List<MonthData>) {
    val maxCount = remember(data) { data.maxOfOrNull { it.count }?.coerceAtLeast(5) ?: 5 }
    val primaryColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val bottomLabelHeight = 24.dp.toPx()
            val leftAxisWidth = 32.dp.toPx()

            val graphWidth = w - leftAxisWidth
            val graphHeight = h - bottomLabelHeight

            // Draw Y Grid lines (3 horizontal levels)
            val gridLevels = 4
            for (i in 0 until gridLevels) {
                val ratio = i.toFloat() / (gridLevels - 1)
                val y = graphHeight * ratio
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(leftAxisWidth, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )

                // Level text
                val textValue = (maxCount - (ratio * maxCount).toInt()).toString()
                drawContext.canvas.nativeCanvas.drawText(
                    textValue,
                    8.dp.toPx(),
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = labelColor.hashCode()
                        textSize = 9.sp.toPx()
                        textAlign = android.graphics.Paint.Align.LEFT
                    }
                )
            }

            // Draw Bars and Labels
            val barCount = data.size
            val stepX = graphWidth / barCount
            val barWidth = (stepX * 0.45f)

            data.forEachIndexed { index, item ->
                val centerX = leftAxisWidth + (stepX * index) + (stepX / 2)
                val barHeight = if (maxCount > 0) {
                    (item.count.toFloat() / maxCount) * graphHeight
                } else {
                    0f
                }

                val left = centerX - (barWidth / 2)
                val top = graphHeight - barHeight

                // Gradient Brush for the elegant Rounded Bars
                val barBrush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor,
                        primaryColor.copy(alpha = 0.5f)
                    )
                )

                // Draw Bar
                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Draw value on top of bar (if > 0)
                if (item.count > 0) {
                    drawContext.canvas.nativeCanvas.drawText(
                        item.count.toString(),
                        centerX,
                        top - 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = primaryColor.hashCode()
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )
                }

                // Draw Month Name underneath
                drawContext.canvas.nativeCanvas.drawText(
                    item.month,
                    centerX,
                    h - 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = labelColor.hashCode()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

@Composable
fun TagDistributionChart(distribution: List<TagShare>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Donut Pie Canvas
        Box(
            modifier = Modifier
                .size(120.dp)
                .weight(1.1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val outerRadius = size.minDimension / 2f
                val strokeWidth = 24.dp.toPx()
                val radius = outerRadius - (strokeWidth / 2f)

                var startAngle = -90f
                distribution.forEach { tagShare ->
                    val sweepAngle = (tagShare.percentage / 100f) * 360f
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = tagShare.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                        startAngle += sweepAngle
                    }
                }
            }
            Text(
                text = "${distribution.size}\nEtiket",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. Legends & share text list
        Column(
            modifier = Modifier.weight(1.9f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            distribution.take(5).forEach { tagShare ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(tagShare.color)
                        )
                        Text(
                            text = tagShare.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${String.format(Locale.US, "%.1f", tagShare.percentage)}% (${tagShare.count})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (distribution.size > 5) {
                Text(
                    text = "+ ${distribution.size - 5} diğer kategori",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}
