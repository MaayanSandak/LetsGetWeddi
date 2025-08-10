package com.example.letsgetweddi.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.letsgetweddi.data.DbPaths
import com.example.letsgetweddi.databinding.FragmentGalleryBinding
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ImageAdapter
    private val imageUrls = mutableListOf<String>()
    private var supplierId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supplierId = arguments?.getString(ARG_SUPPLIER_ID).orEmpty()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ImageAdapter(imageUrls)
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = adapter
        loadImages()
    }

    private fun loadImages() {
        binding.progressBar.visibility = View.VISIBLE
        binding.textEmpty.visibility = View.GONE
        imageUrls.clear()

        val dbRef = FirebaseDatabase.getInstance().getReference(DbPaths.supplier(supplierId)).child("gallery")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.children.count() > 0) {
                    for (c in snapshot.children) {
                        c.getValue(String::class.java)?.let { imageUrls.add(it) }
                    }
                    finishLoad()
                } else {
                    loadFromStorage()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                loadFromStorage()
            }
        })
    }

    private fun loadFromStorage() {
        val storageRef = FirebaseStorage.getInstance().reference.child("supplier_galleries").child(supplierId)
        storageRef.listAll().addOnSuccessListener { list ->
            if (list.items.isEmpty()) {
                finishLoad()
                return@addOnSuccessListener
            }
            var pending = list.items.size
            list.items.forEach { item ->
                item.downloadUrl.addOnSuccessListener { uri ->
                    imageUrls.add(uri.toString())
                }.addOnCompleteListener {
                    pending -= 1
                    if (pending == 0) finishLoad()
                }
            }
        }.addOnFailureListener { finishLoad() }
    }

    private fun finishLoad() {
        binding.progressBar.visibility = View.GONE
        adapter.notifyDataSetChanged()
        binding.textEmpty.visibility = if (imageUrls.isEmpty()) View.VISIBLE else View.GONE
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
