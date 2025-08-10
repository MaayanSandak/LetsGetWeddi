package com.example.letsgetweddi.ui.supplier

import android.view.*
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.letsgetweddi.databinding.FragmentSupplierCalendarBinding
import com.example.letsgetweddi.utils.RoleManager
import com.example.letsgetweddi.utils.UiPermissions

class SupplierCalendarFragment : Fragment() {

    private var _binding: FragmentSupplierCalendarBinding? = null
    private val binding get() = _binding!!
    private var supplierId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSupplierCalendarBinding.inflate(inflater, container, false)

        RoleManager.load(requireContext()) { _, sid ->
            supplierId = sid
            applyUiPermissions()
        }

        return binding.root
    }

    private fun vOrNull(name: String): View? {
        val id = resources.getIdentifier(name, "id", requireContext().packageName)
        return if (id != 0) binding.root.findViewById(id) else null
    }

    private fun applyUiPermissions() {
        val id = supplierId ?: return
        UiPermissions.checkOwner(requireContext(), id) { owner ->
            vOrNull("buttonAddDate")?.visibility = if (owner) View.VISIBLE else View.GONE
            vOrNull("btnAddDate")?.visibility = if (owner) View.VISIBLE else View.GONE
            vOrNull("buttonRemoveDate")?.visibility = if (owner) View.VISIBLE else View.GONE
            vOrNull("btnRemoveDate")?.visibility = if (owner) View.VISIBLE else View.GONE

            (vOrNull("calendarView") ?: vOrNull("calendar"))?.isEnabled = owner
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
