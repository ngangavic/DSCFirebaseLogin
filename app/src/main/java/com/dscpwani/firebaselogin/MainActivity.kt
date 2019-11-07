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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    lateinit var editTextEmail: EditText
    lateinit var editTextPassword: EditText
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)

        //Initialize firebase
        auth = FirebaseAuth.getInstance()

        //initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        //login
        buttonLogin.setOnClickListener {
            login(editTextEmail.text.toString(), editTextPassword.text.toString())
        }

        //go to register activity
        buttonRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

    }

    private fun clearText(){
        editTextEmail.text.clear()
        editTextPassword.text.clear()
    }

    private fun login(email: String, password: String) {
        if (TextUtils.isEmpty(email)) {
            editTextEmail.requestFocus()
            editTextEmail.error = "Cannot be empty"
        } else if (TextUtils.isEmpty(password)) {
            editTextPassword.requestFocus()
            editTextPassword.error = "Cannot be empty"
        } else {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    clearText()
                    if (task.isSuccessful) {
                        // Sign in success,
                        Log.d("MainActivity", "signInWithEmail:success")
                        val user = auth.currentUser
                       // val user = User(name, email)
                        database.child("users").child(auth.currentUser?.uid.toString()).push().key
                        Toast.makeText(this, "Sign in success " , Toast.LENGTH_LONG).show()
                        startActivity(Intent(this,ProfileActivity::class.java))
                        finish()
                    } else {
                        // sign in failed
                        Log.w("MainActivity", "signInWithEmail:failure", task.exception)
                        Toast.makeText(this, "Sign in failed", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }


}
