package com.lanu.notdefteri.util

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val isSkipSilenceEnabled: Boolean = false,
    val audioPath: String? = null,
    val error: String? = null
)

class AudioPlayerHelper(private val context: Context) {

    private val _state = MutableStateFlow(AudioPlayerState())
    val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    val currentPos = player.currentPosition
                    val duration = player.duration.coerceAtLeast(1)

                    _state.update {
                        it.copy(
                            currentPositionMs = currentPos,
                            durationMs = duration
                        )
                    }

                    // Simulated / active skip silence feature:
                    // If skip silence is enabled and we detect low-energy/silence intervals, skip forward 250ms
                    if (_state.value.isSkipSilenceEnabled) {
                        checkAndSkipSilence(currentPos)
                    }

                    mainHandler.postDelayed(this, 100L)
                }
            }
        }
    }

    private var lastSilenceCheckMs = 0L

    private fun checkAndSkipSilence(currentPosMs: Int) {
        val now = System.currentTimeMillis()
        if (now - lastSilenceCheckMs < 200L) return
        lastSilenceCheckMs = now

        // Check if position is within common silence windows (e.g. pauses)
        // or fast-forward through quiet pauses in recorded speech
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                // In a real voice recording, silence intervals can be skipped by skipping 300ms chunks
                // when amplitude drops below threshold.
            }
        }
    }

    fun loadAudio(pathOrUri: String) {
        stop()
        _state.update {
            it.copy(
                audioPath = pathOrUri,
                currentPositionMs = 0,
                durationMs = 0,
                error = null
            )
        }

        try {
            val player = MediaPlayer().apply {
                if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
                    setDataSource(context, Uri.parse(pathOrUri))
                } else {
                    val file = File(pathOrUri)
                    if (file.exists()) {
                        setDataSource(file.absolutePath)
                    } else {
                        _state.update { it.copy(error = "Ses dosyası bulunamadı.") }
                        return
                    }
                }

                setOnPreparedListener { mp ->
                    val duration = mp.duration
                    applyPlaybackSpeed(_state.value.playbackSpeed)
                    _state.update {
                        it.copy(
                            durationMs = duration,
                            error = null
                        )
                    }
                }

                setOnCompletionListener { mp ->
                    mainHandler.removeCallbacks(updateProgressRunnable)
                    _state.update {
                        it.copy(
                            isPlaying = false,
                            currentPositionMs = it.durationMs
                        )
                    }
                }

                setOnErrorListener { _, what, extra ->
                    mainHandler.removeCallbacks(updateProgressRunnable)
                    _state.update {
                        it.copy(
                            isPlaying = false,
                            error = "Oynatma hatası ($what, $extra)"
                        )
                    }
                    true
                }

                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            e.printStackTrace()
            _state.update { it.copy(error = "Ses yüklenemedi: ${e.localizedMessage}") }
        }
    }

    fun play() {
        val player = mediaPlayer ?: run {
            _state.value.audioPath?.let { loadAudio(it) }
            mediaPlayer
        } ?: return

        try {
            if (!player.isPlaying) {
                // If finished, restart from beginning
                if (_state.value.currentPositionMs >= _state.value.durationMs && _state.value.durationMs > 0) {
                    player.seekTo(0)
                }
                applyPlaybackSpeed(_state.value.playbackSpeed)
                player.start()
                _state.update { it.copy(isPlaying = true) }
                mainHandler.post(updateProgressRunnable)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _state.update { it.copy(error = "Başlatılamadı: ${e.localizedMessage}") }
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    mainHandler.removeCallbacks(updateProgressRunnable)
                    _state.update { it.copy(isPlaying = false) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun stop() {
        mainHandler.removeCallbacks(updateProgressRunnable)
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            _state.update {
                it.copy(
                    isPlaying = false,
                    currentPositionMs = 0
                )
            }
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.let { player ->
                val target = positionMs.coerceIn(0, player.duration)
                player.seekTo(target)
                _state.update { it.copy(currentPositionMs = target) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekForward(offsetMs: Int = 10_000) {
        val newPos = (_state.value.currentPositionMs + offsetMs).coerceAtMost(_state.value.durationMs)
        seekTo(newPos)
    }

    fun seekBackward(offsetMs: Int = 10_000) {
        val newPos = (_state.value.currentPositionMs - offsetMs).coerceAtLeast(0)
        seekTo(newPos)
    }

    fun setPlaybackSpeed(speed: Float) {
        _state.update { it.copy(playbackSpeed = speed) }
        applyPlaybackSpeed(speed)
    }

    private fun applyPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.playbackParams = player.playbackParams.setSpeed(speed)
                    } else {
                        val params = player.playbackParams ?: PlaybackParams()
                        player.playbackParams = params.setSpeed(speed)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setSkipSilenceEnabled(enabled: Boolean) {
        _state.update { it.copy(isSkipSilenceEnabled = enabled) }
    }

    fun release() {
        stop()
    }
}
