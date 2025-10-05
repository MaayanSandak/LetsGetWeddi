package com.example.letsgetweddi.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.letsgetweddi.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase

    private val FIXED_AVATAR_URL =
        "https://firebasestorage.googleapis.com/v0/b/letsgetweddi.firebasestorage.app/o/users%2FUID_USER_1%2Favatar.jpg?alt=media&token=d3449a06-d267-42ef-9a39-5a4725922c1f"

    private var selectedWeddingDateMillis: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        fun titleCase(s: String): String =
            s.split(" ", "_", ".", "-")
                .filter { it.isNotBlank() }
                .joinToString(" ") { part ->
                    part.lowercase(Locale.getDefault())
                        .replaceFirstChar { c -> c.titlecase(Locale.getDefault()) }
                }

        fun deriveNameFromEmail(email: String?): String {
            if (email.isNullOrBlank()) return ""
            val local = email.substringBefore("@")
            return titleCase(local)
        }

        fun findNameInSnapshot(snapshot: DataSnapshot): String? {
            val direct = listOf("fullName", "name", "displayName")
                .asSequence()
                .mapNotNull { key -> snapshot.child(key).getValue(String::class.java) }
                .firstOrNull { !it.isNullOrBlank() }
            if (!direct.isNullOrBlank()) return direct

            val q: ArrayDeque<DataSnapshot> = ArrayDeque()
            snapshot.children.forEach { q.add(it) }
            var depth = 0
            var nodes = q.size
            var next = 0
            while (q.isNotEmpty() && depth < 2) {
                val n = q.removeFirst()
                nodes--
                val k = n.key?.lowercase(Locale.getDefault()).orEmpty()
                if (k.contains("name")) {
                    val v = n.getValue(String::class.java)
                    if (!v.isNullOrBlank()) return v
                }
                n.children.forEach { c -> q.add(c); next++ }
                if (nodes == 0) {
                    depth++; nodes = next; next = 0
                }
            }
            return null
        }

        fun loadFixedAvatar() {
            Glide.with(this@ProfileFragment)
                .load(FIXED_AVATAR_URL)
                .centerCrop()
                .into(binding.imageAvatar)
        }

        fun applySnapshot(s: DataSnapshot) {
            val picked = findNameInSnapshot(s)
                ?: auth.currentUser?.displayName
                ?: deriveNameFromEmail(auth.currentUser?.email)
            val name = picked.trim()
            val email = s.child("email").getValue(String::class.java)
                ?: auth.currentUser?.email.orEmpty()

            binding.textViewName.text = if (name.isNotEmpty()) "Name: $name" else "Name: -"
            binding.textViewEmail.text = if (email.isNotEmpty()) "Email: $email" else "Email: -"

            val wMillis = s.child("weddingDate").getValue(Long::class.java)
            if (wMillis != null && wMillis > 0L) {
                selectedWeddingDateMillis = wMillis
                binding.textWeddingDate.text = formatDate(wMillis)
            } else {
                binding.textWeddingDate.text = "Not set"
            }

            loadFixedAvatar()
        }

        db.reference.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(s1: DataSnapshot) {
                    if (s1.exists()) {
                        applySnapshot(s1)
                    } else {
                        db.reference.child("Users").child(uid)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(s2: DataSnapshot) {
                                    if (s2.exists()) {
                                        applySnapshot(s2)
                                    } else {
                                        val email = auth.currentUser?.email.orEmpty()
                                        val name = deriveNameFromEmail(email)
                                        binding.textViewName.text =
                                            if (name.isNotEmpty()) "Name: $name" else "Name: -"
                                        binding.textViewEmail.text =
                                            if (email.isNotEmpty()) "Email: $email" else "Email: -"
                                        binding.textWeddingDate.text = "Not set"
                                        loadFixedAvatar()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to load profile.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load profile.", Toast.LENGTH_SHORT)
                        .show()
                }
            })

        binding.buttonPickWeddingDate.setOnClickListener { openDatePicker() }

        binding.buttonSaveWeddingDate.setOnClickListener {
            val date = selectedWeddingDateMillis
            if (date == null) {
                Toast.makeText(requireContext(), "Pick a date first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            db.reference.child("users").child(uid).child("weddingDate").setValue(date)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Wedding date saved.", Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to save date.", Toast.LENGTH_SHORT)
                        .show()
                }
        }

        binding.buttonChangePassword.setOnClickListener {
            val email = auth.currentUser?.email
            if (email.isNullOrBlank()) {
                Toast.makeText(
                    requireContext(),
                    "No email available for reset.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "Password reset email sent.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to send email.", Toast.LENGTH_SHORT)
                        .show()
                }
        }

        binding.buttonLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun openDatePicker() {
        val cal = Calendar.getInstance()
        selectedWeddingDateMillis?.let { cal.timeInMillis = it }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        val d = cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, yy, mm, dd ->
            val picked = Calendar.getInstance().apply {
                set(Calendar.YEAR, yy)
                set(Calendar.MONTH, mm)
                set(Calendar.DAY_OF_MONTH, dd)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            selectedWeddingDateMillis = picked
            binding.textWeddingDate.text = formatDate(picked)
        }, y, m, d).show()
    }

    private fun formatDate(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format("%02d/%02d/%04d", d, m, y)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
