package com.example.letsgetweddi.ui.gallery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.letsgetweddi.databinding.ActivityGalleryViewBinding
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

        adapter = ImageAdapter(images)
        binding.recycler.layoutManager = GridLayoutManager(this, 2)
        binding.recycler.adapter = adapter

        val supplierId = intent.getStringExtra("supplierId") ?: return
        title = "Gallery"

        val ref = FirebaseStorage.getInstance()
            .reference
            .child("suppliers/$supplierId/gallery")

        ref.listAll()
            .addOnSuccessListener { result ->
                if (result.items.isEmpty()) {
                    binding.textEmpty.visibility = android.view.View.VISIBLE
                    return@addOnSuccessListener
                }
                var pending = result.items.size
                result.items.forEach { item ->
                    item.downloadUrl
                        .addOnSuccessListener { uri -> images.add(uri.toString()) }
                        .addOnCompleteListener {
                            pending -= 1
                            if (pending <= 0) {
                                images.sort()
                                binding.textEmpty.visibility =
                                    if (images.isEmpty()) android.view.View.VISIBLE
                                    else android.view.View.GONE
                                adapter.notifyDataSetChanged()
                            }
                        }
                }
            }
            .addOnFailureListener {
                binding.textEmpty.visibility = android.view.View.VISIBLE
            }
    }
}
