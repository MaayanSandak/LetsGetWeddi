package com.example.letsgetweddi.ui.providers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.adapters.SupplierAdapter
import com.example.letsgetweddi.databinding.FragmentAllSuppliersBinding
import com.example.letsgetweddi.model.Supplier
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AllSuppliersFragment : Fragment() {

    private var _binding: FragmentAllSuppliersBinding? = null
    private val binding get() = _binding!!

    private val all = mutableListOf<Supplier>()
    private val shown = mutableListOf<Supplier>()
    private lateinit var adapter: SupplierAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllSuppliersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SupplierAdapter(shown)
        binding.recyclerSuppliers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSuppliers.setHasFixedSize(true)
        binding.recyclerSuppliers.adapter = adapter

        setupSearch()
        setupLocationSpinner()

        binding.progressBar.visibility = View.VISIBLE
        loadAllSuppliers()
    }

    private fun setupSearch() {
        val sv: SearchView = binding.searchView
        sv.isIconified = false
        sv.clearFocus()
        sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                applyFilter(); return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilter(); return true
            }
        })
    }

    private fun setupLocationSpinner() {
        binding.spinnerLocation.setOnItemSelectedListenerCompat { applyFilter() }
    }

    private fun loadAllSuppliers() {
        val db = FirebaseDatabase.getInstance()
        val refs = listOf(
            db.getReference("Suppliers"),
            db.getReference("suppliers")
        )

        all.clear()
        shown.clear()
        adapter.notifyDataSetChanged()

        fetchSequential(refs.iterator()) {
            applyFilter()
            binding.progressBar.visibility = View.GONE
            toggleEmpty(shown.isEmpty())
            adapter.notifyDataSetChanged()
        }
    }

    private fun fetchSequential(it: Iterator<DatabaseReference>, done: () -> Unit) {
        if (!it.hasNext()) {
            done(); return
        }
        val ref = it.next()
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val s = Supplier.fromSnapshot(child)
                    if (s.id != null) all.add(s)
                }
                fetchSequential(it, done)
            }

            override fun onCancelled(error: DatabaseError) {
                fetchSequential(it, done)
            }
        })
    }

    private fun applyFilter() {
        val q = binding.searchView.query?.toString()?.trim()?.lowercase().orEmpty()
        val selectedLocation = (binding.spinnerLocation.selectedItem as? String) ?: "All locations"

        shown.clear()
        shown.addAll(
            all.asSequence()
                .filter { s -> q.isEmpty() || (s.name ?: "").lowercase().contains(q) }
                .filter { s ->
                    selectedLocation == "All locations" || (s.location ?: "") == selectedLocation
                }
                .sortedBy { it.name ?: "" }
                .toList()
        )

        toggleEmpty(shown.isEmpty())
        adapter.notifyDataSetChanged()
    }

    private fun toggleEmpty(isEmpty: Boolean) {
        binding.textEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerSuppliers.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun android.widget.Spinner.setOnItemSelectedListenerCompat(block: () -> Unit) {
        this.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                block()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                block()
            }
        }
    }
}
