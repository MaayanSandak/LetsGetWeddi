package com.example.letsgetweddi.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.letsgetweddi.MainActivity
import com.example.letsgetweddi.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private val db by lazy { FirebaseDatabase.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.buttonRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                toast("Please fill email and password")
                return@setOnClickListener
            }
            setLoading(true)
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    ensureUserRow()
                    setLoading(false)
                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    toast(e.localizedMessage ?: "Login failed")
                }
        }
    }

    private fun ensureUserRow() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val ref = db.reference.child("users").child(uid)
        ref.get().addOnSuccessListener { snap ->
            if (!snap.exists()) {
                val email = user.email.orEmpty()
                val display = (user.displayName?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore('@', email)
                        ).ifBlank { "User" }
                val map = mapOf(
                    "uid" to uid,
                    "fullName" to titleCase(display),
                    "email" to email,
                    "role" to "client"
                )
                ref.setValue(map)
            }
        }
    }

    private fun titleCase(s: String): String =
        s.split(" ", "_", ".", "-")
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase(Locale.getDefault())
                    .replaceFirstChar { c -> c.titlecase(Locale.getDefault()) }
            }

    private fun setLoading(loading: Boolean) {
        val enable = !loading
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = enable
        binding.buttonRegister.isEnabled = enable
        binding.btnClient.isEnabled = enable
        binding.btnSupplier.isEnabled = enable
        binding.editTextEmail.isEnabled = enable
        binding.editTextPassword.isEnabled = enable
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
