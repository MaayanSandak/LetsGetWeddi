package com.example.letsgetweddi

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.example.letsgetweddi.databinding.ActivityMainBinding
import com.example.letsgetweddi.model.Category
import com.example.letsgetweddi.ui.LoginActivity
import com.example.letsgetweddi.ui.categories.SuppliersListFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private var role: String = "client"
    private var supplierId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.app_name,
            R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Build drawer immediately (default client). If you later cache role – it will rebuild.
        buildMenuForRole(role)
        openStartForRole(role)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean = super.onCreateOptionsMenu(menu)

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val handled = when (item.itemId) {
            ID_MENU_HOME -> { openHome(); true }
            ID_MENU_PROFILE -> { openIfExists("com.example.letsgetweddi.ui.ProfileFragment", "profile"); true }

            ID_MENU_SUPPLIERS_ALL -> { openAllSuppliers(); true }
            ID_MENU_SUPPLIERS_DJS -> { openSuppliers(Category.DJS); true }
            ID_MENU_SUPPLIERS_PHOTOGRAPHERS -> { openSuppliers(Category.PHOTOGRAPHERS); true }
            ID_MENU_SUPPLIERS_DRESSES -> { openSuppliers(Category.DRESSES); true }
            ID_MENU_SUPPLIERS_SUITS -> { openSuppliers(Category.SUITS); true }
            ID_MENU_SUPPLIERS_HAIR -> { openSuppliers(Category.HAIR_MAKEUP); true }
            ID_MENU_SUPPLIERS_HALLS -> { openSuppliers(Category.HALLS); true }

            ID_MENU_FAVORITES -> { openIfExists("com.example.letsgetweddi.ui.favorites.FavoritesFragment", "favorites"); true }
            ID_MENU_TIPS -> { openIfExists("com.example.letsgetweddi.ui.categories.TipsAndChecklistFragment", "tips_checklist"); true }
            ID_MENU_CHAT -> { startIfExists("com.example.letsgetweddi.ui.chat.ConversationsActivity") }
            ID_MENU_SUPPLIER_GALLERY -> { startIfExists("com.example.letsgetweddi.ui.gallery.GalleryManageActivity") }
            ID_MENU_SUPPLIER_AVAILABILITY -> { startIfExists("com.example.letsgetweddi.ui.supplier.SupplierCalendarActivity") }

            ID_MENU_LOGOUT -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> false
        }
        binding.drawerLayout.closeDrawer(binding.navView)
        return handled
    }

    private fun openHome() {
        openIfExists("com.example.letsgetweddi.ui.home.HomeFragment", "home")
    }

    private fun openAllSuppliers() {
        val frag = com.example.letsgetweddi.ui.providers.AllSuppliersFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment_content_main, frag, "suppliers_all")
            .commit()
        setContainerVisible(true)
        title = getString(R.string.suppliers)
    }

    private fun openSuppliers(category: Category) {
        val frag = SuppliersListFragment.newInstance(category)
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment_content_main, frag, "suppliers_${category.id}")
            .commit()
        setContainerVisible(true)
        title = category.title
    }

    private fun openIfExists(className: String, tag: String): Boolean {
        return try {
            val fragClass = Class.forName(className)
            val frag = supportFragmentManager.fragmentFactory.instantiate(classLoader, fragClass.name)
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, frag, tag)
                .commit()
            setContainerVisible(true)
            true
        } catch (_: Throwable) {
            setContainerVisible(false)
            false
        }
    }

    private fun startIfExists(className: String): Boolean {
        return try {
            val c = Class.forName(className)
            startActivity(Intent(this, c))
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun buildMenuForRole(r: String) {
        val menu = binding.navView.menu
        menu.clear()

        menu.add(Menu.NONE, ID_MENU_HOME, Menu.NONE, getString(R.string.home_page))
        menu.add(Menu.NONE, ID_MENU_PROFILE, Menu.NONE, getString(R.string.profile))

        // ----- Suppliers header + items (always visible) -----
        menu.add(Menu.NONE, ID_HEADER_SUPPLIERS, Menu.NONE, getString(R.string.suppliers)).apply {
            isEnabled = false // acts as a non-clickable title row
        }
        menu.add(Menu.NONE, ID_MENU_SUPPLIERS_ALL, Menu.NONE, getString(R.string.suppliers_all))
        menu.add(Menu.NONE, ID_MENU_SUPPLIERS_DJS, Menu.NONE, getString(R.string.category_djs))
        menu.add(Menu.NONE, ID_MENU_SUPPLIERS_PHOTOGRAPHERS, Menu.NONE, getString(R.string.category_photographers))
        menu.add(Menu.NONE, ID_MENU_SUPPLIERS_DRESSES, Menu.NONE, getString(R.string.category_dresses))
        menu.add(Menu.NONE, ID_MENU_SUPPLIERS_SUITS, Menu.NONE, getString(R.string.category_suits))
        menu.add(Menu.NONE, ID_MENU_SUPPLIERS_HAIR, Menu.NONE, getString(R.string.category_hair_makeup))
        menu.add(Menu.NONE, ID_MENU_SUPPLIERS_HALLS, Menu.NONE, getString(R.string.category_halls))

        if (r == "supplier") {
            menu.add(Menu.NONE, ID_MENU_SUPPLIER_GALLERY, Menu.NONE, getString(R.string.supplier_gallery_manage))
            menu.add(Menu.NONE, ID_MENU_SUPPLIER_AVAILABILITY, Menu.NONE, getString(R.string.supplier_availability))
        } else {
            menu.add(Menu.NONE, ID_MENU_FAVORITES, Menu.NONE, getString(R.string.favorites))
            menu.add(Menu.NONE, ID_MENU_TIPS, Menu.NONE, getString(R.string.menu_tips_checklist))
        }

        menu.add(Menu.NONE, ID_MENU_CHAT, Menu.NONE, getString(R.string.menu_messages))
        menu.add(Menu.NONE, ID_MENU_LOGOUT, Menu.NONE, getString(R.string.logout))
    }

    private fun openStartForRole(r: String) {
        if (!openIfExists("com.example.letsgetweddi.ui.home.HomeFragment", "home")) {
            setContainerVisible(false)
        }
    }

    private fun setContainerVisible(visible: Boolean) {
        val container = findViewById<View>(R.id.nav_host_fragment_content_main)
        container?.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    companion object {
        private const val ID_MENU_HOME = 10_000
        private const val ID_MENU_PROFILE = 10_010

        private const val ID_HEADER_SUPPLIERS = 10_015
        private const val ID_MENU_SUPPLIERS_ALL = 10_020
        private const val ID_MENU_SUPPLIERS_DJS = 10_021
        private const val ID_MENU_SUPPLIERS_PHOTOGRAPHERS = 10_022
        private const val ID_MENU_SUPPLIERS_DRESSES = 10_023
        private const val ID_MENU_SUPPLIERS_SUITS = 10_024
        private const val ID_MENU_SUPPLIERS_HAIR = 10_025
        private const val ID_MENU_SUPPLIERS_HALLS = 10_026

        private const val ID_MENU_FAVORITES = 10_030
        private const val ID_MENU_TIPS = 10_040
        private const val ID_MENU_CHAT = 10_050

        private const val ID_MENU_SUPPLIER_GALLERY = 20_020
        private const val ID_MENU_SUPPLIER_AVAILABILITY = 20_030

        private const val ID_MENU_LOGOUT = 99_999
    }
}
