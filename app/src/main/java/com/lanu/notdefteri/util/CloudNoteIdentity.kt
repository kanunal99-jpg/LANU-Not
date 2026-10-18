package com.lanu.notdefteri.util

/**
 * Validate Firestore identity and timestamp before converting cloud data to a
 * Room note. Silent numeric truncation or regenerated timestamps could cause
 * a remote note to replace an unrelated local note.
 *
 * Legacy documents may have non-numeric Firestore IDs as long as their
 * payload contains a valid logical numeric note ID.
 */
object CloudNoteIdentity {
    data class Identity(val id: Int, val createdAt: Long)

    fun validate(documentId: String, rawId: Any?, rawCreatedAt: Any?): Identity {
        val pathId = documentId.toLongOrNull()
        val payloadId = (rawId as? Number)?.toString()?.toLongOrNull()

        if (rawId != null && payloadId == null) {
            throw IllegalStateException("Bulut not kimliği geçersiz: $documentId")
        }

        val id = payloadId ?: pathId
            ?: throw IllegalStateException("Bulut not kimliği eksik: $documentId")

        if (id !in 1L..Int.MAX_VALUE.toLong()) {
            throw IllegalStateException("Bulut not kimliği desteklenen aralık dışında: $id")
        }

        if (pathId != null && pathId != id) {
            throw IllegalStateException(
                "Bulut belge kimliği ($pathId) ile not kimliği ($id) uyuşmuyor."
            )
        }

        val createdAt = (rawCreatedAt as? Number)?.toString()?.toLongOrNull()
            ?: throw IllegalStateException("Bulut notunun oluşturulma tarihi geçersiz: $id")

        if (createdAt < 0L) {
            throw IllegalStateException("Bulut notunun oluşturulma tarihi negatif: $id")
        }

        return Identity(id.toInt(), createdAt)
    }
}
