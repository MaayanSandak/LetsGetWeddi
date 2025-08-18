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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SuppliersListFragment : Fragment() {

    private var _binding: FragmentSuppliersListBinding? = null
    private val binding get() = _binding!!

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
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSuppliersListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val cat = category ?: run { showEmpty(true); return }

        // Set screen title to the current category
        requireActivity().title = cat.title

        adapter = SupplierAdapter(filteredSuppliers, isFavorites = false)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.setHasFixedSize(true)
        binding.recycler.adapter = adapter

        spinnerAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, locationList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLocation.adapter = spinnerAdapter

        binding.spinnerLocation.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    v: View?,
                    position: Int,
                    id: Long
                ) = filter()

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true.also { filter() }
            override fun onQueryTextChange(newText: String?) = true.also { filter() }
        })

        loadSuppliers(cat)
    }

    private fun loadSuppliers(cat: Category) {
        showEmpty(false)
        val db = FirebaseDatabase.getInstance()
        val refs = listOf(
            db.getReference(DbPaths.SUPPLIERS), // "Suppliers"
            db.getReference("suppliers")        // lowercase fallback
        )

        fun tryIndex(i: Int) {
            if (i >= refs.size) {
                bindFromSnapshot(null, cat); return
            }
            val ref = refs[i]
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.hasChildren()) bindFromSnapshot(snapshot, cat)
                    else tryIndex(i + 1)
                }

                override fun onCancelled(error: DatabaseError) {
                    tryIndex(i + 1)
                }
            })
        }
        tryIndex(0)
    }

    private fun bindFromSnapshot(snapshot: DataSnapshot?, cat: Category) {
        allSuppliers.clear()
        locationList.clear()
        locationList.add("All locations")

        val nodes = snapshot?.children?.toList().orEmpty()
        for (node in nodes) {
            val s = Supplier.fromSnapshot(node)
            if (s.id == null) continue
            allSuppliers.add(s)
            val loc = s.location.orEmpty()
            if (loc.isNotBlank() && !locationList.contains(loc)) locationList.add(loc)
        }

        // Try category filter; if empty, show all so UI stays visible
        val wantedId = cat.id.lowercase()
        val wantedTitle = cat.title.lowercase()
        val byCategory = allSuppliers.filter { s ->
            val catField = s.category?.lowercase().orEmpty()
            catField == wantedId || catField == wantedTitle
        }
        if (byCategory.isNotEmpty()) {
            allSuppliers.clear()
            allSuppliers.addAll(byCategory)
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
            val locOk = (loc == "All locations") || (s.location == loc)
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
