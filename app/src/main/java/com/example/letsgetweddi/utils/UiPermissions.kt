package com.example.letsgetweddi.utils

import android.content.Context

object UiPermissions {

    fun checkOwner(context: Context, supplierId: String?, onResult: (Boolean) -> Unit) {
        if (supplierId.isNullOrBlank()) { onResult(false); return }
        RoleManager.isSupplier(context) { isSupplier, mySupplierId ->
            onResult(isSupplier && !mySupplierId.isNullOrBlank() && mySupplierId == supplierId)
        }
    }
}
