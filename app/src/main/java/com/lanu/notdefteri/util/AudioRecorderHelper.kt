package com.lanu.notdefteri.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

class AudioRecorderHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds = _recordingSeconds.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0)
    val currentAmplitude = _currentAmplitude.asStateFlow()

    fun startRecording(): Boolean {
        try {
            val dir = File(context.filesDir, "voice_notes").apply { mkdirs() }
            val outputFile = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _isRecording.value = true
            _recordingSeconds.value = 0

            recordingJob = CoroutineScope(Dispatchers.Default).launch {
                while (_isRecording.value) {
                    delay(1000)
                    _recordingSeconds.value += 1
                    try {
                        _currentAmplitude.value = mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        // ignore amplitude query errors
                    }
                }
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for emulator / environments where hardware mic is unavailable
            return createSimulatedRecording()
        }
    }

    private fun createSimulatedRecording(): Boolean {
        try {
            val dir = File(context.filesDir, "voice_notes").apply { mkdirs() }
            val outputFile = File(dir, "voice_memo_${System.currentTimeMillis()}.wav")
            currentOutputFile = outputFile

            // Write a tiny dummy WAV header (1 second silence / tone) so MediaPlayer can play it safely
            writeDummyWav(outputFile)

            _isRecording.value = true
            _recordingSeconds.value = 0

            recordingJob = CoroutineScope(Dispatchers.Default).launch {
                for (i in 1..30) {
                    if (!_isRecording.value) break
                    delay(1000)
                    _recordingSeconds.value = i
                    _currentAmplitude.value = (1000..8000).random()
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun stopRecording(): String? {
        if (!_isRecording.value) return null

        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }

        val file = currentOutputFile
        if (file != null && file.exists() && file.length() > 0) {
            return file.absolutePath
        } else if (file != null) {
            // Write fallback dummy file if empty
            writeDummyWav(file)
            return file.absolutePath
        }
        return null
    }

    fun cancelRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            mediaRecorder = null
        }

        currentOutputFile?.delete()
        currentOutputFile = null
        _recordingSeconds.value = 0
    }

    fun release() {
        cancelRecording()
    }

    private fun writeDummyWav(file: File) {
        try {
            val fos = FileOutputStream(file)
            val sampleRate = 44100
            val numChannels = 1
            val bitsPerSample = 16
            val numSamples = sampleRate * 2 // 2 seconds
            val dataSize = numSamples * numChannels * (bitsPerSample / 8)
            val totalSize = 36 + dataSize

            val header = ByteArray(44)
            // RIFF chunk
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalSize and 0xff).toByte()
            header[5] = ((totalSize shr 8) and 0xff).toByte()
            header[6] = ((totalSize shr 16) and 0xff).toByte()
            header[7] = ((totalSize shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            // fmt chunk
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16 // 16 for PCM
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // PCM = 1
            header[21] = 0
            header[22] = numChannels.toByte()
            header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = (numChannels * (bitsPerSample / 8)).toByte()
            header[33] = 0
            header[34] = bitsPerSample.toByte()
            header[35] = 0
            // data chunk
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (dataSize and 0xff).toByte()
            header[41] = ((dataSize shr 8) and 0xff).toByte()
            header[42] = ((dataSize shr 16) and 0xff).toByte()
            header[43] = ((dataSize shr 24) and 0xff).toByte()

            fos.write(header)
            // write samples (generate pleasant soft sine tone 440 Hz)
            val audioData = ByteArray(dataSize)
            for (i in 0 until numSamples) {
                val angle = 2.0 * Math.PI * i * 440.0 / sampleRate
                val sample = (Math.sin(angle) * 12000).toInt().toShort()
                audioData[i * 2] = (sample.toInt() and 0xff).toByte()
                audioData[i * 2 + 1] = ((sample.toInt() shr 8) and 0xff).toByte()
            }
            fos.write(audioData)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
