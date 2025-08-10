package com.example.letsgetweddi.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.adapters.SupplierAdapter
import com.example.letsgetweddi.data.DbPaths
import com.example.letsgetweddi.databinding.FragmentHallsBinding
import com.example.letsgetweddi.model.Supplier
import com.google.firebase.database.*

class HallsFragment : Fragment() {

    private var _binding: FragmentHallsBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var adapter: SupplierAdapter
    private val allSuppliers = mutableListOf<Supplier>()
    private val filteredSuppliers = mutableListOf<Supplier>()
    private val locationList = mutableListOf("All locations")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHallsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SupplierAdapter(filteredSuppliers)
        binding.recyclerHalls.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHalls.adapter = adapter

        binding.searchViewHalls.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { filter(); return true }
            override fun onQueryTextChange(newText: String?): Boolean { filter(); return true }
        })

        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, locationList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLocationHalls.adapter = spinnerAdapter
        binding.spinnerLocationHalls.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) { filter() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        database = FirebaseDatabase.getInstance().getReference(DbPaths.SUPPLIERS)
        database.orderByChild("category").equalTo("halls")
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
                            val loc = s.location?.trim().orEmpty()
                            if (loc.isNotEmpty() && !locationList.contains(loc)) locationList.add(loc)
                        }
                    }
                    spinnerAdapter.notifyDataSetChanged()
                    filter()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun filter() {
        val q = binding.searchViewHalls.query?.toString()?.lowercase()?.trim().orEmpty()
        val loc = binding.spinnerLocationHalls.selectedItem?.toString().orEmpty()
        filteredSuppliers.clear()
        filteredSuppliers.addAll(allSuppliers.filter { s ->
            val nameOk = s.name?.lowercase()?.contains(q) == true
            val locOk = loc == "All locations" || s.location == loc
            nameOk && locOk
        })
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
