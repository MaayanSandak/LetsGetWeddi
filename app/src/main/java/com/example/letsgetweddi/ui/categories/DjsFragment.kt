package com.example.letsgetweddi.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.adapters.SupplierAdapter
import com.example.letsgetweddi.data.DbPaths
import com.example.letsgetweddi.databinding.FragmentDjsBinding
import com.example.letsgetweddi.model.Supplier
import com.google.firebase.database.*

class DjsFragment : Fragment() {

    private lateinit var binding: FragmentDjsBinding
    private lateinit var database: DatabaseReference
    private lateinit var adapter: SupplierAdapter
    private val allSuppliers = mutableListOf<Supplier>()
    private val filteredSuppliers = mutableListOf<Supplier>()
    private val locationList = mutableListOf("All locations")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentDjsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SupplierAdapter(filteredSuppliers)
        binding.recyclerDjs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDjs.adapter = adapter

        binding.searchViewDjs.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { filterSuppliers(); return true }
            override fun onQueryTextChange(newText: String?): Boolean { filterSuppliers(); return true }
        })

        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, locationList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLocationDjs.adapter = spinnerAdapter
        binding.spinnerLocationDjs.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterSuppliers()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        database = FirebaseDatabase.getInstance().getReference(DbPaths.SUPPLIERS)
        database.orderByChild("category").equalTo("djs")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allSuppliers.clear()
                    locationList.clear()
                    locationList.add("All locations")
                    for (child in snapshot.children) {
                        val s = child.getValue(Supplier::class.java)?.copy(
                            id = child.child("id").getValue(String::class.java) ?: child.key
                        )
                        if (s != null) {
                            allSuppliers.add(s)
                            s.location?.takeIf { it.isNotBlank() }?.let { if (!locationList.contains(it)) locationList.add(it) }
                        }
                    }
                    spinnerAdapter.notifyDataSetChanged()
                    filterSuppliers()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun filterSuppliers() {
        val query = binding.searchViewDjs.query.toString().lowercase()
        val selectedLocation = binding.spinnerLocationDjs.selectedItem.toString()
        filteredSuppliers.clear()
        filteredSuppliers.addAll(allSuppliers.filter { supplier ->
            val matchesQuery = supplier.name?.lowercase()?.contains(query) == true
            val matchesLocation = selectedLocation == "All locations" || supplier.location == selectedLocation
            matchesQuery && matchesLocation
        })
        adapter.notifyDataSetChanged()
    }
}
