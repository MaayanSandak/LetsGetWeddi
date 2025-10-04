package com.example.letsgetweddi.ui.gallery

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.letsgetweddi.databinding.ActivityGalleryViewBinding
import com.example.letsgetweddi.ui.ProviderDetailsActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class GalleryViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryViewBinding
    private val images = mutableListOf<String>()
    private lateinit var adapter: ImageAdapter
    private lateinit var supplierId: String

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

        supplierId = intent.getStringExtra(ProviderDetailsActivity.EXTRA_SUPPLIER_ID)
            ?: intent.getStringExtra("supplierId")
                    ?: extractSupplierIdFromDeepLink(intent.data)
                    ?: ""

        val categoryId = intent.getStringExtra("categoryId")
            ?: intent.data?.getQueryParameter("categoryId")
            ?: ""

        if (supplierId.isBlank() || categoryId.isBlank()) {
            Toast.makeText(this, "Missing supplier or category", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        loadFromDatabaseOrStorage(supplierId)
    }

    private fun extractSupplierIdFromDeepLink(data: Uri?): String? {
        if (data == null) return null
        val parts = data.pathSegments ?: return null
        return parts.lastOrNull()
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
        binding.textEmpty.visibility =
            if (empty) android.view.View.VISIBLE else android.view.View.GONE
        binding.recycler.visibility =
            if (empty) android.view.View.GONE else android.view.View.VISIBLE
    }
}
