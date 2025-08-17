package com.example.letsgetweddi.ui.supplier

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.letsgetweddi.R
import com.example.letsgetweddi.utils.RoleManager
import com.example.letsgetweddi.utils.UiPermissions
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso

class EditSupplierFragment : Fragment() {

    private lateinit var imageSupplier: ImageView
    private lateinit var buttonSelectImage: Button
    private lateinit var buttonSave: Button
    private lateinit var editName: EditText
    private lateinit var editDescription: EditText
    private lateinit var editLocation: EditText
    private lateinit var editCategory: EditText
    private lateinit var editPhone: EditText

    private var supplierId: String? = null
    private var canEdit: Boolean = false
    private var pickedImage: Uri? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_edit_supplier, container, false)

        imageSupplier = view.findViewById(R.id.imageSupplier)
        buttonSelectImage = view.findViewById(R.id.buttonSelectImage)
        buttonSave = view.findViewById(R.id.buttonSaveSupplier)
        editName = view.findViewById(R.id.editName)
        editDescription = view.findViewById(R.id.editDescription)
        editLocation = view.findViewById(R.id.editLocation)
        editCategory = view.findViewById(R.id.editCategory)
        editPhone = view.findViewById(R.id.editPhone)

        RoleManager.load(requireContext()) { _, sid ->
            supplierId = sid
            loadSupplierData()
            applyUiPermissions()
        }

        buttonSelectImage.setOnClickListener {
            if (!canEdit) return@setOnClickListener
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 1001)
        }

        buttonSave.setOnClickListener {
            if (!canEdit) return@setOnClickListener
            saveSupplier()
        }

        return view
    }

    private fun applyUiPermissions() {
        val id = supplierId ?: return
        UiPermissions.checkOwner(requireContext(), id) { owner ->
            canEdit = owner
            buttonSelectImage.visibility = if (owner) View.VISIBLE else View.GONE
            buttonSave.visibility = if (owner) View.VISIBLE else View.GONE
            editName.isEnabled = owner
            editPhone.isEnabled = owner
            editLocation.isEnabled = owner
            editDescription.isEnabled = owner
            editCategory.isEnabled = owner
        }
    }

    private fun loadSupplierData() {
        val id = supplierId ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Suppliers").child(id)
        ref.get().addOnSuccessListener { s ->
            val name = s.child("name").value?.toString() ?: ""
            val description = s.child("description").value?.toString() ?: ""
            val location = s.child("location").value?.toString() ?: ""
            val category = s.child("category").value?.toString() ?: ""
            val phone = s.child("phone").value?.toString() ?: ""
            val imageUrl = s.child("imageUrl").value?.toString() ?: ""

            editName.setText(name)
            editDescription.setText(description)
            editLocation.setText(location)
            editCategory.setText(category)
            editPhone.setText(phone)

            if (imageUrl.isNotEmpty()) {
                Picasso.get().load(imageUrl).placeholder(R.drawable.rounded_card_placeholder).into(imageSupplier)
            } else {
                imageSupplier.setImageResource(R.drawable.rounded_card_placeholder)
            }
        }
    }

    private fun saveSupplier() {
        val id = supplierId ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Suppliers").child(id)
        val updates = hashMapOf<String, Any>(
            "name" to (editName.text?.toString() ?: ""),
            "description" to (editDescription.text?.toString() ?: ""),
            "location" to (editLocation.text?.toString() ?: ""),
            "category" to (editCategory.text?.toString() ?: ""),
            "phone" to (editPhone.text?.toString() ?: "")
        )
        ref.updateChildren(updates)
    }

    @Deprecated("Deprecated in AndroidX")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            pickedImage = data?.data
            pickedImage?.let { imageSupplier.setImageURI(it) }
        }
    }
}
