package com.example.letsgetweddi.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.letsgetweddi.databinding.ActivityLoginBinding
import com.example.letsgetweddi.utils.RoleManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private var selectedRole: String = "client"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        binding.toggleRole.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedRole = if (checkedId == binding.btnSupplier.id) "supplier" else "client"
            }
        }

        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                toast("Fill all fields")
                return@setOnClickListener
            }
            setLoading(true)
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser!!.uid
                    val ref = FirebaseDatabase.getInstance().getReference("Users").child(uid)
                    ref.child("role").get().addOnSuccessListener { snap ->
                        val roleInDb = snap.getValue(String::class.java)
                        val roleFinal = roleInDb ?: selectedRole
                        if (roleInDb == null) {
                            ref.child("role").get().addOnSuccessListener { s ->
                                val roleInDb = s.getValue(String::class.java)
                                val roleFinal = roleInDb ?: selectedRole
                                if (roleInDb == null) {
                                    ref.child("role").setValue(roleFinal).addOnCompleteListener {
                                        RoleManager.load(this@LoginActivity) { role, supplierId ->
                                            goHome()
                                        }
                                    }
                                } else {
                                    RoleManager.load(this@LoginActivity) { role, supplierId ->
                                        goHome()
                                    }
                                }
                            }
                        }
                    }.addOnFailureListener {
                        setLoading(false)
                        toast("Failed to read role")
                    }
                } else {
                    setLoading(false)
                    toast(task.exception?.localizedMessage ?: "Login failed")
                }
            }
        }

        binding.buttonRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun goHome() {
        setLoading(false)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !loading
        binding.buttonRegister.isEnabled = !loading
        binding.btnClient.isEnabled = !loading
        binding.btnSupplier.isEnabled = !loading
        binding.editTextEmail.isEnabled = !loading
        binding.editTextPassword.isEnabled = !loading
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}