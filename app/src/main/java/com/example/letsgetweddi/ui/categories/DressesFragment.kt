package com.example.letsgetweddi.ui.categories

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.adapters.SupplierAdapter
import com.example.letsgetweddi.data.DbPaths
import com.example.letsgetweddi.databinding.FragmentDressesBinding
import com.example.letsgetweddi.model.Supplier
import com.google.firebase.database.*

class DressesFragment : Fragment() {

    private lateinit var binding: FragmentDressesBinding
    private lateinit var database: DatabaseReference
    private lateinit var adapter: SupplierAdapter
    private val allSuppliers = mutableListOf<Supplier>()
    private val filteredSuppliers = mutableListOf<Supplier>()
    private val locationList = mutableListOf("All locations")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDressesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = SupplierAdapter(filteredSuppliers)
        binding.recyclerDresses.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDresses.adapter = adapter

        binding.searchViewDresses.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { filter(); return true }
            override fun onQueryTextChange(newText: String?): Boolean { filter(); return true }
        })

        val spinAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, locationList)
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLocationDresses.adapter = spinAdapter
        binding.spinnerLocationDresses.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { filter() }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        database = FirebaseDatabase.getInstance().getReference(DbPaths.SUPPLIERS)
        database.orderByChild("category").equalTo("dresses")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allSuppliers.clear()
                    locationList.clear()
                    locationList.add("All locations")
                    for (c in snapshot.children) {
                        val s = c.getValue(Supplier::class.java)?.copy(
                            id = c.child("id").getValue(String::class.java) ?: c.key
                        )
                        if (s != null) {
                            allSuppliers.add(s)
                            s.location?.takeIf { it.isNotBlank() }?.let { if (!locationList.contains(it)) locationList.add(it) }
                        }
                    }
                    spinAdapter.notifyDataSetChanged()
                    filter()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun filter() {
        val q = binding.searchViewDresses.query.toString().lowercase()
        val loc = binding.spinnerLocationDresses.selectedItem.toString()
        filteredSuppliers.clear()
        filteredSuppliers.addAll(allSuppliers.filter { s ->
            val nameOk = s.name?.lowercase()?.contains(q) == true
            val locOk = loc == "All locations" || s.location == loc
            nameOk && locOk
        })
        adapter.notifyDataSetChanged()
    }
}
