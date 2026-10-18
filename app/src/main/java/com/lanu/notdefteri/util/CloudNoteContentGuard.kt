package com.lanu.notdefteri.util

/**
 * Firestore snapshots must never turn a malformed or missing note field into
 * an empty value and then overwrite a real local note. Legitimately empty
 * string fields remain supported.
 */
object CloudNoteContentGuard {
    fun requiredText(data: Map<String, Any?>, field: String): String =
        data[field] as? String ?: throw IllegalStateException(
            "Bulut notu alanı eksik veya geçersiz: $field"
        )

    fun optionalText(data: Map<String, Any?>, field: String): String? =
        when (val value = data[field]) {
            null -> null // legacy documents can omit optional fields
            is String -> value.ifBlank { null }
            else -> throw IllegalStateException("Geçersiz bulut notu metin alanı: $field")
        }

    fun boolean(data: Map<String, Any?>, field: String): Boolean =
        when (val value = data[field]) {
            null -> false // legacy documents can omit optional flags
            is Boolean -> value
            else -> throw IllegalStateException("Geçersiz bulut notu bayrağı: $field")
        }
}
