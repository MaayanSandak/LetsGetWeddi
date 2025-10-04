package com.example.letsgetweddi.model

import com.google.firebase.database.DataSnapshot

data class Review(
    val userId: String? = null,
    val name: String? = null,
    val rating: Float = 0f,
    val comment: String? = null,
    val timestamp: Long = 0L
) {
    companion object {
        fun fromSnapshot(snapshot: DataSnapshot): Review? {
            if (!snapshot.exists()) return null

            val name = firstNonEmptyString(
                snapshot,
                listOf("name", "userName", "displayName", "author", "reviewerName")
            )
            val comment = firstNonEmptyString(
                snapshot,
                listOf("comment", "text", "content", "review", "body")
            )
            val rating = coerceFloat(
                firstExistingValue(snapshot, listOf("rating", "rate", "stars", "score"))
            )
            val timestamp = coerceLong(
                firstExistingValue(snapshot, listOf("timestamp", "time", "date", "createdAt"))
            )
            val userId = firstNonEmptyString(
                snapshot,
                listOf("userId", "uid", "authorId", "userUid")
            )

            return Review(userId, name, rating, comment, timestamp)
        }

        private fun firstNonEmptyString(snap: DataSnapshot, keys: List<String>): String? {
            for (k in keys) {
                val v = snap.child(k).value ?: continue
                val s = when (v) {
                    is String -> v
                    is Number, is Boolean -> v.toString()
                    else -> v.toString()
                }.trim()
                if (s.isNotEmpty()) return s
            }
            return null
        }

        private fun firstExistingValue(snap: DataSnapshot, keys: List<String>): Any? {
            for (k in keys) {
                val n = snap.child(k)
                if (n.exists()) return n.value
            }
            return null
        }

        private fun coerceFloat(any: Any?): Float = when (any) {
            null -> 0f
            is Number -> any.toFloat()
            is String -> any.toFloatOrNull() ?: 0f
            else -> 0f
        }

        private fun coerceLong(any: Any?): Long = when (any) {
            null -> 0L
            is Number -> any.toLong()
            is String -> any.toLongOrNull() ?: 0L
            else -> 0L
        }
    }
}
