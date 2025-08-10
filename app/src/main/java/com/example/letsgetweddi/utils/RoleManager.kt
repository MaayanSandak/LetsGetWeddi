package com.example.letsgetweddi.utils

import com.example.letsgetweddi.data.FirebaseRefs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object RoleManager {

    interface Callback {
        fun onRoleLoaded(role: String, supplierId: String?)
        fun onNoUser()
    }

    fun load(callback: Callback) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            callback.onNoUser()
            return
        }
        val uid = user.uid
        val ref = FirebaseRefs.users().child(uid)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val role = snapshot.child("role").getValue(String::class.java) ?: "client"
                val supplierId = snapshot.child("supplierId").getValue(String::class.java)
                callback.onRoleLoaded(role, supplierId)
            }
            override fun onCancelled(error: DatabaseError) {
                callback.onRoleLoaded("client", null)
            }
        })
    }
}
