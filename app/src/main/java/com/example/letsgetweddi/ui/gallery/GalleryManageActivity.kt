package com.example.letsgetweddi.ui.gallery

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.letsgetweddi.R
import com.example.letsgetweddi.utils.RoleManager

class GalleryManageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery_manage)

        val supplierId = extractSupplierIdFromDeepLink(intent?.data)
        RoleManager.isSupplier(this) { isSupplier, cachedSid ->
            val owner = isSupplier && supplierId != null && supplierId == (cachedSid ?: supplierId)
            if (!owner) {
                Toast.makeText(this, "View-only", Toast.LENGTH_SHORT).show()
                finish()
                return@isSupplier
            }
        }
    }

    private fun extractSupplierIdFromDeepLink(data: Uri?): String? {
        val p = data?.path ?: return null
        val parts = p.trim('/').split('/')
        return parts.lastOrNull()
    }
}
