package com.example.letsgetweddi.ui.supplier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.databinding.FragmentSupplierCalendarBinding
import com.example.letsgetweddi.utils.UiPermissions
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SupplierCalendarFragment : Fragment(), SupplierDatesAdapter.Listener {

    private var _binding: FragmentSupplierCalendarBinding? = null
    private val binding get() = _binding!!

    private var supplierId: String = ""
    private val dates = mutableListOf<String>()
    private var adapter: SupplierDatesAdapter? = null
    private var canEdit: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supplierId = requireArguments().getString(ARG_SUPPLIER_ID).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupplierCalendarBinding.inflate(inflater, container, false)

        binding.recyclerDates.layoutManager = LinearLayoutManager(requireContext())
        adapter = SupplierDatesAdapter(dates, canEdit = false, listener = this)
        binding.recyclerDates.adapter = adapter

        binding.progressBar.visibility = View.GONE
        binding.textEmpty.visibility = View.GONE

        loadDates()

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            if (canEdit) {
                val iso = toIso(year, month, dayOfMonth)
                addDate(iso)
            } else {
                Toast.makeText(requireContext(), "View-only", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClearAll.setOnClickListener {
            if (canEdit) clearAllDates() else Toast.makeText(requireContext(), "View-only", Toast.LENGTH_SHORT).show()
        }

        UiPermissions.checkOwner(supplierId) { owner ->
            canEdit = owner
            binding.btnClearAll.visibility = if (owner) View.VISIBLE else View.GONE
            binding.calendarView.isEnabled = owner
            adapter = SupplierDatesAdapter(dates, canEdit = owner, listener = this)
            binding.recyclerDates.adapter = adapter
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRemove(dateIso: String) {
        if (!canEdit) return
        dbRef().child(dateIso).removeValue().addOnCompleteListener {
            dates.remove(dateIso)
            adapter?.notifyDataSetChanged()
            binding.textEmpty.visibility = if (dates.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun dbRef() =
        FirebaseDatabase.getInstance()
            .getReference("SuppliersAvailability")
            .child(supplierId)

    private fun loadDates() {
        binding.progressBar.visibility = View.VISIBLE
        dbRef().addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dates.clear()
                for (c in snapshot.children) if (c.key != null) dates.add(c.key!!)
                dates.sort()
                adapter?.notifyDataSetChanged()
                binding.textEmpty.visibility = if (dates.isEmpty()) View.VISIBLE else View.GONE
                binding.progressBar.visibility = View.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun addDate(iso: String) {
        binding.progressBar.visibility = View.VISIBLE
        dbRef().child(iso).setValue(true).addOnCompleteListener { task ->
            binding.progressBar.visibility = View.GONE
            if (task.isSuccessful) {
                if (!dates.contains(iso)) {
                    val newList = (dates + iso).sorted()
                    dates.clear()
                    dates.addAll(newList)
                    adapter?.notifyDataSetChanged()
                    binding.textEmpty.visibility = if (dates.isEmpty()) View.VISIBLE else View.GONE
                }
                Toast.makeText(requireContext(), "Date added: $iso", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearAllDates() {
        binding.progressBar.visibility = View.VISIBLE
        dbRef().removeValue().addOnCompleteListener {
            binding.progressBar.visibility = View.GONE
            dates.clear()
            adapter?.notifyDataSetChanged()
            binding.textEmpty.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "All dates cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toIso(year: Int, monthZeroBased: Int, day: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthZeroBased)
        cal.set(Calendar.DAY_OF_MONTH, day)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    companion object {
        private const val ARG_SUPPLIER_ID = "supplierId"
        fun newInstance(supplierId: String): SupplierCalendarFragment {
            val f = SupplierCalendarFragment()
            f.arguments = Bundle().apply { putString(ARG_SUPPLIER_ID, supplierId) }
            return f
        }
    }
}
