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
            // id: prefer explicit "id", fallback to snapshot.key
            val id = (s.child("id").getValue(String::class.java) ?: s.key)

            // name: try several common keys
            val name = firstNonNull(
                s, listOf("name", "title", "supplierName", "fullName")
            )

            // description / details
            val description = firstNonNull(
                s, listOf("description", "details", "about")
            )

            // location
            val location = firstNonNull(
                s, listOf("location", "city", "area", "region")
            )

            // image (url or storage path)
            val imageUrl = firstNonNull(
                s, listOf("imageUrl", "image", "photoUrl", "pictureUrl", "avatar", "cover")
            )

            // phone
            val phone = firstNonNull(
                s, listOf("phone", "phoneNumber", "tel")
            )

            // category (id or title)
            val category = firstNonNull(
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

        private fun firstNonNull(s: DataSnapshot, keys: List<String>): String? {
            for (k in keys) {
                val v = s.child(k).getValue(String::class.java)
                if (!v.isNullOrBlank()) return v
            }
            return null
        }
    }
}
