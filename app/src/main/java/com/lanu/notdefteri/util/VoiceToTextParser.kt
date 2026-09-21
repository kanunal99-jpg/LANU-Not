package com.lanu.notdefteri.util

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class VoiceCommandAction {
    SAVE,
    CLEAR,
    PIN,
    TITLE,
    TAG,
    NEW_NOTE
}

class VoiceToTextParser(private val app: Application) : RecognitionListener {

    private val _state = MutableStateFlow(VoiceToTextParserState())
    val state: StateFlow<VoiceToTextParserState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var currentLanguage: String = "tr-TR"
    private var isContinuous: Boolean = true
    private var isExplicitlyStopped: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        val available = SpeechRecognizer.isRecognitionAvailable(app)
        _state.update { it.copy(isAvailable = available) }
    }

    fun setContinuousMode(enabled: Boolean) {
        isContinuous = enabled
        _state.update { it.copy(isContinuous = enabled) }
    }

    fun startListening(languageCode: String = "tr-TR", continuous: Boolean = isContinuous) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { startListening(languageCode, continuous) }
            return
        }

        currentLanguage = languageCode
        isContinuous = continuous
        isExplicitlyStopped = false
        mainHandler.removeCallbacksAndMessages(null)

        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            _state.update {
                it.copy(
                    error = "Ses tanıma servisi bu cihazda doğrudan çalıştırılamadı. Sistem sesli girişini kullanabilirsiniz.",
                    isListening = false,
                    isAvailable = false,
                    partialText = ""
                )
            }
            return
        }

        destroyRecognizer()

        _state.update {
            it.copy(
                isListening = true,
                isContinuous = isContinuous,
                error = null,
                partialText = "",
                rmsDb = 0f,
                currentLanguage = languageCode,
                triggeredAction = null
            )
        }

        try {
            recognizer = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(app)
                ) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(app).apply {
                        setRecognitionListener(this@VoiceToTextParser)
                    }
                } else {
                    SpeechRecognizer.createSpeechRecognizer(app).apply {
                        setRecognitionListener(this@VoiceToTextParser)
                    }
                }
            } catch (fallbackEx: Exception) {
                SpeechRecognizer.createSpeechRecognizer(app).apply {
                    setRecognitionListener(this@VoiceToTextParser)
                }
            }

            val intent = createSpeechIntent(languageCode)
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            _state.update {
                it.copy(
                    isListening = false,
                    error = "Ses tanıma başlatılamadı: ${e.localizedMessage ?: "Bilinmeyen hata"}"
                )
            }
        }
    }

    fun stopListening() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { stopListening() }
            return
        }

        isExplicitlyStopped = true
        mainHandler.removeCallbacksAndMessages(null)
        _state.update { it.copy(isListening = false, rmsDb = 0f, partialText = "") }
        try {
            recognizer?.stopListening()
            recognizer?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleListening(languageCode: String = currentLanguage) {
        if (_state.value.isListening) {
            stopListening()
        } else {
            startListening(languageCode)
        }
    }

    fun reset() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { reset() }
            return
        }

        isExplicitlyStopped = true
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
        _state.value = VoiceToTextParserState(
            isAvailable = SpeechRecognizer.isRecognitionAvailable(app),
            currentLanguage = currentLanguage,
            isContinuous = isContinuous
        )
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearSpokenText() {
        _state.update { it.copy(spokenText = "", partialText = "") }
    }

    fun clearAction() {
        _state.update { it.copy(triggeredAction = null) }
    }

    fun destroy() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { destroy() }
            return
        }

        isExplicitlyStopped = true
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
    }

    private fun destroyRecognizer() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recognizer = null
    }

    fun createSpeechIntent(languageCode: String = currentLanguage): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, app.packageName)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
    }

    /**
     * Translates common speech punctuation words into clean punctuation marks
     * for seamless hands-free note dictation.
     */
    fun formatVoiceCommands(text: String, lang: String = currentLanguage): String {
        var formatted = text.trim()
        if (lang.startsWith("tr")) {
            formatted = formatted
                .replace(Regex("(?i)\\b(nokta)\\b"), ".")
                .replace(Regex("(?i)\\b(virgül)\\b"), ",")
                .replace(Regex("(?i)\\b(iki nokta)\\b"), ":")
                .replace(Regex("(?i)\\b(soru işareti)\\b"), "?")
                .replace(Regex("(?i)\\b(ünlem işareti|ünlem)\\b"), "!")
                .replace(Regex("(?i)\\b(yeni satır|alt satır)\\b"), "\n")
                .replace(Regex("(?i)\\b(tire|çizgi)\\b"), "-")
                .replace(Regex("(?i)\\b(parantez aç)\\b"), "(")
                .replace(Regex("(?i)\\b(parantez kapat|parantezi kapat)\\b"), ")")
        } else {
            formatted = formatted
                .replace(Regex("(?i)\\b(period|full stop)\\b"), ".")
                .replace(Regex("(?i)\\b(comma)\\b"), ",")
                .replace(Regex("(?i)\\b(colon)\\b"), ":")
                .replace(Regex("(?i)\\b(question mark)\\b"), "?")
                .replace(Regex("(?i)\\b(exclamation mark|exclamation point)\\b"), "!")
                .replace(Regex("(?i)\\b(new line)\\b"), "\n")
                .replace(Regex("(?i)\\b(hyphen|dash)\\b"), "-")
                .replace(Regex("(?i)\\b(open parenthesis)\\b"), "(")
                .replace(Regex("(?i)\\b(close parenthesis)\\b"), ")")
        }

        // Clean up spaces before punctuation
        return formatted
            .replace(Regex("\\s+([.,:?!])"), "$1")
            .replace(Regex("([.,:?!])([a-zA-ZçÇğĞıİöÖşŞüÜ])"), "$1 $2")
    }

    /**
     * Appends spoken text directly into existing note content with smart capitalization and spacing.
     */
    fun appendTranscription(existingText: String, newSpokenText: String, lang: String = currentLanguage): String {
        val cleanSpoken = formatVoiceCommands(newSpokenText, lang).trim()
        if (cleanSpoken.isEmpty()) return existingText
        if (existingText.isBlank()) {
            return cleanSpoken.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        val trimmedExisting = existingText.trimEnd()
        val endsWithPunctuation = trimmedExisting.endsWith(".") ||
                trimmedExisting.endsWith("?") ||
                trimmedExisting.endsWith("!") ||
                trimmedExisting.endsWith("\n")

        val preparedSpoken = if (endsWithPunctuation) {
            cleanSpoken.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            cleanSpoken
        }

        return if (trimmedExisting.endsWith("\n")) {
            "$trimmedExisting$preparedSpoken"
        } else {
            "$trimmedExisting $preparedSpoken"
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _state.update { it.copy(error = null, isListening = true) }
    }

    override fun onBeginningOfSpeech() {
        _state.update { it.copy(isListening = true) }
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Normalize rmsDb to 0f - 1f range for fluid visual wave animations
        val normalized = ((rmsdB + 2f) / 14f).coerceIn(0f, 1f)
        _state.update { it.copy(rmsDb = normalized) }
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _state.update { it.copy(rmsDb = 0f) }
    }

    override fun onError(error: Int) {
        if (error == SpeechRecognizer.ERROR_CLIENT) {
            _state.update {
                it.copy(
                    isListening = false,
                    partialText = "",
                    rmsDb = 0f,
                    error = "Cihaz veya önizleme ortamında ses servisi yanıt vermedi. Sistem diktesini veya örnek ses girişini deneyebilirsiniz."
                )
            }
            return
        }

        val isTimeoutOrNoMatch = error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                error == SpeechRecognizer.ERROR_NO_MATCH

        // In continuous mode, silent pauses or no match shouldn't disrupt the user with scary red banners;
        // automatically restart listening seamlessly
        if (isContinuous && !isExplicitlyStopped && isTimeoutOrNoMatch) {
            restartListeningWithDelay(300L)
            return
        }

        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Ses kaydı hatası. Mikrofon donanımını kontrol edin."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon izni verilmedi."
            SpeechRecognizer.ERROR_NETWORK -> "İnternet bağlantı hatası."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Bağlantı zaman aşımına uğradı."
            SpeechRecognizer.ERROR_NO_MATCH -> "Ses anlaşılamadı. Lütfen tekrar konuşun."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Ses servisi meşgul, tekrar deneniyor..."
            SpeechRecognizer.ERROR_SERVER -> "Ses tanıma sunucu hatası."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Sessizlik algılandı."
            else -> "Ses tanıma hatası ($error)."
        }

        _state.update {
            it.copy(
                error = errorMessage,
                isListening = false,
                partialText = "",
                rmsDb = 0f
            )
        }

        // For busy error, attempt auto-recovery
        if (isContinuous && !isExplicitlyStopped && error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            restartListeningWithDelay(600L)
        }
    }

    /**
     * Allows previewing and testing voice-to-text dictation in environments like emulators
     * where hardware microphones or speech recognition services are not available.
     */
    fun simulateSpokenText(sampleText: String) {
        if (sampleText.isBlank()) return
        _state.update {
            it.copy(
                error = null,
                partialText = ""
            )
        }
        processRecognizedText(sampleText, isContinuousModeActive = false)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val rawText = matches?.firstOrNull() ?: ""
        processRecognizedText(rawText, isContinuousModeActive = isContinuous && !isExplicitlyStopped)
    }

    private fun processRecognizedText(rawText: String, isContinuousModeActive: Boolean) {
        if (rawText.isNotBlank()) {
            val lower = rawText.trim().lowercase(java.util.Locale.ROOT)

            // Check standalone hands-free voice commands
            val isSave = when {
                currentLanguage.startsWith("tr") -> lower in listOf(
                    "kaydet",
                    "notu kaydet",
                    "notu kaydet lütfen",
                    "kaydet ve bitir",
                    "kaydet lütfen"
                )
                else -> lower in listOf(
                    "save",
                    "save note",
                    "save the note",
                    "save and close",
                    "save please"
                )
            }
            val isClear = when {
                currentLanguage.startsWith("tr") -> lower in listOf(
                    "temizle",
                    "metni temizle",
                    "hepsini sil",
                    "sil ve baştan başla"
                )
                else -> lower in listOf(
                    "clear",
                    "clear text",
                    "delete all",
                    "clear note"
                )
            }
            val isPin = when {
                currentLanguage.startsWith("tr") -> lower in listOf("sabitle", "pinle", "notu sabitle", "notu pinle")
                else -> lower in listOf("pin", "pin note", "pin this note")
            }
            val isNewNote = when {
                currentLanguage.startsWith("tr") -> lower in listOf("yeni not", "not oluştur", "yeni not oluştur", "yeni not ekle")
                else -> lower in listOf("new note", "create note", "add new note")
            }

            var isTitleCommand = false
            var extractedTitle = ""
            if (currentLanguage.startsWith("tr")) {
                if (lower.startsWith("başlık ")) {
                    isTitleCommand = true
                    extractedTitle = rawText.trim().substring(7).trim()
                }
            } else {
                if (lower.startsWith("title ")) {
                    isTitleCommand = true
                    extractedTitle = rawText.trim().substring(6).trim()
                }
            }

            var isTagCommand = false
            var extractedTag = ""
            if (currentLanguage.startsWith("tr")) {
                if (lower.startsWith("etiket ")) {
                    isTagCommand = true
                    extractedTag = rawText.trim().substring(7).trim()
                }
            } else {
                if (lower.startsWith("tag ")) {
                    isTagCommand = true
                    extractedTag = rawText.trim().substring(4).trim()
                }
            }

            if (isSave) {
                _state.update {
                    it.copy(
                        triggeredAction = VoiceCommandAction.SAVE,
                        partialText = "",
                        rmsDb = 0f
                    )
                }
                stopListening()
                return
            } else if (isClear) {
                _state.update {
                    it.copy(
                        triggeredAction = VoiceCommandAction.CLEAR,
                        spokenText = "",
                        partialText = "",
                        rmsDb = 0f
                    )
                }
                if (isContinuousModeActive) {
                    restartListeningWithDelay(250L)
                }
                return
            } else if (isPin) {
                _state.update {
                    it.copy(
                        triggeredAction = VoiceCommandAction.PIN,
                        partialText = "",
                        rmsDb = 0f
                    )
                }
                if (isContinuousModeActive) {
                    restartListeningWithDelay(250L)
                }
                return
            } else if (isNewNote) {
                _state.update {
                    it.copy(
                        triggeredAction = VoiceCommandAction.NEW_NOTE,
                        partialText = "",
                        rmsDb = 0f
                    )
                }
                stopListening()
                return
            } else if (isTitleCommand && extractedTitle.isNotBlank()) {
                val formatted = formatVoiceCommands(extractedTitle, currentLanguage)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                _state.update {
                    it.copy(
                        triggeredAction = VoiceCommandAction.TITLE,
                        commandPayload = formatted,
                        partialText = "",
                        rmsDb = 0f
                    )
                }
                if (isContinuousModeActive) {
                    restartListeningWithDelay(250L)
                }
                return
            } else if (isTagCommand && extractedTag.isNotBlank()) {
                val formatted = formatVoiceCommands(extractedTag, currentLanguage)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                _state.update {
                    it.copy(
                        triggeredAction = VoiceCommandAction.TAG,
                        commandPayload = formatted,
                        partialText = "",
                        rmsDb = 0f
                    )
                }
                if (isContinuousModeActive) {
                    restartListeningWithDelay(250L)
                }
                return
            }

            // Check if user spoke a note ending with "notu kaydet" / "save note"
            var textToProcess = rawText
            var shouldTriggerSave = false
            if (currentLanguage.startsWith("tr")) {
                val savePhrases = listOf(" notu kaydet", " kaydet lütfen", " notu kaydet lütfen")
                for (phrase in savePhrases) {
                    if (textToProcess.endsWith(phrase, ignoreCase = true)) {
                        textToProcess = textToProcess.substring(0, textToProcess.length - phrase.length).trim()
                        shouldTriggerSave = true
                        break
                    }
                }
            } else {
                val savePhrases = listOf(" save note", " save the note", " save please")
                for (phrase in savePhrases) {
                    if (textToProcess.endsWith(phrase, ignoreCase = true)) {
                        textToProcess = textToProcess.substring(0, textToProcess.length - phrase.length).trim()
                        shouldTriggerSave = true
                        break
                    }
                }
            }

            val formattedText = formatVoiceCommands(textToProcess, currentLanguage)
            _state.update {
                it.copy(
                    spokenText = formattedText,
                    partialText = "",
                    rmsDb = 0f,
                    error = null,
                    triggeredAction = if (shouldTriggerSave) VoiceCommandAction.SAVE else null
                )
            }

            if (shouldTriggerSave) {
                stopListening()
                return
            }
        } else {
            _state.update {
                it.copy(
                    partialText = "",
                    rmsDb = 0f
                )
            }
        }

        // In continuous dictation mode, automatically keep listening for the next sentence
        if (isContinuousModeActive) {
            restartListeningWithDelay(250L)
        } else {
            _state.update { it.copy(isListening = false) }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotBlank()) {
            val formatted = formatVoiceCommands(text, currentLanguage)
            _state.update {
                it.copy(partialText = formatted)
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun restartListeningWithDelay(delayMs: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isContinuous && !isExplicitlyStopped) {
                destroyRecognizer()
                startListening(currentLanguage, isContinuous)
            }
        }, delayMs)
    }
}

data class VoiceToTextParserState(
    val isListening: Boolean = false,
    val isContinuous: Boolean = true,
    val partialText: String = "",
    val spokenText: String = "",
    val error: String? = null,
    val rmsDb: Float = 0f,
    val currentLanguage: String = "tr-TR",
    val isAvailable: Boolean = true,
    val triggeredAction: VoiceCommandAction? = null,
    val commandPayload: String? = null
)
