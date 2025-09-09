package com.example.letsgetweddi.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.letsgetweddi.databinding.FragmentGalleryBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private var supplierId: String? = null
    private val images = mutableListOf<String>()
    private lateinit var adapter: ImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supplierId = arguments?.getString(ARG_SUPPLIER_ID)
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
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = adapter

        binding.progressBar.visibility = View.VISIBLE
        binding.textEmpty.visibility = View.GONE
        images.clear()
        adapter.notifyDataSetChanged()

        val id = supplierId ?: return
        loadFromStorage(id)
    }

    private fun loadFromStorage(id: String) {
        val root = FirebaseStorage.getInstance().reference
            .child("supplier_galleries")
            .child(id)

        root.listAll()
            .addOnSuccessListener { list ->
                if (list.items.isEmpty()) {
                    loadFromDbCandidates(id)
                    return@addOnSuccessListener
                }
                var remaining = list.items.size
                list.items.forEach { item ->
                    item.downloadUrl
                        .addOnSuccessListener { uri ->
                            images.add(uri.toString())
                        }
                        .addOnCompleteListener {
                            remaining -= 1
                            if (remaining <= 0) finishLoading()
                        }
                }
            }
            .addOnFailureListener {
                loadFromDbCandidates(id)
            }
    }

    private fun loadFromDbCandidates(id: String) {
        val db = FirebaseDatabase.getInstance()
        val candidates = listOf(
            "Suppliers/$id/images",
            "suppliers/$id/images",
            "supplier_galleries/$id",
            "Gallery/$id",
            "gallery/$id"
        )
        tryLoadDbPath(db, candidates.iterator())
    }

    private fun tryLoadDbPath(db: FirebaseDatabase, it: Iterator<String>) {
        if (!it.hasNext()) {
            finishLoading()
            return
        }
        val path = it.next()
        db.getReference(path).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<String>()
                for (child in snapshot.children) {
                    val url = child.getValue(String::class.java)
                    if (!url.isNullOrBlank()) list.add(url)
                }
                if (list.isNotEmpty()) {
                    images.addAll(list)
                    finishLoading()
                } else {
                    tryLoadDbPath(db, it)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                tryLoadDbPath(db, it)
            }
        })
    }

    private fun finishLoading() {
        binding.progressBar.visibility = View.GONE
        if (images.isEmpty()) {
            binding.textEmpty.visibility = View.VISIBLE
        } else {
            binding.textEmpty.visibility = View.GONE
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SUPPLIER_ID = "supplierId"
        fun newInstance(supplierId: String): GalleryFragment {
            val f = GalleryFragment()
            f.arguments = Bundle().apply { putString(ARG_SUPPLIER_ID, supplierId) }
            return f
        }
    }
}
