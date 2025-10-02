package com.example.letsgetweddi.ui.gallery

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.letsgetweddi.databinding.ActivityGalleryViewBinding
import com.example.letsgetweddi.ui.ProviderDetailsActivity
import com.google.firebase.storage.FirebaseStorage

class GalleryViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryViewBinding
    private val images = mutableListOf<String>()
    private lateinit var adapter: ImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        title = "Gallery"

        adapter = ImageAdapter(images)
        binding.recycler.layoutManager = GridLayoutManager(this, 2)
        binding.recycler.adapter = adapter

        val id = intent.getStringExtra(ProviderDetailsActivity.EXTRA_SUPPLIER_ID)
            ?: intent.data?.lastPathSegment

        if (id.isNullOrBlank()) {
            Toast.makeText(this, "Missing supplier id", Toast.LENGTH_SHORT).show()
            binding.textEmpty.text = "no_images"
            binding.textEmpty.visibility = android.view.View.VISIBLE
            return
        }

        loadFromStorage(id)
    }

    private fun loadFromStorage(id: String) {
        images.clear()
        adapter.notifyDataSetChanged()
        showEmpty(false)

        val root = FirebaseStorage.getInstance().reference.child("suppliers/$id/gallery")
        root.listAll()
            .addOnSuccessListener { result ->
                if (result.items.isEmpty()) {
                    showEmpty(true); return@addOnSuccessListener
                }
                var remaining = result.items.size
                result.items.forEach { ref ->
                    ref.downloadUrl
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
        binding.textEmpty.visibility =
            if (empty) android.view.View.VISIBLE else android.view.View.GONE
        binding.recycler.visibility =
            if (empty) android.view.View.GONE else android.view.View.VISIBLE
    }
}
