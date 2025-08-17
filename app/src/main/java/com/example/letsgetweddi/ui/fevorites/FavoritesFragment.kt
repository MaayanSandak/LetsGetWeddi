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
                favorites.clear()
                for (child in snapshot.children) {
                    val supplier = child.getValue(Supplier::class.java)?.copy(
                        id = child.child("id").getValue(String::class.java) ?: child.key
                    )
                    if (supplier != null) favorites.add(supplier)
                }
                favorites.sortBy { it.name ?: "" }
                adapter.notifyDataSetChanged()
                binding.textEmpty.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                binding.textEmpty.visibility = View.VISIBLE
            }
        })
    }
}
