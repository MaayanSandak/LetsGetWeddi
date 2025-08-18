package com.example.letsgetweddi

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.example.letsgetweddi.databinding.ActivityMainBinding
import com.example.letsgetweddi.model.Category
import com.example.letsgetweddi.ui.LoginActivity
import com.example.letsgetweddi.ui.categories.SuppliersListFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    private var role: String = "client"
    private var supplierId: String? = null
    private var suppliersExpanded = false

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        suppliersExpanded = savedInstanceState?.getBoolean(STATE_SUPPLIERS_EXPANDED, false) ?: false

        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.app_name, R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        // Wire header action view after menu is available
        wireSuppliersHeaderAction()

        // Apply initial visibility
        applySuppliersExpandedUI()

        val uid = auth.currentUser?.uid
        if (uid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        applyRoleUI("client")
        verifyRoleAndRebuild(uid)
        openHome()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_SUPPLIERS_EXPANDED, suppliersExpanded)
        super.onSaveInstanceState(outState)
    }

    private fun verifyRoleAndRebuild(uid: String) {
        db.getReference("Users").child(uid).get().addOnSuccessListener { snap ->
            val dbRole = snap.child("role").getValue(String::class.java).orEmpty()
            val dbSupplierId = snap.child("supplierId").getValue(String::class.java)

            if (dbRole == "supplier" && !dbSupplierId.isNullOrBlank()) {
                db.getReference("Suppliers").child(dbSupplierId).get().addOnSuccessListener { s2 ->
                    if (s2.exists()) {
                        role = "supplier"; supplierId = dbSupplierId
                    } else {
                        role = "client"; supplierId = null
                    }
                    applyRoleUI(role); applySuppliersExpandedUI(); renderHeaderArrow()
                }
            } else {
                role = "client"; supplierId = null
                applyRoleUI(role); applySuppliersExpandedUI(); renderHeaderArrow()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean = super.onCreateOptionsMenu(menu)

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val handled = when (item.itemId) {
            // Children handled normally (we keep drawer open until user selects)
            R.id.menu_home -> {
                openHome(); true
            }

            R.id.menu_profile -> {
                openIfExists(
                    "com.example.letsgetweddi.ui.ProfileFragment",
                    "profile",
                    getString(R.string.profile)
                ); true
            }

            R.id.menu_suppliers_all -> {
                openAllSuppliers(); true
            }

            R.id.menu_suppliers_djs -> {
                openSuppliers(Category.DJS); true
            }

            R.id.menu_suppliers_photographers -> {
                openSuppliers(Category.PHOTOGRAPHERS); true
            }

            R.id.menu_suppliers_dresses -> {
                openSuppliers(Category.DRESSES); true
            }

            R.id.menu_suppliers_suits -> {
                openSuppliers(Category.SUITS); true
            }

            R.id.menu_suppliers_hair -> {
                openSuppliers(Category.HAIR_MAKEUP); true
            }

            R.id.menu_suppliers_halls -> {
                openSuppliers(Category.HALLS); true
            }

            R.id.menu_favorites -> {
                openIfExists(
                    "com.example.letsgetweddi.ui.favorites.FavoritesFragment",
                    "favorites",
                    getString(R.string.favorites)
                ); true
            }

            R.id.menu_tips -> {
                openIfExists(
                    "com.example.letsgetweddi.ui.categories.TipsAndChecklistFragment",
                    "tips_checklist",
                    getString(R.string.menu_tips_checklist)
                ); true
            }

            R.id.menu_chat -> {
                startIfExists("com.example.letsgetweddi.ui.chat.ConversationsActivity")
            }

            R.id.menu_supplier_gallery -> {
                startIfExists("com.example.letsgetweddi.ui.gallery.GalleryManageActivity")
            }

            R.id.menu_supplier_availability -> {
                startIfExists("com.example.letsgetweddi.ui.supplier.SupplierCalendarActivity")
            }

            R.id.menu_logout -> {
                auth.signOut(); startActivity(
                    Intent(
                        this,
                        LoginActivity::class.java
                    )
                ); finish(); true
            }

            else -> false
        }

        // Close only for real navigation (selecting a child), not for header toggle (header handled in wireSuppliersHeaderAction)
        if (handled) binding.drawerLayout.closeDrawer(binding.navView)
        return handled
    }

    private fun wireSuppliersHeaderAction() {
        val item = binding.navView.menu.findItem(R.id.menu_suppliers_header)
        val row = item.actionView
        val titleView = row?.findViewById<TextView>(R.id.headerTitle)
        val arrowView = row?.findViewById<ImageView>(R.id.headerArrow)

        row?.setOnClickListener {
            suppliersExpanded = !suppliersExpanded
            applySuppliersExpandedUI()
            renderHeaderArrow()
        }
        // Initial arrow state
        renderHeaderArrow()
    }

    private fun renderHeaderArrow() {
        val row = binding.navView.menu.findItem(R.id.menu_suppliers_header).actionView
        val arrowView = row?.findViewById<ImageView>(R.id.headerArrow)
        arrowView?.rotation = if (suppliersExpanded) 180f else 0f
    }

    private fun applyRoleUI(r: String) {
        val m = binding.navView.menu
        val isSupplier = r == "supplier"
        m.findItem(R.id.menu_favorites)?.isVisible = !isSupplier
        m.findItem(R.id.menu_tips)?.isVisible = !isSupplier
        m.findItem(R.id.menu_supplier_gallery)?.isVisible = isSupplier
        m.findItem(R.id.menu_supplier_availability)?.isVisible = isSupplier
    }

    private fun applySuppliersExpandedUI() {
        val m = binding.navView.menu
        m.setGroupVisible(R.id.group_suppliers_children, suppliersExpanded)
        binding.navView.invalidate()
        binding.navView.requestLayout()
    }

    private fun openHome() {
        val ok = openIfExists(
            className = "com.example.letsgetweddi.ui.home.HomeFragment",
            tag = "home",
            titleText = getString(R.string.home_page)
        )
        if (!ok) setContainerVisible(false)
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

    private fun openIfExists(className: String, tag: String, titleText: String? = null): Boolean {
        return try {
            val fragClass = Class.forName(className)
            val frag =
                supportFragmentManager.fragmentFactory.instantiate(classLoader, fragClass.name)
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, frag, tag)
                .commit()
            setContainerVisible(true)
            if (!titleText.isNullOrEmpty()) title = titleText
            true
        } catch (_: Throwable) {
            setContainerVisible(false)
            false
        }
    }

    private fun startIfExists(className: String): Boolean {
        return try {
            startActivity(Intent(this, Class.forName(className))); true
        } catch (_: Throwable) {
            false
        }
    }

    private fun setContainerVisible(visible: Boolean) {
        val container = findViewById<View>(R.id.nav_host_fragment_content_main)
        container?.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    companion object {
        private const val STATE_SUPPLIERS_EXPANDED = "state_suppliers_expanded"
    }
}
