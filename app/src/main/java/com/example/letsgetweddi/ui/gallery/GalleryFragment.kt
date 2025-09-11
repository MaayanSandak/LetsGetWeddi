package com.example.letsgetweddi.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.letsgetweddi.R
import com.example.letsgetweddi.databinding.FragmentGalleryBinding
import com.google.firebase.storage.FirebaseStorage

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var recycler: RecyclerView
    private lateinit var emptyText: TextView

    private val images = ArrayList<String>()
    private lateinit var adapter: ImageAdapter

    private var supplierId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supplierId = arguments?.getString(ARG_SUPPLIER_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        // Be robust even if the generated binding field name differs:
        recycler = binding.root.findViewById(R.id.recycler)
        emptyText = binding.root.findViewById(R.id.textEmpty)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ImageAdapter(images)
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        recycler.setHasFixedSize(true)
        recycler.adapter = adapter
        loadFromStorage()
    }

    private fun loadFromStorage() {
        val id = supplierId ?: return
        emptyText.visibility = View.GONE
        images.clear()
        adapter.notifyDataSetChanged()

        val ref = FirebaseStorage.getInstance()
            .reference.child("suppliers/$id/gallery")

        ref.listAll()
            .addOnSuccessListener { result ->
                if (result.items.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }
                // Fetch download URLs in order; update adapter when all done
                var remaining = result.items.size
                result.items.forEach { item ->
                    item.downloadUrl
                        .addOnSuccessListener { uri ->
                            images.add(uri.toString())
                        }
                        .addOnCompleteListener {
                            remaining -= 1
                            if (remaining == 0) {
                                images.sort()
                                adapter.notifyDataSetChanged()
                                emptyText.visibility =
                                    if (images.isEmpty()) View.VISIBLE else View.GONE
                            }
                        }
                }
            }
            .addOnFailureListener {
                emptyText.visibility = View.VISIBLE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SUPPLIER_ID = "supplierId"

        fun newInstance(supplierId: String): GalleryFragment {
            return GalleryFragment().apply {
                arguments = Bundle().apply { putString(ARG_SUPPLIER_ID, supplierId) }
            }
        }
    }
}
