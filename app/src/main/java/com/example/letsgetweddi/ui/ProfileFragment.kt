package com.example.letsgetweddi.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.letsgetweddi.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        val uid = auth.currentUser?.uid ?: return

        // Read from both "users" (lowercase) and "Users" (uppercase) and both "fullName"/"name"
        fun fill(snapshot: DataSnapshot) {
            val name = snapshot.child("fullName").value?.toString()
                ?: snapshot.child("name").value?.toString()
                ?: ""
            val email = snapshot.child("email").value?.toString()
                ?: auth.currentUser?.email.orEmpty()
            binding.textViewName.text = "Name: $name"
            binding.textViewEmail.text = "Email: $email"
        }

        db.reference.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(s1: DataSnapshot) {
                    if (s1.exists()) {
                        fill(s1)
                    } else {
                        db.reference.child("Users").child(uid)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(s2: DataSnapshot) { fill(s2) }
                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(context, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                }
            })

        binding.buttonChangePassword.setOnClickListener {
            val email = auth.currentUser?.email
            if (email != null) {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener { Toast.makeText(context, "Password reset email sent.", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(context, "Failed to send email.", Toast.LENGTH_SHORT).show() }
            }
        }

        binding.buttonLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
