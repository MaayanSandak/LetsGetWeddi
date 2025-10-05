package com.example.letsgetweddi.ui.supplier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.letsgetweddi.databinding.FragmentSupplierCalendarBinding

class SupplierCalendarFragment : Fragment() {

    private var _binding: FragmentSupplierCalendarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupplierCalendarBinding.inflate(inflater, container, false)
        hideSupplierEditControls()
        return binding.root
    }

    private fun hideSupplierEditControls() {
        vOrNull("buttonAddDate")?.visibility = View.GONE
        vOrNull("btnAddDate")?.visibility = View.GONE
        vOrNull("buttonRemoveDate")?.visibility = View.GONE
        vOrNull("btnRemoveDate")?.visibility = View.GONE
    }

    private fun vOrNull(name: String): View? {
        val id = resources.getIdentifier(name, "id", requireContext().packageName)
        return if (id != 0) binding.root.findViewById(id) else null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
