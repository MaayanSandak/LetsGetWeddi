package com.example.letsgetweddi.ui.supplier

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.letsgetweddi.databinding.ActivitySupplierDashboardBinding
import com.example.letsgetweddi.utils.RoleManager
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso

class SupplierDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupplierDashboardBinding
    private var supplierId: String? = null
    private var imageUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupplierDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        RoleManager.load(this) { role, sid ->
            val isSupplier = role == "supplier" && !sid.isNullOrBlank()
            if (!isSupplier) {
                finish()
                return@load
            }
            supplierId = sid
            loadSupplier()
        }
    }

    private fun loadSupplier() {
        val id = supplierId ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Suppliers").child(id)
        ref.get().addOnSuccessListener { s ->
            val name = s.child("name").value?.toString() ?: ""
            val description = s.child("description").value?.toString() ?: ""
            val location = s.child("location").value?.toString() ?: ""
            val phone = s.child("phone").value?.toString() ?: ""
            imageUrl = s.child("imageUrl").value?.toString() ?: ""

            binding.editName.setText(name)
            binding.editDescription.setText(description)
            binding.editLocation.setText(location)
            binding.editPhone.setText(phone)

            if (imageUrl.isNotEmpty()) {
                Picasso.get().load(imageUrl).into(binding.imageSupplier)
            }
        }
    }
}
