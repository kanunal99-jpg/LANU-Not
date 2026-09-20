package com.lanu.notdefteri.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WaveformIndicator(
    isListening: Boolean,
    rmsDb: Float,
    modifier: Modifier = Modifier,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(48.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        // Base amplitude from activity state
        val maxAmplitude = if (isListening) (centerY * 0.8f).coerceAtLeast(10f) else 2f
        // Normalize rmsDb to amplitude multiplier
        val rawAmp = (rmsDb + 2f).coerceAtLeast(0f) / 10f
        val currentAmp = if (isListening) maxAmplitude * rawAmp.coerceAtMost(1.2f) else 2f

        val points = 80
        val step = width / points

        for (i in 0 until points) {
            val x = i * step
            val normalizedX = (i.toFloat() / points) * 2 * Math.PI.toFloat()
            
            // Layered sine waves for an elegant fluid effect
            val yOffset = sin(normalizedX * 3f + phase) * currentAmp * 0.6f +
                    sin(normalizedX * 6f - phase * 1.3f) * currentAmp * 0.3f +
                    sin(normalizedX * 1.5f + phase * 0.5f) * currentAmp * 0.1f
            
            val y = centerY + yOffset
            val nextX = (i + 1) * step
            val nextNormalizedX = ((i + 1).toFloat() / points) * 2 * Math.PI.toFloat()
            val nextYOffset = sin(nextNormalizedX * 3f + phase) * currentAmp * 0.6f +
                    sin(nextNormalizedX * 6f - phase * 1.3f) * currentAmp * 0.3f +
                    sin(nextNormalizedX * 1.5f + phase * 0.5f) * currentAmp * 0.1f
            val nextY = centerY + nextYOffset

            drawLine(
                color = color.copy(alpha = if (isListening) 0.8f else 0.2f),
                start = Offset(x, y),
                end = Offset(nextX, nextY),
                strokeWidth = if (isListening) 4.dp.toPx() else 1.5.dp.toPx()
            )
        }
    }
}
