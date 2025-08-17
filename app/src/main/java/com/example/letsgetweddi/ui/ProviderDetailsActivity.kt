package com.example.letsgetweddi.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.letsgetweddi.data.DbPaths
import com.example.letsgetweddi.databinding.ActivityProviderDetailsBinding
import com.example.letsgetweddi.model.Supplier
import com.example.letsgetweddi.utils.RoleManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProviderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProviderDetailsBinding
    private lateinit var db: DatabaseReference
    private val auth = FirebaseAuth.getInstance()
    private var supplierId: String? = null
    private var supplier: Supplier? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supplierId = intent.getStringExtra("supplierId")
        db = FirebaseDatabase.getInstance().getReference(DbPaths.SUPPLIERS)

        setupToolbar()
        loadSupplier()
        bindActions()
    }

    private fun setupToolbar() {
        binding.toolbarNavBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun loadSupplier() {
        val id = supplierId ?: return
        db.child(id).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                supplier = snapshot.getValue(Supplier::class.java)?.copy(
                    id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key
                )
                render()
            }
            override fun onCancelled(error: DatabaseError) { /* no-op */ }
        })
    }

    private fun render() {
        val s = supplier ?: return

        // Bind basic fields (match XML ids)
        binding.textName.text = s.name ?: ""
        binding.textLocation.text = s.location ?: ""
        binding.textDescription.text = s.description ?: ""

        // TODO: Load s.imageUrl into binding.imageHeader using your image loader

        // Decide what the current user can edit – via RoleManager (async)
        val uid = auth.currentUser?.uid
        RoleManager.isSupplier(this) { isSupplier, mySupplierId ->
            val canEdit = isSupplier && (mySupplierId != null && mySupplierId == s.id)

            binding.buttonEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
            binding.buttonManageGallery.visibility = if (canEdit) View.VISIBLE else View.GONE
            binding.buttonManageAvailability.visibility = if (canEdit) View.VISIBLE else View.GONE
        }

        // Always visible
        binding.buttonFav.visibility = View.VISIBLE
        binding.buttonChat.visibility = View.VISIBLE
        binding.buttonWhatsApp.visibility = View.VISIBLE
    }

    private fun bindActions() {
        binding.buttonWhatsApp.setOnClickListener {
            val phone = supplier?.phone ?: return@setOnClickListener
            val uri = Uri.parse("https://wa.me/${phone.replace("+","")}")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        binding.buttonChat.setOnClickListener {
            val peer = supplierId ?: return@setOnClickListener
            val intent = Intent(this, com.example.letsgetweddi.ui.chat.ChatActivity::class.java)
            intent.putExtra("peerId", peer)
            startActivity(intent)
        }
        binding.buttonFav.setOnClickListener { toggleFavorite() }

        binding.buttonManageGallery.setOnClickListener {
            startActivity(
                Intent(this, com.example.letsgetweddi.ui.gallery.GalleryManageActivity::class.java)
                    .putExtra("supplierId", supplierId)
            )
        }
        binding.buttonManageAvailability.setOnClickListener {
            startActivity(
                Intent(this, com.example.letsgetweddi.ui.supplier.SupplierCalendarActivity::class.java)
                    .putExtra("supplierId", supplierId)
            )
        }
             binding.buttonEdit.setOnClickListener {
            startActivity(
                Intent(this, com.example.letsgetweddi.ui.supplier.SupplierEditActivity::class.java)
                    .putExtra("supplierId", supplierId)
            )
        }
    }

    private fun toggleFavorite() {
        val sId = supplierId ?: return
        val uid = auth.currentUser?.uid ?: return
        val favRef = FirebaseDatabase.getInstance()
            .getReference("${DbPaths.FAVORITES}/$uid/$sId")
        favRef.get().addOnSuccessListener { snap ->
            if (snap.exists()) favRef.removeValue() else favRef.setValue(true)
        }
    }
}
