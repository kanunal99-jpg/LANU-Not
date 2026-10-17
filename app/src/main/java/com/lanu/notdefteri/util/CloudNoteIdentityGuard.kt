package com.lanu.notdefteri.util

/**
 * Remote note paths are numeric primary keys. A payload whose embedded ID
 * disagrees with its Firestore document ID must never be restored or synced:
 * otherwise one cloud record can overwrite a completely different local note.
 *
 * A missing embedded ID remains valid for legacy numeric document paths.
 */
object CloudNoteIdentityGuard {
    fun validatedId(documentId: String, embeddedId: Any?): Int {
        val id = documentId.toIntOrNull()
            ?.takeIf { it > 0 && it.toString() == documentId }
            ?: throw IllegalStateException(
                "Bulut not belgesinin kimliği geçersiz: $documentId. İşlem durduruldu."
            )
        if (embeddedId != null) {
            val n = embeddedId as? Number
            if (n == null || n.toLong() != id.toLong() || n.toDouble() != id.toDouble()) {
                throw IllegalStateException(
                    "Bulut not kimliği belge yolu ile uyuşmuyor: $documentId. İşlem durduruldu."
                )
            }
        }
        return id
    }

    fun validatedCreatedAt(raw: Any?): Long {
        val number = raw as? Number
        val createdAt = number?.toLong()
        if (createdAt == null || createdAt <= 0L ||
            number.toDouble() != createdAt.toDouble()) {
            throw IllegalStateException(
                "Bulut notunun oluşturulma tarihi geçersiz. Not değiştirilmedi."
            )
        }
        return createdAt
    }
}
