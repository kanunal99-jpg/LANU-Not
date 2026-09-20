package com.lanu.notdefteri.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanu.notdefteri.util.AudioPlayerHelper
import com.lanu.notdefteri.util.AudioPlayerState
import java.util.Locale

@Composable
fun AudioPlayerCard(
    audioPath: String,
    title: String = "Ses Kaydı",
    modifier: Modifier = Modifier,
    onDeleteAudio: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val playerHelper = remember { AudioPlayerHelper(context) }
    val playerState by playerHelper.state.collectAsState()

    DisposableEffect(audioPath) {
        playerHelper.loadAudio(audioPath)
        onDispose {
            playerHelper.release()
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_player_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Title + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Ses Kaydı",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                if (onDeleteAudio != null) {
                    IconButton(
                        onClick = onDeleteAudio,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("delete_audio_note_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Ses Kaydını Sil",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Waveform & Time Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WaveformIndicator(
                    isListening = playerState.isPlaying,
                    rmsDb = if (playerState.isPlaying) 0.6f else 0.1f,
                    modifier = Modifier.width(100.dp).height(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "${formatTime(playerState.currentPositionMs)} / ${formatTime(playerState.durationMs)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("audio_player_timer")
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timeline Scrubber Slider
            Slider(
                value = playerState.currentPositionMs.toFloat(),
                onValueChange = { newPos ->
                    playerHelper.seekTo(newPos.toInt())
                },
                valueRange = 0f..(playerState.durationMs.coerceAtLeast(100).toFloat()),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .testTag("audio_player_slider")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Controls Row: Rewind 10s, Play/Pause, Forward 10s
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { playerHelper.seekBackward(10_000) },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("audio_player_rewind_10s")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "10sn Geri",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                FilledIconButton(
                    onClick = { playerHelper.togglePlayPause() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("audio_player_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Duraklat" else "Oynat",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = { playerHelper.seekForward(10_000) },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("audio_player_forward_10s")
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "10sn İleri",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Playback Options: Speed Chips + Skip Silence Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed Selector Chips (0.75x, 1.0x, 1.25x, 1.5x, 2.0x)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speeds.forEach { speed ->
                        val isSelected = playerState.playbackSpeed == speed
                        FilterChip(
                            selected = isSelected,
                            onClick = { playerHelper.setPlaybackSpeed(speed) },
                            label = {
                                Text(
                                    text = "${speed}x",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("speed_chip_${speed}x")
                        )
                    }
                }

                // Skip Silence Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("skip_silence_container")
                ) {
                    Text(
                        text = "Sessizliği Atla",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (playerState.isSkipSilenceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = playerState.isSkipSilenceEnabled,
                        onCheckedChange = { enabled ->
                            playerHelper.setSkipSilenceEnabled(enabled)
                        },
                        modifier = Modifier
                            .scale(0.75f)
                            .testTag("skip_silence_switch")
                    )
                }
            }

            if (!playerState.error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = playerState.error ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatTime(timeMs: Int): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
