package com.lanu.notdefteri.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

data class TtsState(
    val isSpeaking: Boolean = false,
    val isReady: Boolean = false,
    val languageCode: String = "tr-TR",
    val speed: Float = 1.0f,
    val error: String? = null
)

class TextToSpeechHelper(private val context: Context) : TextToSpeech.OnInitListener {

    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            e.printStackTrace()
            _state.update { it.copy(error = "Metin okuma motoru başlatılamadı: ${e.localizedMessage}") }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _state.update { it.copy(isSpeaking = true, error = null) }
                }

                override fun onDone(utteranceId: String?) {
                    _state.update { it.copy(isSpeaking = false) }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _state.update { it.copy(isSpeaking = false, error = "Okuma sırasında hata oluştu.") }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _state.update { it.copy(isSpeaking = false, error = "Okuma hatası ($errorCode).") }
                }
            })

            // Set default locale (Turkish)
            val trLocale = Locale("tr", "TR")
            val result = tts?.setLanguage(trLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default
                tts?.setLanguage(Locale.getDefault())
            }

            _state.update { it.copy(isReady = true) }
        } else {
            isInitialized = false
            _state.update { it.copy(isReady = false, error = "TTS servisi başlatılamadı.") }
        }
    }

    fun speak(text: String, languageCode: String = "tr-TR", speed: Float = 1.0f) {
        if (text.isBlank()) return

        if (!isInitialized || tts == null) {
            _state.update { it.copy(error = "Seslendirme motoru henüz hazır değil, lütfen bekleyin.") }
            return
        }

        try {
            val locale = when (languageCode) {
                "en-US", "en" -> Locale.US
                "de-DE", "de" -> Locale.GERMANY
                "fr-FR", "fr" -> Locale.FRANCE
                else -> Locale("tr", "TR")
            }

            val langResult = tts?.setLanguage(locale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Try fallback to device default
                tts?.setLanguage(Locale.getDefault())
            }

            tts?.setSpeechRate(speed)
            tts?.setPitch(1.0f)

            _state.update { it.copy(languageCode = languageCode, speed = speed, error = null) }

            val maxChunkLength = TextToSpeech.getMaxSpeechInputLength().coerceAtMost(3500)
            if (text.length <= maxChunkLength) {
                val params = Bundle()
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "lanu_tts_${System.currentTimeMillis()}")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "lanu_tts_0")
            } else {
                // Split into manageable chunks
                val chunks = text.chunked(maxChunkLength)
                chunks.forEachIndexed { index, chunk ->
                    val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                    val params = Bundle()
                    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "lanu_tts_${System.currentTimeMillis()}_$index")
                    tts?.speak(chunk, queueMode, params, "lanu_tts_$index")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _state.update { it.copy(isSpeaking = false, error = "Seslendirme hatası: ${e.localizedMessage}") }
        }
    }

    fun setSpeed(speed: Float) {
        _state.update { it.copy(speed = speed) }
        tts?.setSpeechRate(speed)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _state.update { it.copy(isSpeaking = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tts = null
        isInitialized = false
    }
}
