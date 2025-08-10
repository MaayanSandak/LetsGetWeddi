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
import com.example.letsgetweddi.databinding.FragmentAllSuppliersBinding
import com.example.letsgetweddi.model.Supplier
import com.google.firebase.database.*

class AllSuppliersFragment : Fragment() {

    private var _binding: FragmentAllSuppliersBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbRef: DatabaseReference
    private lateinit var supplierAdapter: SupplierAdapter
    private val allSuppliersList = mutableListOf<Supplier>()
    private val filteredSuppliersList = mutableListOf<Supplier>()
    private val locationList = mutableListOf("All locations")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllSuppliersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dbRef = FirebaseDatabase.getInstance().getReference("Suppliers")

        supplierAdapter = SupplierAdapter(filteredSuppliersList)
        binding.recyclerSuppliers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSuppliers.adapter = supplierAdapter

        loadSuppliers()

        binding.searchViewSuppliers.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterSuppliers(query ?: "", binding.spinnerLocation.selectedItem.toString())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterSuppliers(newText ?: "", binding.spinnerLocation.selectedItem.toString())
                return true
            }
        })

        binding.spinnerLocation.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                filterSuppliers(
                    binding.searchViewSuppliers.query.toString(),
                    binding.spinnerLocation.selectedItem.toString()
                )
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
            }
        }
    }

    private fun loadSuppliers() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allSuppliersList.clear()
                locationList.clear()
                locationList.add("All locations")

                for (child in snapshot.children) {
                    val supplier = child.getValue(Supplier::class.java)
                    if (supplier != null) {
                        allSuppliersList.add(supplier)
                        if (!supplier.location.isNullOrEmpty() && !locationList.contains(supplier.location!!)) {
                            locationList.add(supplier.location!!)
                        }
                    }
                }

                val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, locationList)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerLocation.adapter = spinnerAdapter

                filterSuppliers(
                    binding.searchViewSuppliers.query.toString(),
                    binding.spinnerLocation.selectedItem.toString()
                )
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun filterSuppliers(query: String, location: String) {
        val queryLower = query.lowercase()
        filteredSuppliersList.clear()
        filteredSuppliersList.addAll(allSuppliersList.filter { supplier ->
            val matchesQuery = supplier.name?.lowercase()?.contains(queryLower) == true
            val matchesLocation = (location == "All locations" || supplier.location == location)
            matchesQuery && matchesLocation
        })
        supplierAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
