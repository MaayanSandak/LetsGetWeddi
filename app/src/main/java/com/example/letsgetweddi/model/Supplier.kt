package com.example.letsgetweddi.model

import com.google.firebase.database.DataSnapshot

data class Supplier(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val location: String? = null,
    val imageUrl: String? = null,
    val phone: String? = null,
    val category: String? = null,
    val categoryId: String? = null,
    val categories: List<String>? = null,
    val images: List<String>? = null
) {
    companion object {
        fun fromSnapshot(s: DataSnapshot): Supplier {
            val rawId = s.child("id").getValue(String::class.java)
            val id = if (rawId.isNullOrBlank()) s.key else rawId

            val name = firstNonNullString(s, listOf("name", "title", "supplierName", "fullName"))
            val description = firstNonNullString(s, listOf("description", "details", "about"))

            val location = readLocationFlexible(s)

            var imageUrl = firstNonNullString(
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

            val images = readStringList(s, listOf("images", "gallery", "photos"))
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.distinct()

            if (imageUrl.isNullOrBlank() && !images.isNullOrEmpty()) {
                imageUrl = images.firstOrNull()
            }

            val phone = firstNonNullString(s, listOf("phone", "phoneNumber", "tel"))
            val category = firstNonNullString(
                s,
                listOf("category", "categ", "type", "supplierType", "profession")
            )
            val categoryId = firstNonNullString(s, listOf("categoryId", "category_id"))
            val categories = readStringList(s, listOf("categories", "tags"))
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.distinct()

            return Supplier(
                id = id,
                name = name,
                description = description,
                location = location,
                imageUrl = imageUrl,
                phone = phone,
                category = category,
                categoryId = categoryId,
                categories = categories,
                images = images
            )
        }

        private fun firstNonNullString(s: DataSnapshot, keys: List<String>): String? {
            for (k in keys) {
                val anyVal = s.child(k).value ?: continue
                val str = coerceToString(anyVal)
                if (!str.isNullOrBlank()) return str
            }
            return null
        }

        private fun readStringList(s: DataSnapshot, keys: List<String>): List<String>? {
            for (k in keys) {
                val node = s.child(k)
                if (!node.exists()) continue
                val out = mutableListOf<String>()
                when (val v = node.value) {
                    is List<*> -> v.forEach { if (it != null) out.add(it.toString()) }
                    is Map<*, *> -> {
                        v.forEach { (key, value) ->
                            if (key != null) out.add(key.toString())
                            when (value) {
                                is String -> if (value.isNotBlank()) out.add(value)
                                is Map<*, *> -> value.keys.filterNotNull()
                                    .forEach { out.add(it.toString()) }

                                is List<*> -> value.filterNotNull()
                                    .forEach { out.add(it.toString()) }

                                else -> {}
                            }
                        }
                    }

                    is String -> out.add(v)
                }
                if (out.isNotEmpty()) return out
            }
            return null
        }

        private fun readLocationFlexible(s: DataSnapshot): String? {
            val locAny = s.child("location").value ?: s.child("locationDetail").value
            ?: s.child("address").value
            when (locAny) {
                is String -> return locAny
                is Map<*, *> -> {
                    val city = coerceToString(locAny["city"])
                    val area = coerceToString(locAny["area"])
                    val parts = listOfNotNull(
                        city?.takeIf { it.isNotBlank() },
                        area?.takeIf { it.isNotBlank() })
                    if (parts.isNotEmpty()) return parts.joinToString(", ")
                    return locAny.entries.joinToString(", ") { "${it.key}:${it.value}" }
                }

                is List<*> -> {
                    val joined = locAny.joinToString(",") { it?.toString().orEmpty() }.trim(',')
                    if (joined.isNotBlank()) return joined
                }
            }
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
