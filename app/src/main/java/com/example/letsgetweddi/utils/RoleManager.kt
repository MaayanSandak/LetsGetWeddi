package com.example.letsgetweddi.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object RoleManager {
    private const val PREF = "role_prefs"
    private const val KEY_ROLE = "role"
    private const val KEY_SUPPLIER_ID = "supplier_id"

    fun cache(context: Context, role: String?, supplierId: String?) {
        prefs(context).edit()
            .putString(KEY_ROLE, role)
            .putString(KEY_SUPPLIER_ID, supplierId)
            .apply()
    }

    fun getCachedRole(context: Context): String? = prefs(context).getString(KEY_ROLE, null)
    fun getCachedSupplierId(context: Context): String? = prefs(context).getString(KEY_SUPPLIER_ID, null)

    fun load(context: Context, onResult: (role: String?, supplierId: String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { onResult(null, null); return }
        FirebaseDatabase.getInstance().getReference("users").child(uid).get()
            .addOnSuccessListener { s ->
                val role = s.child("clientType").getValue(String::class.java) ?: "client"
                val supplierId = s.child("supplierId").getValue(String::class.java)
                cache(context, role, supplierId)
                onResult(role, supplierId)
            }
            .addOnFailureListener { onResult(null, null) }
    }

    fun isSupplier(context: Context, onResult: (Boolean, String?) -> Unit) {
        val cached = getCachedRole(context)
        val sid = getCachedSupplierId(context)
        if (cached != null) { onResult(cached == "supplier", sid); return }
        load(context) { role, supplierId -> onResult(role == "supplier", supplierId) }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}
