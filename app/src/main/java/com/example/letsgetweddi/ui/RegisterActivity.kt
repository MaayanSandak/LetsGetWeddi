package com.example.letsgetweddi.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.letsgetweddi.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val btnGoLogin = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGoLogin)
        btnGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        val nameEditText = findViewById<EditText>(R.id.editTextName)
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val roleGroup = findViewById<RadioGroup>(R.id.roleGroup)
        val createAccountButton = findViewById<View>(R.id.buttonCreateAccount)

        createAccountButton.setOnClickListener {
            val name = nameEditText.text?.toString()?.trim().orEmpty()
            val email = emailEditText.text?.toString()?.trim().orEmpty()
            val password = passwordEditText.text?.toString()?.trim().orEmpty()
            val selectedRoleId = roleGroup.checkedRadioButtonId

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || selectedRoleId == -1) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = if (selectedRoleId != -1)
                findViewById<View>(selectedRoleId).resources.getResourceEntryName(selectedRoleId)
            else
                ""

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { res ->
                    val uid = res.user?.uid ?: return@addOnSuccessListener
                    val userMap = mapOf(
                        "uid" to uid,
                        "fullName" to name,
                        "email" to email,
                        "clientType" to role
                    )
                    db.reference.child("users").child(uid).setValue(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, it.localizedMessage ?: "Failed to save user", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, it.localizedMessage ?: "Register failed", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
