package com.lanu.notdefteri.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanu.notdefteri.util.FirestoreSyncState
import com.lanu.notdefteri.util.SyncStatus

@Composable
fun SyncStatusBadge(
    syncState: FirestoreSyncState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val status = syncState.syncStatus

    val containerBg: Color
    val contentColor: Color
    val borderColor: Color
    val labelText: String

    when (status) {
        SyncStatus.SYNCING -> {
            containerBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            labelText = if (isCompact) "Eşitleniyor" else "Eşitleniyor..."
        }
        SyncStatus.SYNCED -> {
            containerBg = Color(0xFFE8F5E9) // Soft Green
            contentColor = Color(0xFF1B5E20) // Dark Green
            borderColor = Color(0xFFA5D6A7)
            labelText = if (isCompact) "Eşitlendi" else "Bulut Eşitlendi"
        }
        SyncStatus.OFFLINE -> {
            containerBg = Color(0xFFFFF3E0) // Soft Amber
            contentColor = Color(0xFFE65100) // Deep Orange/Amber
            borderColor = Color(0xFFFFCC80)
            labelText = if (isCompact) "Çevrimdışı" else "Çevrimdışı (Yerel)"
        }
        SyncStatus.ERROR -> {
            containerBg = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
            contentColor = MaterialTheme.colorScheme.onErrorContainer
            borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            labelText = "Eşitleme Hatası"
        }
    }

    // Rotating animation for SYNCING state
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerBg,
        shadowElevation = 0.dp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("sync_status_badge")
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 8.dp else 10.dp,
                vertical = if (isCompact) 4.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Animated Status Dot or Icon
            AnimatedContent(
                targetState = status,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "sync_icon_transition"
            ) { targetStatus ->
                when (targetStatus) {
                    SyncStatus.SYNCING -> {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Eşitleniyor",
                            tint = contentColor,
                            modifier = Modifier
                                .size(if (isCompact) 14.dp else 16.dp)
                                .rotate(rotationAngle)
                        )
                    }
                    SyncStatus.SYNCED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(contentColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Eşitlendi",
                                tint = contentColor,
                                modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
                            )
                        }
                    }
                    SyncStatus.OFFLINE -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(contentColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Çevrimdışı",
                                tint = contentColor,
                                modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
                            )
                        }
                    }
                    SyncStatus.ERROR -> {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Hata",
                            tint = contentColor,
                            modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    color = contentColor
                )
            )
        }
    }
}
