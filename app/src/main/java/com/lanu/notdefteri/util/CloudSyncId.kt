package com.lanu.notdefteri.util

import java.security.SecureRandom

/**
 * New cross-device vault identifiers need more entropy than a truncated
 * eight-character UUID. This only changes ID creation for fresh installs:
 * saved legacy IDs remain valid and are never automatically rotated.
 *
 * A strong ID is NOT a replacement for Firebase authentication and Rules.
 */
object CloudSyncId {
    private val rng = SecureRandom()
    private const val HEX = "0123456789ABCDEF"

    /**
     * Accept existing human-entered vault IDs without changing legacy IDs.
     * Prevent Firestore document path separators, reserved names and overly
     * long input from being stored as the active sync ID.
     */
    fun normalizeManual(raw: String): String? {
        val normalized = raw.trim().uppercase(java.util.Locale.ROOT)
        if (normalized.isEmpty() || normalized.length > 128) return null
        if (!normalized.all { it in 'A'..'Z' || it in '0'..'9' || it == '-' || it == '_' }) return null
        if (normalized.startsWith("__") && normalized.endsWith("__")) return null
        return normalized
    }

    fun generate(): String {
        val bytes = ByteArray(12) // 96 bits from a cryptographic RNG.
        rng.nextBytes(bytes)
        return buildString(29) {
            append("LANU-")
            bytes.forEach { byte ->
                val unsigned = byte.toInt() and 0xFF
                append(HEX[unsigned ushr 4])
                append(HEX[unsigned and 0x0F])
            }
        }
    }
}
