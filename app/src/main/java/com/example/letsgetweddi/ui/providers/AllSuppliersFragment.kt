package com.example.letsgetweddi.ui.providers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.adapters.SupplierAdapter
import com.example.letsgetweddi.data.FirebaseRefs
import com.example.letsgetweddi.databinding.FragmentAllSuppliersBinding
import com.example.letsgetweddi.model.Supplier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * All suppliers list with search, location filter, and favorites state syncing.
 */
class AllSuppliersFragment : Fragment() {

    private var _binding: FragmentAllSuppliersBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SupplierAdapter
    private val allSuppliers = mutableListOf<Supplier>()
    private val filtered = mutableListOf<Supplier>()

    private var suppliersListener: ValueEventListener? = null
    private var favoritesListener: ValueEventListener? = null

    private val db: FirebaseDatabase get() = FirebaseDatabase.getInstance()
    private val suppliersRef: DatabaseReference get() = FirebaseRefs.suppliers()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllSuppliersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SupplierAdapter(filtered, false)
        binding.recyclerSuppliers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSuppliers.adapter = adapter

        // Load suppliers
        attachSuppliersListener()

        // Observe favorites for icon state
        attachFavoritesListener()

        // Search & location filter
        setupFilters()
    }

    private fun attachSuppliersListener() {
        suppliersListener?.let { suppliersRef.removeEventListener(it) }
        suppliersListener = suppliersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allSuppliers.clear()
                snapshot.children.forEach { child ->
                    allSuppliers.add(Supplier.fromSnapshot(child))
                }
                applyFilter()
            }

            override fun onCancelled(error: DatabaseError) {
                // No-op; keep UI as-is
            }
        })
    }

    private fun attachFavoritesListener() {
        favoritesListener?.let {
            // If already attached previously, detach
            val uidPrev = FirebaseAuth.getInstance().currentUser?.uid
            if (uidPrev != null) FirebaseRefs.favorites(uidPrev).removeEventListener(it)
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            adapter.setFavoriteIds(emptySet())
            return
        }

        favoritesListener =
            FirebaseRefs.favorites(uid).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ids = snapshot.children
                        .filter { it.getValue(Boolean::class.java) == true }
                        .mapNotNull { it.key }
                        .toSet()
                    adapter.setFavoriteIds(ids)
                }

                override fun onCancelled(error: DatabaseError) {
                    adapter.setFavoriteIds(emptySet())
                }
            })
    }

    private fun setupFilters() {
        // Example spinner expects item "All locations" + concrete locations; adapt to your dataset
        val locations = mutableListOf("All locations")
        // Build locations list from suppliers when they change
        // (Initial empty; will be repopulated in applyFilter)

        binding.searchViewSuppliers.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                applyFilter()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilter()
                return true
            }
        })

        // Basic adapter; items updated whenever suppliers update
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            locations
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.spinnerLocation.adapter = spinnerAdapter
        binding.spinnerLocation.setOnItemSelectedListenerCompat { _, _ ->
            applyFilter()
        }

        // Rebuild locations whenever suppliers update
        suppliersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val set = snapshot.children
                    .map { Supplier.fromSnapshot(it).location ?: "" }
                    .filter { it.isNotBlank() }
                    .toMutableSet()
                locations.clear()
                locations.add("All locations")
                locations.addAll(set.sorted())
                spinnerAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun applyFilter() {
        val q = binding.searchViewSuppliers.query?.toString().orEmpty().lowercase()
        val selectedLocation = binding.spinnerLocation.selectedItem?.toString() ?: "All locations"

        filtered.clear()
        filtered.addAll(
            allSuppliers.filter { s ->
                val matchesQuery = s.name?.lowercase()?.contains(q) == true
                val matchesLoc =
                    selectedLocation == "All locations" || s.location == selectedLocation
                matchesQuery && matchesLoc
            }.sortedBy { it.name ?: "" }
        )
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        suppliersListener?.let { suppliersRef.removeEventListener(it) }
        favoritesListener?.let {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) FirebaseRefs.favorites(uid).removeEventListener(it)
        }
        _binding = null
    }
}

/**
 * Small helper to avoid verbose AdapterView listeners.
 */
private fun ViewGroup.setOnItemSelectedListenerCompat(onSelected: (position: Int, id: Long) -> Unit) {
    // This view is actually Spinner in layout; attach a simple listener via reflection-safe cast.
    val spinner = this as? android.widget.Spinner ?: return
    spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: android.widget.AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            onSelected(position, id)
        }

        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
    }
}
