package com.example.letsgetweddi.model

import com.google.firebase.database.DataSnapshot

data class Supplier(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val location: String? = null,
    val imageUrl: String? = null,
    val phone: String? = null,
    val category: String? = null
) {
    companion object {
        fun fromSnapshot(s: DataSnapshot): Supplier {
            val id = s.child("id").getValue(String::class.java) ?: s.key

            val name = firstNonNullString(
                s, listOf("name", "title", "supplierName", "fullName")
            )

            val description = firstNonNullString(
                s, listOf("description", "details", "about")
            )

            // location: NEVER call getValue(String::class.java) here; it may be a Map
            val location = readLocationFlexible(s)

            // image: prefer coverImage, then common image fields
            val imageUrl = firstNonNullString(
                s,
                listOf(
                    "coverImage",
                    "imageUrl",
                    "image",
                    "photoUrl",
                    "pictureUrl",
                    "avatar",
                    "cover"
                )
            )

            val phone = firstNonNullString(
                s, listOf("phone", "phoneNumber", "tel")
            )

            val category = firstNonNullString(
                s, listOf("category", "type", "categoryId", "categoryName")
            )

            return Supplier(
                id = id,
                name = name,
                description = description,
                location = location,
                imageUrl = imageUrl,
                phone = phone,
                category = category
            )
        }

        // ---------- helpers ----------

        private fun firstNonNullString(s: DataSnapshot, keys: List<String>): String? {
            for (k in keys) {
                val anyVal = s.child(k).value
                val asStr = coerceToString(anyVal)
                if (!asStr.isNullOrBlank()) return asStr
            }
            return null
        }

        private fun readLocationFlexible(s: DataSnapshot): String? {
            // Try multiple nodes that might hold location-ish data
            val locAny =
                s.child("location").value
                    ?: s.child("locationDetail").value
                    ?: s.child("address").value

            when (locAny) {
                is String -> return locAny
                is Map<*, *> -> {
                    // common fields inside a location object
                    val city = coerceToString(locAny["city"])
                    val area = coerceToString(locAny["area"])
                    val region = coerceToString(locAny["region"])
                    val name = coerceToString(locAny["name"]) // fallback if exists
                    val parts = listOfNotNull(
                        city?.takeIf { it.isNotBlank() },
                        area?.takeIf { it.isNotBlank() },
                        region?.takeIf { it.isNotBlank() },
                        name?.takeIf { it.isNotBlank() }
                    )
                    if (parts.isNotEmpty()) return parts.joinToString(", ")
                    // As a very last resort: stringify the whole map (not ideal, but safe)
                    return locAny.entries.joinToString(", ") { "${it.key}:${it.value}" }
                }

                is List<*> -> {
                    val joined = locAny.joinToString(",") { it?.toString().orEmpty() }.trim(',')
                    if (joined.isNotBlank()) return joined
                }
            }

            // Dedicated flat fields as fallback
            val city = firstNonNullString(s, listOf("city"))
            val area = firstNonNullString(s, listOf("area", "region"))
            val parts =
                listOfNotNull(city?.takeIf { it.isNotBlank() }, area?.takeIf { it.isNotBlank() })
            if (parts.isNotEmpty()) return parts.joinToString(", ")

            return null
        }

        private fun coerceToString(v: Any?): String? = when (v) {
            null -> null
            is String -> v
            is Number -> v.toString()
            is Boolean -> v.toString()
            is Map<*, *> -> {
                // If someone saved an object where a string is expected, try common fields
                val name = v["name"] ?: v["title"] ?: v["city"] ?: v["area"]
                when (name) {
                    is String -> name
                    is Number, is Boolean -> name.toString()
                    else -> null
                }
            }

            is List<*> -> v.joinToString(",") { it?.toString().orEmpty() }.ifBlank { null }
            else -> v.toString()
        }
    }
}
