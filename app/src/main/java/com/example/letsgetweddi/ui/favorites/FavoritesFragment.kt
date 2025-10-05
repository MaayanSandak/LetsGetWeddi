package com.example.letsgetweddi.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.adapters.SupplierAdapter
import com.example.letsgetweddi.data.FirebaseRefs
import com.example.letsgetweddi.databinding.FragmentFavoritesBinding
import com.example.letsgetweddi.model.Supplier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class FavoritesFragment : Fragment() {

    private lateinit var binding: FragmentFavoritesBinding
    private val favorites = mutableListOf<Supplier>()
    private lateinit var adapter: SupplierAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SupplierAdapter(favorites, isFavorites = true)
        binding.recyclerFavorites.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFavorites.adapter = adapter

        val user = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseRefs.favorites(user.uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val result = LinkedHashMap<String, Supplier>()
                val idsToFetch = mutableListOf<String>()

                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val raw = child.value
                    when (raw) {
                        is Boolean -> {
                            if (raw) idsToFetch.add(id)
                        }

                        is Map<*, *> -> {
                            snapToSupplierSafely(child, fallbackId = id)?.let { s ->
                                result[id] = s
                            }
                        }

                        else -> {
                        }
                    }
                }

                if (idsToFetch.isEmpty()) {
                    applyFavorites(result.values.toList())
                    return
                }

                var remaining = idsToFetch.size
                for (sid in idsToFetch) {
                    FirebaseRefs.suppliers().child(sid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snap: DataSnapshot) {
                                snapToSupplierSafely(snap, fallbackId = sid)?.let { s ->
                                    result[sid] = s
                                }
                                if (--remaining == 0) applyFavorites(result.values.toList())
                            }

                            override fun onCancelled(error: DatabaseError) {
                                if (--remaining == 0) applyFavorites(result.values.toList())
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                favorites.clear()
                adapter.notifyDataSetChanged()
                binding.textEmpty.visibility = View.VISIBLE
            }
        })
    }

    private fun applyFavorites(list: List<Supplier>) {
        val sorted = list.sortedBy { it.name ?: "" }
        favorites.clear()
        favorites.addAll(sorted)
        adapter.notifyDataSetChanged()
        binding.textEmpty.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun snapToSupplierSafely(snap: DataSnapshot, fallbackId: String? = null): Supplier? {
        val id = snap.key ?: fallbackId ?: return null

        fun value(path: String): Any? = snap.child(path).value

        fun stringOrNull(path: String): String? {
            val v = value(path)
            return when (v) {
                null -> null
                is String -> v
                is Number, is Boolean -> v.toString()
                is Map<*, *> -> null
                else -> null
            }
        }

        fun floatOrNull(path: String): Float? {
            val v = value(path)
            return when (v) {
                is Number -> v.toFloat()
                is String -> v.toFloatOrNull()
                else -> null
            }
        }

        fun intOrNull(path: String): Int? {
            val v = value(path)
            return when (v) {
                is Number -> v.toInt()
                is String -> v.toIntOrNull()
                else -> null
            }
        }

        val name = stringOrNull("name")
        val description = stringOrNull("description")
        val imageUrl = stringOrNull("imageUrl") ?: stringOrNull("coverImage")
        val categoryId = stringOrNull("categoryId") ?: stringOrNull("category")
        val rating = floatOrNull("rating") ?: floatOrNull("averageRating")
        val reviewCount = intOrNull("reviewCount") ?: intOrNull("reviewsCount")

        val phone = stringOrNull("phone")

        val location: String? = when (val locVal = value("location")) {
            is String -> locVal
            is Map<*, *> -> {
                val city = (locVal["city"] as? String)?.takeIf { it.isNotBlank() }
                val region = (locVal["region"] as? String)?.takeIf { it.isNotBlank() }
                listOfNotNull(city, region).joinToString(", ").ifBlank { null }
            }

            else -> null
        }

        return Supplier(
            id = id,
            name = name,
            description = description,
            imageUrl = imageUrl,
            categoryId = categoryId,
            rating = rating,
            reviewCount = reviewCount,
            phone = phone,
            location = location
        )
    }
}
