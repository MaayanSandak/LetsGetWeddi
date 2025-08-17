package com.example.letsgetweddi.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.adapters.SupplierAdapter
import com.example.letsgetweddi.data.DbPaths
import com.example.letsgetweddi.databinding.FragmentSuppliersListBinding
import com.example.letsgetweddi.model.Category
import com.example.letsgetweddi.model.Supplier
import com.google.firebase.database.*

class SuppliersListFragment : Fragment() {

    private var _binding: FragmentSuppliersListBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var adapter: SupplierAdapter
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    private val allSuppliers = mutableListOf<Supplier>()
    private val filteredSuppliers = mutableListOf<Supplier>()
    private val locationList = mutableListOf("All locations")

    private var category: Category? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = arguments?.getString(ARG_CATEGORY_ID).orEmpty()
        category = Category.fromId(id)
        database = FirebaseDatabase.getInstance().getReference(DbPaths.SUPPLIERS)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSuppliersListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val cat = category ?: run {
            showEmpty(true)
            return
        }

        binding.headerTitle.text = cat.title
        binding.headerImage.setImageResource(cat.headerDrawable)

        adapter = SupplierAdapter(filteredSuppliers)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, locationList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLocation.adapter = spinnerAdapter

        binding.spinnerLocation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) = filter()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true.also { filter() }
            override fun onQueryTextChange(newText: String?) = true.also { filter() }
        })

        loadSuppliersForCategory(cat)
    }

    private fun loadSuppliersForCategory(cat: Category) {
        // Primary: query by category
        database.orderByChild("category").equalTo(cat.id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.hasChildren()) {
                        bindFromSnapshot(snapshot)
                    } else {
                        // Fallback: load all and filter locally (handles mismatched category keys)
                        database.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(allSnap: DataSnapshot) {
                                bindFromSnapshot(allSnap, filterBy = cat.id)
                            }
                            override fun onCancelled(error: DatabaseError) { showEmpty(true) }
                        })
                    }
                }
                override fun onCancelled(error: DatabaseError) { showEmpty(true) }
            })
    }

    private fun bindFromSnapshot(snapshot: DataSnapshot, filterBy: String? = null) {
        allSuppliers.clear()
        locationList.clear()
        locationList.add("All locations")

        for (child in snapshot.children) {
            val raw = child.getValue(Supplier::class.java) ?: continue
            val supplier = raw.copy(id = child.child("id").getValue(String::class.java) ?: child.key)
            if (filterBy == null || supplier.category == filterBy) {
                allSuppliers.add(supplier)
                val loc = supplier.location.orEmpty()
                if (loc.isNotBlank() && !locationList.contains(loc)) locationList.add(loc)
            }
        }

        spinnerAdapter.notifyDataSetChanged()
        filter()
    }

    private fun filter() {
        val query = binding.searchView.query?.toString()?.lowercase()?.trim().orEmpty()
        val loc = binding.spinnerLocation.selectedItem?.toString().orEmpty()

        filteredSuppliers.clear()
        filteredSuppliers.addAll(allSuppliers.filter { s ->
            val nameOk = s.name?.lowercase()?.contains(query) == true
            val locOk = loc == "All locations" || s.location == loc
            nameOk && locOk
        })

        adapter.notifyDataSetChanged()
        showEmpty(filteredSuppliers.isEmpty())
    }

    private fun showEmpty(empty: Boolean) {
        binding.emptyView.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recycler.visibility = if (empty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"
        fun newInstance(category: Category) = SuppliersListFragment().apply {
            arguments = bundleOf(ARG_CATEGORY_ID to category.id)
        }
    }
}
