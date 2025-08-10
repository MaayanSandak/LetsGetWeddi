package com.example.letsgetweddi.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import com.example.letsgetweddi.R
import com.example.letsgetweddi.databinding.ActivityMainBinding
import com.example.letsgetweddi.ui.favorites.FavoritesFragment
import com.example.letsgetweddi.ui.supplier.SupplierDashboardActivity
import com.example.letsgetweddi.utils.RoleManager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private var currentRole: String = "client"
    private var currentUserSupplierId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        RoleManager.load(object : RoleManager.Callback {
            override fun onRoleLoaded(role: String, supplierId: String?) {
                currentRole = role
                currentUserSupplierId = supplierId
                applyDrawerForRole()
                openDefaultDestination()
            }

            override fun onNoUser() {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        })
    }

    private fun applyDrawerForRole() {
        val menuRes = if (currentRole == "supplier") {
            R.menu.supplier_main_drawer
        } else {
            R.menu.client_main_drawer
        }
        binding.navView.menu.clear()
        binding.navView.inflateMenu(menuRes)
    }

    private fun openDefaultDestination() {
        if (currentRole == "supplier") {
            startActivity(Intent(this, SupplierDashboardActivity::class.java))
            binding.navView.setCheckedItem(R.id.nav_supplier_dashboard)
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, FavoritesFragment())
                .commit()
            binding.navView.setCheckedItem(R.id.nav_favorites)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_favorites -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment_content_main, FavoritesFragment())
                    .addToBackStack(null)
                    .commit()
            }
            R.id.nav_supplier_dashboard -> {
                startActivity(Intent(this, SupplierDashboardActivity::class.java))
            }
            R.id.nav_supplier_gallery -> {
                val id = currentUserSupplierId ?: return true
                startActivity(Intent(Intent.ACTION_VIEW, "letsgetweddi://gallery/manage/$id".toUri()))
            }
            R.id.nav_supplier_availability -> {
                val id = currentUserSupplierId ?: return true
                startActivity(Intent(Intent.ACTION_VIEW, "letsgetweddi://availability/$id".toUri()))
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
