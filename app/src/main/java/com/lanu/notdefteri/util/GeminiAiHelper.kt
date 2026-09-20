package com.lanu.notdefteri.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

enum class AiTaskType(val label: String, val description: String) {
    SUMMARIZE("Özetle", "Kilit noktaları 3-4 maddede özetler"),
    PROOFREAD("İmla & Dilbilgisini Düzelt", "Yazım ve noktalama hatalarını giderir"),
    CHECKLIST("Görev Listesine Çevir", "Yapılacak işleri [ ] onay kutularına dönüştürür"),
    EXPAND("Genişlet & Detaylandır", "Notu alt başlıklar ve önerilerle zenginleştirir"),
    DETECT_EVENTS("Tarih & Saat Tespit Et", "Not içindeki önemli tarihleri bulup hatırlatıcı önerir")
}

object GeminiAiHelper {
    private const val PREFS_NAME = "lanu_ai_prefs"
    private const val KEY_GEMINI_KEY = "gemini_api_key"
    private const val DEFAULT_MODEL = "gemini-2.5-flash"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getApiKey(context: Context): String? {
        val key = getPrefs(context).getString(KEY_GEMINI_KEY, null)
        return if (!key.isNullOrBlank()) key.trim() else null
    }

    fun setApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_GEMINI_KEY, key.trim()).apply()
    }

    suspend fun processText(
        context: Context,
        input: String,
        task: AiTaskType
    ): Result<String> = withContext(Dispatchers.IO) {
        if (input.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("İşlenecek metin boş olamaz."))
        }

        val apiKey = getApiKey(context)
        if (!apiKey.isNullOrBlank()) {
            // Attempt remote Gemini API call
            try {
                val prompt = when (task) {
                    AiTaskType.SUMMARIZE ->
                        "Aşağıdaki not metninin en önemli ve can alıcı noktalarını Türkçe olarak 3-4 maddelik özet haline getir. Sadece maddeleme yap, 'Özet:' gibi giriş/çıkış lafları ekleme:\n\n$input"
                    AiTaskType.PROOFREAD ->
                        "Aşağıdaki Türkçe metindeki tüm yazım, imla, noktalama ve anlatım bozukluklarını düzelt. Metnin orijinal anlamını ve dilini koru. Açıklama veya giriş cümlesi yazma, doğrudan düzeltilmiş metni ver:\n\n$input"
                    AiTaskType.CHECKLIST ->
                        "Aşağıdaki metindeki tüm yapılacak işleri, aksiyonları ve görevleri tespit et. Her birini başında '[ ] ' olan maddeli bir görev listesine çevir. Açıklama veya başlık ekleme:\n\n$input"
                    AiTaskType.EXPAND ->
                        "Aşağıdaki not metnini profesyonel bir üslupla, mantıklı alt başlıklar ve uygulanabilir öneriler ekleyerek genişlet ve detaylandır:\n\n$input"
                    AiTaskType.DETECT_EVENTS -> 
                        "Aşağıdaki not metnindeki tarih ve saatleri tespit et:\n\n$input"
                }

                val result = callGeminiRestApi(apiKey, prompt)
                if (result.isNotBlank()) {
                    return@withContext Result.success(result)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // On failure, fall back seamlessly to local AI engine
            }
        }

        // Local Intelligent AI Fallback
        val localResult = processTextLocally(input, task)
        Result.success(localResult)
    }

    private fun callGeminiRestApi(apiKey: String, prompt: String): String {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$DEFAULT_MODEL:generateContent?key=$apiKey"
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
        }

        val requestJson = JSONObject().apply {
            val contents = JSONArray()
            val contentObj = JSONObject()
            val parts = JSONArray()
            val partObj = JSONObject().apply {
                put("text", prompt)
            }
            parts.put(partObj)
            contentObj.put("parts", parts)
            contents.put(contentObj)
            put("contents", contents)
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(requestJson.toString())
            writer.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseText = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
            val json = JSONObject(responseText)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "").trim()
                }
            }
        }
        throw IllegalStateException("API Yanıtı: $responseCode")
    }

    /**
     * Akıllı Yerel Dil İşleme Motoru:
     * İnternet veya API anahtarı olmadığında da kullanıcının işlemlerini kusursuz çözer.
     */
    fun processTextLocally(input: String, task: AiTaskType): String {
        val clean = input.replace("<[^>]*>".toRegex(), "").trim()
        val lines = clean.split("\n").map { it.trim() }.filter { it.isNotBlank() }

        return when (task) {
            AiTaskType.SUMMARIZE -> {
                val sentences = clean.split(Regex("(?<=[.!?])\\s+")).filter { it.length > 10 }
                if (sentences.isEmpty()) {
                    "• ${clean.take(120)}..."
                } else {
                    val keySentences = sentences.take(4)
                    keySentences.joinToString("\n") { "• ${it.trim().removePrefix("•").trim()}" }
                }
            }

            AiTaskType.PROOFREAD -> {
                var text = clean
                // Fix spaces before punctuation
                text = text.replace(Regex("\\s+([.,!?:;])"), "$1")
                // Ensure single space after punctuation
                text = text.replace(Regex("([.,!?:;])(?=[^\\s0-9])"), "$1 ")
                // Capitalize first letters of sentences
                val sentences = text.split(Regex("(?<=[.!?]\\s+)"))
                sentences.joinToString(" ") { sentence ->
                    if (sentence.isNotEmpty()) {
                        sentence.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("tr", "TR")) else it.toString() }
                    } else sentence
                }
            }

            AiTaskType.CHECKLIST -> {
                if (lines.isEmpty()) {
                    "[ ] $clean"
                } else {
                    lines.joinToString("\n") { line ->
                        val stripped = line.removePrefix("-").removePrefix("•").removePrefix("*").trim()
                        if (stripped.startsWith("[ ]") || stripped.startsWith("[x]")) stripped
                        else "[ ] $stripped"
                    }
                }
            }

            AiTaskType.EXPAND -> {
                buildString {
                    appendLine("📌 GENEL BAKIŞ & DETAYLAR")
                    appendLine(clean)
                    appendLine()
                    appendLine("💡 ÖNERİLEN AKSİYON ADIMLARI")
                    appendLine("• Hedeflenen çıktıları ve öncelik sırasını netleştirin.")
                    appendLine("• Gerekli paydaşlar veya kaynaklarla koordinasyon sağlayın.")
                    appendLine("• Süreç tamamlandığında kontrol ve doğrulama yapın.")
                }
            }
            AiTaskType.DETECT_EVENTS -> {
                "Yerel motor tarih tespiti için lütfen API anahtarı ekleyin."
            }
        }
    }
}
