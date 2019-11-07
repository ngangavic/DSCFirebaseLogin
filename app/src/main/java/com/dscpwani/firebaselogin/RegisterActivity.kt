package com.dscpwani.firebaselogin

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    lateinit var editTextEmail: EditText
    lateinit var editTextPassword: EditText
    lateinit var editTextCPassword: EditText
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextCPassword = findViewById(R.id.editTextCPassword)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        //go to login activity
        buttonLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        //go to register activity
        buttonRegister.setOnClickListener {
            register(
                editTextEmail.text.toString(),
                editTextPassword.text.toString(),
                editTextCPassword.text.toString()
            )
        }

    }

    //check if password match
    private fun checkPassword(password1: String, password2: String): Boolean {
        return password1.length >= 6 && password1.equals(password2)
    }

    //check password lenght
    private fun checkPasswordLength(password1: String): Boolean {
        return password1.length >= 6
    }

    //clear editText
    private fun clearText() {
        editTextEmail.text.clear()
        editTextPassword.text.clear()
        editTextCPassword.text.clear()
    }

    //register function
    private fun register(email: String, password1: String, password2: String) {
        if (TextUtils.isEmpty(email)) {
            editTextEmail.requestFocus()
            editTextEmail.error = "Cannot be empty"
        } else if (TextUtils.isEmpty(password1)) {
            editTextPassword.requestFocus()
            editTextPassword.error = "Cannot be empty"
        } else if (TextUtils.isEmpty(password2)) {
            editTextCPassword.requestFocus()
            editTextCPassword.error = "Cannot be empty"
        } else if (!checkPasswordLength(password1)) {
            editTextPassword.requestFocus()
            editTextPassword.error = "Password too short"
        } else if (!checkPassword(password1, password2)) {
            editTextCPassword.requestFocus()
            editTextCPassword.error = "Password do not match"
        } else {
            //register
            auth.createUserWithEmailAndPassword(email, password1)
                .addOnCompleteListener(this) { task ->
                    clearText()
                    if (task.isSuccessful) {
                        // Sign in success
                        Log.d("Register Activity", "createUserWithEmail:success")
                        val user = auth.currentUser
                        Toast.makeText(this, "Registration success "+user, Toast.LENGTH_LONG).show()
                        startActivity(Intent(this,MainActivity::class.java))
                        finish()
                    } else {
                        // Registration failed
                        Log.w("Register Activity", "createUserWithEmail:failure", task.exception)
                        Toast.makeText(this, "Registration failed", Toast.LENGTH_LONG).show()
                    }

                }
        }
    }

}
