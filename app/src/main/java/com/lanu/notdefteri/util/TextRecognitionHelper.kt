package com.lanu.notdefteri.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

object TextRecognitionHelper {

    /**
     * Extracts text from an image path or content Uri using Google ML Kit Text Recognition.
     */
    fun extractTextFromImage(
        context: Context,
        imagePathOrUri: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val inputImage = if (imagePathOrUri.startsWith("content://") || imagePathOrUri.startsWith("file://")) {
                InputImage.fromFilePath(context, Uri.parse(imagePathOrUri))
            } else {
                val file = File(imagePathOrUri)
                if (!file.exists()) {
                    onError("Görsel dosyası bulunamadı.")
                    return
                }
                InputImage.fromFilePath(context, Uri.fromFile(file))
            }

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val blocksText = visionText.textBlocks.joinToString("\n\n") { block ->
                        block.lines.joinToString("\n") { line -> line.text }
                    }
                    val recognizedText = if (blocksText.isNotBlank()) blocksText else visionText.text
                    if (recognizedText.isNotBlank()) {
                        onSuccess(recognizedText)
                    } else {
                        onError("Görselde tespit edilebilen okunabilir metin veya el yazısı bulunamadı.")
                    }
                }
                .addOnFailureListener { e ->
                    onError("Metin tanıma hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}")
                }
        } catch (e: Exception) {
            e.printStackTrace()
            onError("Görsel işlenemedi: ${e.localizedMessage}")
        }
    }
}
