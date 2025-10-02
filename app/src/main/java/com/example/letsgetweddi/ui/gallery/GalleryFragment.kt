package com.example.letsgetweddi.ui.gallery

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.letsgetweddi.databinding.FragmentGalleryBinding
import com.google.firebase.storage.FirebaseStorage

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private val images = mutableListOf<String>()
    private lateinit var adapter: ImageAdapter
    private var supplierId: String? = null
    private var categoryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supplierId = arguments?.getString("supplierId")
        categoryId = arguments?.getString("categoryId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ImageAdapter(images)
        binding.recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recycler.adapter = adapter

        binding.header.setOnClickListener {
            val id = supplierId
            val category = categoryId
            if (!id.isNullOrBlank() && !category.isNullOrBlank()) {
                val uri = Uri.parse("letsgetweddi://gallery/$id")
                startActivity(
                    Intent(Intent.ACTION_VIEW, uri)
                        .putExtra("supplierId", id)
                        .putExtra("categoryId", category)
                )
            }
        }

        if (!supplierId.isNullOrBlank()) {
            loadFromStorage(supplierId!!)
        } else {
            showEmpty(true)
        }
    }

    private fun loadFromStorage(supplierId: String) {
        images.clear()
        adapter.notifyDataSetChanged()
        showEmpty(false)

        val root = FirebaseStorage.getInstance().reference
            .child("suppliers/$supplierId/gallery")

        root.listAll()
            .addOnSuccessListener { result ->
                result.items.forEach { ref ->
                    ref.downloadUrl
                        .addOnSuccessListener { uri ->
                            images.add(uri.toString())
                        }
                        .addOnCompleteListener {
                            adapter.notifyDataSetChanged()
                        }
                }
            }

            .addOnFailureListener { showEmpty(true) }
    }

    private fun showEmpty(empty: Boolean) {
        binding.textEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recycler.visibility = if (empty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(supplierId: String, categoryId: String): GalleryFragment {
            return GalleryFragment().apply {
                arguments = Bundle().apply {
                    putString("supplierId", supplierId)
                    putString("categoryId", categoryId)
                }
            }
        }
    }
}
