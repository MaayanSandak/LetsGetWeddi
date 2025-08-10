package com.example.letsgetweddi.ui.supplier

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.letsgetweddi.R
import com.example.letsgetweddi.utils.UiPermissions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class EditSupplierFragment : Fragment() {

    private lateinit var imageSupplier: ImageView
    private lateinit var buttonSelectImage: Button
    private lateinit var editName: EditText
    private lateinit var editDescription: EditText
    private lateinit var editLocation: EditText
    private lateinit var editCategory: EditText
    private lateinit var editPhone: EditText
    private lateinit var buttonSave: Button

    private var imageUri: Uri? = null
    private var supplierId: String? = null
    private var canEdit: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supplierId = arguments?.getString("supplierId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_edit_supplier, container, false)

        imageSupplier = view.findViewById(R.id.imageSupplier)
        buttonSelectImage = view.findViewById(R.id.buttonSelectImage)
        editName = view.findViewById(R.id.editName)
        editDescription = view.findViewById(R.id.editDescription)
        editLocation = view.findViewById(R.id.editLocation)
        editCategory = view.findViewById(R.id.editCategory)
        editPhone = view.findViewById(R.id.editPhone)
        buttonSave = view.findViewById(R.id.buttonSaveSupplier)

        loadSupplierData()
        applyPermissions()

        buttonSelectImage.setOnClickListener {
            if (!canEdit) return@setOnClickListener
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 1001)
        }

        buttonSave.setOnClickListener {
            if (canEdit) saveSupplier() else Toast.makeText(requireContext(), "View-only", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun applyPermissions() {
        val id = supplierId ?: return
        UiPermissions.checkOwner(id) { owner ->
            canEdit = owner
            buttonSelectImage.visibility = if (owner) View.VISIBLE else View.GONE
            buttonSave.visibility = if (owner) View.VISIBLE else View.GONE

            editName.isEnabled = owner
            editDescription.isEnabled = owner
            editLocation.isEnabled = owner
            editCategory.isEnabled = owner
            editPhone.isEnabled = owner
        }
    }

    private fun loadSupplierData() {
        val id = supplierId ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Suppliers").child(id)
        ref.get().addOnSuccessListener { s ->
            val name = s.child("name").value?.toString() ?: ""
            val desc = s.child("description").value?.toString() ?: ""
            val loc = s.child("location").value?.toString() ?: ""
            val cat = s.child("category").value?.toString() ?: ""
            val phone = s.child("phone").value?.toString() ?: ""
            val image = s.child("imageUrl").value?.toString() ?: ""

            editName.setText(name)
            editDescription.setText(desc)
            editLocation.setText(loc)
            editCategory.setText(cat)
            editPhone.setText(phone)
            if (image.isNotBlank()) {
                Picasso.get().load(image).fit().centerCrop().into(imageSupplier)
            }
        }
    }

    private fun saveSupplier() {
        val id = supplierId ?: return
        val map = hashMapOf<String, Any>(
            "name" to editName.text.toString().trim(),
            "description" to editDescription.text.toString().trim(),
            "location" to editLocation.text.toString().trim(),
            "category" to editCategory.text.toString().trim(),
            "phone" to editPhone.text.toString().trim()
        )

        FirebaseDatabase.getInstance().getReference("Suppliers")
            .child(id).updateChildren(map)
            .addOnSuccessListener { Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show() }

        val uri = imageUri ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val path = "supplier_headers/$uid/$id/header.jpg"
        FirebaseStorage.getInstance().reference.child(path)
            .putFile(uri)
            .addOnSuccessListener {
                it.storage.downloadUrl.addOnSuccessListener { url ->
                    FirebaseDatabase.getInstance().getReference("Suppliers")
                        .child(id).child("imageUrl").setValue(url.toString())
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            imageSupplier.setImageURI(imageUri)
        }
    }

    companion object {
        fun newInstance(supplierId: String): EditSupplierFragment {
            val f = EditSupplierFragment()
            f.arguments = Bundle().apply { putString("supplierId", supplierId) }
            return f
        }
    }
}
