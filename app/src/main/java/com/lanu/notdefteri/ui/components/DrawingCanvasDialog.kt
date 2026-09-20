package com.lanu.notdefteri.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream

data class DrawnPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false,
    val alpha: Float = 1.0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvasDialog(
    initialImagePath: String? = null,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val paths = remember { mutableStateListOf<DrawnPath>() }
    var currentPoints = remember { mutableStateListOf<Offset>() }
    var selectedColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }
    var selectedWidth by remember { mutableFloatStateOf(6f) }
    var isEraser by remember { mutableStateOf(false) }
    var isHighlighter by remember { mutableStateOf(false) }

    val colors = listOf(
        Color(0xFF1E1E1E), // Black
        Color(0xFF1976D2), // Blue
        Color(0xFFD32F2F), // Red
        Color(0xFF388E3C), // Green
        Color(0xFFF57C00), // Orange
        Color(0xFF7B1FA2), // Purple
        Color(0xFFFBC02D)  // Yellow
    )

    fun saveDrawingToFile(canvasWidth: Int, canvasHeight: Int) {
        try {
            val width = if (canvasWidth > 0) canvasWidth else 1080
            val height = if (canvasHeight > 0) canvasHeight else 1080
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val androidCanvas = AndroidCanvas(bitmap)
            androidCanvas.drawColor(android.graphics.Color.WHITE)

            val paint = AndroidPaint().apply {
                isAntiAlias = true
                style = AndroidPaint.Style.STROKE
                strokeCap = AndroidPaint.Cap.ROUND
                strokeJoin = AndroidPaint.Join.ROUND
            }

            for (drawn in paths) {
                if (drawn.points.size < 2) continue
                if (drawn.isEraser) {
                    paint.color = android.graphics.Color.WHITE
                    paint.alpha = 255
                } else {
                    paint.color = drawn.color.toArgb()
                    paint.alpha = (drawn.alpha * 255).toInt()
                }
                paint.strokeWidth = drawn.strokeWidth

                val androidPath = android.graphics.Path()
                androidPath.moveTo(drawn.points[0].x, drawn.points[0].y)
                for (i in 1 until drawn.points.size) {
                    androidPath.lineTo(drawn.points[i].x, drawn.points[i].y)
                }
                androidCanvas.drawPath(androidPath, paint)
            }

            val dir = File(context.filesDir, "drawings").apply { mkdirs() }
            val file = File(dir, "draw_${System.currentTimeMillis()}.png")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
            fos.flush()
            fos.close()

            onSave(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("El Yazısı & Çizim") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (paths.isNotEmpty()) {
                                    paths.removeAt(paths.size - 1)
                                }
                            },
                            enabled = paths.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Geri Al")
                        }
                        IconButton(
                            onClick = { paths.clear() },
                            enabled = paths.isNotEmpty()
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Temizle")
                        }
                        Button(
                            onClick = {
                                saveDrawingToFile(1080, 1400)
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kaydet")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Color Palette Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (color in colors) {
                                val isSelected = !isEraser && selectedColor == color
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedColor = color
                                            isEraser = false
                                        }
                                )
                            }
                        }

                        HorizontalDivider()

                        // Tools & Width Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pen tool
                            FilterChip(
                                selected = !isEraser && !isHighlighter,
                                onClick = {
                                    isEraser = false
                                    isHighlighter = false
                                    selectedWidth = 6f
                                },
                                label = { Text("Kalem") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )

                            // Highlighter tool
                            FilterChip(
                                selected = !isEraser && isHighlighter,
                                onClick = {
                                    isEraser = false
                                    isHighlighter = true
                                    selectedWidth = 24f
                                    if (selectedColor == Color(0xFF1E1E1E)) {
                                        selectedColor = Color(0xFFFBC02D)
                                    }
                                },
                                label = { Text("Vurgulayıcı") },
                                leadingIcon = {
                                    Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )

                            // Eraser tool
                            FilterChip(
                                selected = isEraser,
                                onClick = {
                                    isEraser = true
                                    isHighlighter = false
                                    selectedWidth = 32f
                                },
                                label = { Text("Silgi") },
                                leadingIcon = {
                                    Icon(Icons.Default.AutoFixNormal, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.White)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isEraser, selectedColor, selectedWidth, isHighlighter) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints = mutableStateListOf(offset)
                                },
                                onDrag = { change, _ ->
                                    currentPoints.add(change.position)
                                },
                                onDragEnd = {
                                    if (currentPoints.isNotEmpty()) {
                                        paths.add(
                                            DrawnPath(
                                                points = currentPoints.toList(),
                                                color = selectedColor,
                                                strokeWidth = selectedWidth,
                                                isEraser = isEraser,
                                                alpha = if (isHighlighter) 0.45f else 1.0f
                                            )
                                        )
                                        currentPoints.clear()
                                    }
                                }
                            )
                        }
                ) {
                    // Draw existing paths
                    for (drawn in paths) {
                        if (drawn.points.size < 2) continue
                        val path = Path().apply {
                            moveTo(drawn.points[0].x, drawn.points[0].y)
                            for (i in 1 until drawn.points.size) {
                                lineTo(drawn.points[i].x, drawn.points[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = if (drawn.isEraser) Color.White else drawn.color.copy(alpha = drawn.alpha),
                            style = Stroke(
                                width = drawn.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // Draw current active dragging path
                    if (currentPoints.size >= 2) {
                        val path = Path().apply {
                            moveTo(currentPoints[0].x, currentPoints[0].y)
                            for (i in 1 until currentPoints.size) {
                                lineTo(currentPoints[i].x, currentPoints[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = if (isEraser) Color.White else selectedColor.copy(alpha = if (isHighlighter) 0.45f else 1.0f),
                            style = Stroke(
                                width = selectedWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }
    }
}
