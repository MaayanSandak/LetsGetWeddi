package com.example.letsgetweddi.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.R
import com.example.letsgetweddi.adapters.ReviewAdapter
import com.example.letsgetweddi.data.DbPaths
import com.example.letsgetweddi.data.FirebaseRefs
import com.example.letsgetweddi.databinding.ActivityProviderDetailsBinding
import com.example.letsgetweddi.model.Review
import com.example.letsgetweddi.model.Supplier
import com.example.letsgetweddi.ui.gallery.GalleryFragment
import com.example.letsgetweddi.utils.RoleManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class ProviderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProviderDetailsBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance() }
    private var supplierId: String? = null
    private var supplier: Supplier? = null
    private val reviews: MutableList<Pair<String, String>> = mutableListOf()
    private lateinit var reviewsAdapter: ReviewAdapter

    private var favRef: DatabaseReference? = null
    private var favListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supplierId = intent.getStringExtra("supplierId")

        setupToolbar()
        setupLists()
        wireActions()

        loadSupplier()
        observeFavorite()
        loadReviews()
        loadAvailabilityHint()
        mountInlineGalleryIfPossible()
    }

    override fun onResume() {
        super.onResume()
        observeFavorite()
        loadAvailabilityHint()
        mountInlineGalleryIfPossible()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.toolbarNavBack.visibility = View.GONE
    }

    private fun setupLists() {
        reviewsAdapter = ReviewAdapter(reviews)
        binding.recyclerReviews.layoutManager = LinearLayoutManager(this)
        binding.recyclerReviews.adapter = reviewsAdapter
    }

    private fun wireActions() {
        binding.buttonWhatsApp.setOnClickListener {
            val phone = supplier?.phone ?: return@setOnClickListener
            val uri = Uri.parse("https://wa.me/${phone.replace("+", "")}")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        binding.buttonChat.setOnClickListener {
            val peer = supplierId ?: return@setOnClickListener
            val intent = Intent(this, com.example.letsgetweddi.ui.chat.ChatActivity::class.java)
                .putExtra("otherUserId", peer)
                .putExtra("otherUserName", supplier?.name ?: "")
            startActivity(intent)
        }
        binding.buttonFav.setOnClickListener { toggleFavorite() }

        binding.buttonEdit.setOnClickListener {
            val id = supplierId ?: return@setOnClickListener
            startActivity(
                Intent(this, com.example.letsgetweddi.ui.supplier.SupplierEditActivity::class.java)
                    .putExtra("supplierId", id)
            )
        }

        binding.buttonManageGallery.setOnClickListener {
            val id = supplierId ?: return@setOnClickListener
            startActivity(
                Intent(this, com.example.letsgetweddi.ui.gallery.GalleryManageActivity::class.java)
                    .setData(Uri.parse("letsgetweddi://gallery/manage/$id"))
            )
        }

        binding.buttonManageAvailability.setOnClickListener {
            val id = supplierId ?: return@setOnClickListener
            startActivity(
                Intent(
                    this,
                    com.example.letsgetweddi.ui.supplier.SupplierCalendarActivity::class.java
                )
                    .setData(Uri.parse("letsgetweddi://availability/$id"))
                    .putExtra("supplierId", id)
            )
        }
    }

    private fun loadSupplier() {
        val id = supplierId ?: return
        val primary = db.getReference(DbPaths.supplier(id))
        primary.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    supplier = Supplier.fromSnapshot(snapshot)
                    render()
                } else {
                    db.getReference("suppliers/$id")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(s2: DataSnapshot) {
                                supplier = Supplier.fromSnapshot(s2)
                                render()
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun render() {
        val s = supplier ?: return
        title = s.name ?: getString(R.string.supplier_name)

        binding.textName.text = s.name.orEmpty()
        binding.textLocation.text = s.location.orEmpty()
        binding.textDescription.text = s.description.orEmpty()

        val img = s.imageUrl.orEmpty().trim()
        if (img.isNotBlank()) {
            Picasso.get().load(img).into(binding.imageHeader)
        } else {
            binding.imageHeader.setImageDrawable(null)
        }

        RoleManager.isSupplier(this) { isSupplier, mySupplierId ->
            val canEdit = isSupplier && !mySupplierId.isNullOrBlank() && mySupplierId == s.id
            binding.manageRow.visibility = if (canEdit) View.VISIBLE else View.GONE
        }

        binding.buttonFav.visibility = View.VISIBLE
        binding.buttonChat.visibility = View.VISIBLE
        binding.buttonWhatsApp.visibility = View.VISIBLE

        mountInlineGalleryIfPossible()
        loadAvailabilityHint()
    }

    private fun observeFavorite() {
        val sId = supplierId ?: return
        val uid = auth.currentUser?.uid ?: return
        favListener?.let { favRef?.removeEventListener(it) }
        favRef = FirebaseRefs.favorite(uid, sId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateFavIcon(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        favRef!!.addValueEventListener(listener)
        favListener = listener
        updateFavIcon(false)
    }

    private fun updateFavIcon(isFav: Boolean) {
        val btn = binding.buttonFav as ImageButton
        btn.setImageResource(if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
        btn.contentDescription =
            getString(if (isFav) R.string.remove_from_favorites else R.string.add_to_favorites)
    }

    private fun toggleFavorite() {
        val sId = supplierId ?: return
        val uid = auth.currentUser?.uid ?: return
        val ref = FirebaseRefs.favorite(uid, sId)
        ref.get().addOnSuccessListener { snap ->
            if (snap.exists()) {
                ref.removeValue()
            } else {
                val s = supplier ?: return@addOnSuccessListener
                ref.setValue(s)
            }
        }
    }

    private fun loadReviews() {
        val sId = supplierId ?: return
        val ref = FirebaseRefs.reviews(sId)
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var sum = 0f
                var count = 0
                reviews.clear()
                for (child in snapshot.children) {
                    val r = child.getValue(Review::class.java) ?: continue
                    if (!r.comment.isNullOrBlank()) {
                        reviews.add((r.name ?: "") to r.comment!!)
                    }
                    if (r.rating > 0f) {
                        sum += r.rating
                        count += 1
                    }
                }
                binding.textRatingCount.text = count.toString()
                binding.textRatingAvg.text =
                    if (count > 0) String.format("%.1f", sum / count) else "0.0"
                reviewsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadAvailabilityHint() {
        val sId = supplierId ?: return
        FirebaseRefs.availability(sId).get().addOnSuccessListener { snap ->
            binding.textAvailability.visibility =
                if (snap.hasChildren()) View.GONE else View.VISIBLE
        }
    }

    private fun mountInlineGalleryIfPossible() {
        val sId = supplierId ?: return
        if (supportFragmentManager.findFragmentById(R.id.galleryContainer) == null) {
            val f = GalleryFragment.newInstance(sId)
            supportFragmentManager.beginTransaction()
                .replace(R.id.galleryContainer, f)
                .commitAllowingStateLoss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        favListener?.let { favRef?.removeEventListener(it) }
        favListener = null
        favRef = null
    }
}
