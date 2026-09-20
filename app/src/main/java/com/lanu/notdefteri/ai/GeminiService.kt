package com.lanu.notdefteri.ai

import com.lanu.notdefteri.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>
)

@Serializable
data class Candidate(
    val content: Content
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

@Serializable
data class DetectedEvent(
    val title: String,
    val date: String, // YYYY-MM-DD
    val time: String? = null // HH:mm
)

@Serializable
data class DetectedEventsResponse(
    val events: List<DetectedEvent>
)

class GeminiHelper {
    private val apiKey = BuildConfig.GEMINI_API_KEY

    suspend fun detectEvents(noteContent: String): List<DetectedEvent> {
        if (apiKey == "YOUR_API_KEY_HERE" || apiKey.isBlank()) return emptyList()

        val prompt = """
            Aşağıdaki not içeriğindeki önemli tarih ve saatleri tespit et. 
            Bunları bir etkinlik listesi olarak döndür.
            Tarihleri YYYY-MM-DD formatında, saatleri HH:mm formatında ver.
            Başlık kısa ve öz olsun.
            Bugünün tarihi: ${java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())}
            
            Not İçeriği:
            $noteContent
        """.trimIndent()

        val responseSchema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("events") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("title") { put("type", "STRING") }
                            putJsonObject("date") { put("type", "STRING") }
                            putJsonObject("time") { put("type", "STRING") }
                        }
                        putJsonArray("required") {
                            add(kotlinx.serialization.json.JsonPrimitive("title"))
                            add(kotlinx.serialization.json.JsonPrimitive("date"))
                        }
                    }
                }
            }
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                // Retrofit serialization of responseSchema might need more care if it's JsonObject
                // But for now let's try the direct approach or use a more manual prompt if schema fails.
            )
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return emptyList()
            val result = Json.decodeFromString<DetectedEventsResponse>(jsonText)
            result.events
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

// Extension to help build json array in buildJsonObject since I used it in prompt
private fun kotlinx.serialization.json.JsonObjectBuilder.putJsonArray(key: String, content: kotlinx.serialization.json.JsonArrayBuilder.() -> Unit) {
    put(key, kotlinx.serialization.json.buildJsonArray(content))
}

private fun kotlinx.serialization.json.JsonArrayBuilder.addJsonObject(content: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) {
    add(kotlinx.serialization.json.buildJsonObject(content))
}
