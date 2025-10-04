package com.example.letsgetweddi.ui.gallery

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.letsgetweddi.databinding.FragmentGalleryBinding
import com.example.letsgetweddi.ui.ProviderDetailsActivity
import com.google.firebase.database.FirebaseDatabase
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
                startActivity(
                    Intent(requireContext(), GalleryViewActivity::class.java)
                        .putExtra(ProviderDetailsActivity.EXTRA_SUPPLIER_ID, id)
                        .putExtra("categoryId", category)
                )
            }
        }

        val id = supplierId
        if (id.isNullOrBlank()) {
            showEmpty(true)
        } else {
            loadFromDatabaseOrStorage(id)
        }
    }

    private fun loadFromDatabaseOrStorage(id: String) {
        images.clear()
        adapter.notifyDataSetChanged()
        showEmpty(false)

        val db = FirebaseDatabase.getInstance()
        db.getReference("suppliers/$id").get()
            .addOnSuccessListener { snap ->
                val list = mutableListOf<String>()
                val node = snap.child("gallery")
                if (node.exists()) {
                    node.children.forEach { c ->
                        c.getValue(String::class.java)?.let { list.add(it) }
                    }
                }
                if (list.isNotEmpty()) {
                    loadGsList(list)
                } else {
                    listFolder(id)
                }
            }
            .addOnFailureListener { listFolder(id) }
    }

    private fun loadGsList(gsList: List<String>) {
        val storage = FirebaseStorage.getInstance()
        var remaining = gsList.size
        gsList.forEach { gs ->
            try {
                val ref = storage.getReferenceFromUrl(gs)
                ref.downloadUrl
                    .addOnSuccessListener { uri -> images.add(uri.toString()) }
                    .addOnCompleteListener {
                        remaining -= 1
                        if (remaining == 0) {
                            adapter.notifyDataSetChanged()
                            showEmpty(images.isEmpty())
                        }
                    }
            } catch (_: Throwable) {
                remaining -= 1
                if (remaining == 0) {
                    adapter.notifyDataSetChanged()
                    showEmpty(images.isEmpty())
                }
            }
        }
    }

    private fun listFolder(id: String) {
        val ref = FirebaseStorage.getInstance().reference
            .child("suppliers/$id/gallery")
        ref.listAll()
            .addOnSuccessListener { result ->
                if (result.items.isEmpty()) {
                    showEmpty(true); return@addOnSuccessListener
                }
                var remaining = result.items.size
                result.items.forEach { item ->
                    item.downloadUrl
                        .addOnSuccessListener { uri -> images.add(uri.toString()) }
                        .addOnCompleteListener {
                            remaining -= 1
                            if (remaining == 0) {
                                adapter.notifyDataSetChanged()
                                showEmpty(images.isEmpty())
                            }
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
