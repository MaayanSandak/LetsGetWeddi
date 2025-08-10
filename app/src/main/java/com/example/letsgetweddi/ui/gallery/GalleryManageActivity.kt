package com.example.letsgetweddi.ui.gallery

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.letsgetweddi.R
import com.example.letsgetweddi.utils.UiPermissions

class GalleryManageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery_manage)

        val supplierId = extractSupplierIdFromDeepLink(intent?.data)
        if (supplierId.isNullOrBlank()) {
            finish()
            return
        }

        UiPermissions.checkOwner(this, supplierId) { isOwner ->
            if (!isOwner) {
                Toast.makeText(this, "View-only", Toast.LENGTH_SHORT).show()
                finish()
                return@checkOwner
            }
        }
    }

    private fun extractSupplierIdFromDeepLink(data: Uri?): String? {
        if (data == null) return null
        val segments = data.pathSegments ?: return null
        return segments.lastOrNull()
    }
}
