package com.example.letsgetweddi.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.letsgetweddi.R
import com.example.letsgetweddi.databinding.ActivityMainBinding
import com.example.letsgetweddi.utils.RoleManager
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
            this, binding.drawerLayout ,
            R.string.app_name, R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(toggle); toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish(); return
        }

        val cachedRole = RoleManager.getCachedRole(this)
        val cachedSid = RoleManager.getCachedSupplierId(this)
        if (cachedRole != null) {
            role = cachedRole; supplierId = cachedSid
            buildMenuForRole(role); openStartForRole(role)
        } else {
            RoleManager.load(this) { r, sid ->
                role = r ?: "client"; supplierId = sid
                buildMenuForRole(role); openStartForRole(role)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean = super.onCreateOptionsMenu(menu)

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            ID_MENU_SUPPLIERS -> openIfExists("com.example.letsgetweddi.ui.categories.DjsFragment", "suppliers")
            ID_MENU_FAVORITES -> openIfExists("com.example.letsgetweddi.ui.favorites.FavoritesFragment", "favorites")
            ID_MENU_TIPS -> openIfExists("com.example.letsgetweddi.ui.categories.TipsAndChecklistFragment", "tips_checklist")
            ID_MENU_CHECKLIST -> openIfExists("com.example.letsgetweddi.ui.categories.TipsAndChecklistFragment", "tips_checklist")
            ID_MENU_CHAT -> startIfExists("com.example.letsgetweddi.ui.chat.ConversationsActivity")
            ID_MENU_MY_PROFILE -> openIfExists("com.example.letsgetweddi.ui.supplier.ProfileFragment", "supplier_profile")
            ID_MENU_GALLERY -> startDeep("com.example.letsgetweddi.ui.gallery.GalleryManageActivity", "letsgetweddi://gallery/manage/${supplierId ?: ""}")
            ID_MENU_AVAILABILITY -> startDeep("com.example.letsgetweddi.ui.supplier.SupplierCalendarActivity", "letsgetweddi://availability/${supplierId ?: ""}")
            ID_MENU_SUPPLIER_CHAT -> startIfExists("com.example.letsgetweddi.ui.chat.ConversationsActivity")
            ID_MENU_LOGOUT -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
        binding.drawerLayout.closeDrawer(binding.navView)
        return true
    }


    private fun buildMenuForRole(r: String) {
        val menu = binding.navView.menu; menu.clear()
        if (r == "supplier") {
            menu.add(Menu.NONE, ID_MENU_MY_PROFILE, Menu.NONE, "My Profile")
            menu.add(Menu.NONE, ID_MENU_GALLERY, Menu.NONE, "Manage Gallery")
            menu.add(Menu.NONE, ID_MENU_AVAILABILITY, Menu.NONE, "Manage Availability")
            menu.add(Menu.NONE, ID_MENU_SUPPLIER_CHAT, Menu.NONE, "Chat")
            menu.add(Menu.NONE, ID_MENU_LOGOUT, Menu.NONE, "Logout")
        } else {
            menu.add(Menu.NONE, ID_MENU_SUPPLIERS, Menu.NONE, "Suppliers")
            menu.add(Menu.NONE, ID_MENU_FAVORITES, Menu.NONE, "Favorites")
            menu.add(Menu.NONE, ID_MENU_TIPS, Menu.NONE, "Tips & Checklist")
            menu.add(Menu.NONE, ID_MENU_CHAT, Menu.NONE, "Chat")
            menu.add(Menu.NONE, ID_MENU_LOGOUT, Menu.NONE, "Logout")
        }
    }

    private fun openStartForRole(r: String) {
        if (!openIfExists("com.example.letsgetweddi.ui.categories.DjsFragment", "suppliers")) {
            setContainerVisible(false)
        } else {
            setContainerVisible(true)
            binding.toolbar.title = getString(R.string.app_name)
        }
    }

    private fun openIfExists(fragmentClass: String, tag: String): Boolean = try {
        Class.forName(fragmentClass)
        val frag = supportFragmentManager.fragmentFactory.instantiate(classLoader, fragmentClass)
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment_content_main, frag, tag)
            .commitNowAllowingStateLoss()
        true
    } catch (_: Throwable) { false }

    private fun startIfExists(activityClass: String): Boolean = try {
        val clazz = Class.forName(activityClass); startActivity(Intent(this, clazz)); true
    } catch (_: Throwable) { false }

    private fun startDeep(activityClass: String, uri: String): Boolean = try {
        val clazz = Class.forName(activityClass)
        val i = Intent(this, clazz); i.data = Uri.parse(uri); startActivity(i); true
    } catch (_: Throwable) { false }

    private fun setContainerVisible(v: Boolean) {
        findViewById<View>(R.id.nav_host_fragment_content_main)?.isVisible = v
    }

    companion object {
        private const val ID_MENU_SUPPLIERS = 10_001
        private const val ID_MENU_FAVORITES = 10_002
        private const val ID_MENU_TIPS = 10_003
        private const val ID_MENU_CHECKLIST = 10_004
        private const val ID_MENU_CHAT = 10_005
        private const val ID_MENU_MY_PROFILE = 20_001
        private const val ID_MENU_GALLERY = 20_002
        private const val ID_MENU_AVAILABILITY = 20_003
        private const val ID_MENU_SUPPLIER_CHAT = 20_004
        private const val ID_MENU_LOGOUT = 99_999
    }
}
