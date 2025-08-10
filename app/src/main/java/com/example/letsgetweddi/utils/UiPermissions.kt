package com.example.letsgetweddi.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object UiPermissions {
    fun isOwnerOfSupplier(supplierId: String?, onResult: (Boolean) -> Unit) {
        val id = supplierId ?: return onResult(false)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(false)
        val ref = FirebaseDatabase.getInstance().getReference("Suppliers").child(id).child("ownerUid")
        ref.get().addOnSuccessListener { snap ->
            onResult(snap.value?.toString() == uid)
        }.addOnFailureListener { onResult(false) }
    }

    fun getUserRole(onResult: (String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(null)
        val ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
        ref.get().addOnSuccessListener { s -> onResult(s.getValue(String::class.java)) }
            .addOnFailureListener { onResult(null) }
    }
}
