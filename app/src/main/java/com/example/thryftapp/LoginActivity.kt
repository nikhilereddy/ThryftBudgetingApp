package com.example.thryftapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.thryftapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        db = AppDatabase.getDatabase(this)

        if (!NetworkUtils.isOnline(this)) {
            showNoInternetDialog()
            disableInputs()
        }
        /**
         * Attribution:
         * Website: Firebase Authentication, how does it work?

         *  Author: FIREBASE
         *  URL: https://firebase.google.com/docs/auth
         *  Accessed on: 2025-06-06
        -        */
        binding.signInButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim().lowercase(Locale.ROOT)
            val password = binding.passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                toast("Please fill in all fields")
            } else {
                loginUser(email, password)
            }
        }

        binding.signupText.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun disableInputs() {
        binding.signInButton.isEnabled = false
        binding.emailEditText.isEnabled = false
        binding.passwordInput.isEnabled = false
    }

    private fun showNoInternetDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_no_internet, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.closeButton).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun loginUser(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        //Save session
                        getSharedPreferences("thryft_session", MODE_PRIVATE)
                            .edit()
                            .putString("user_id", user.uid)
                            .putString("user_email", email)
                            .putBoolean("is_logged_in", true)
                            .apply()

                        //Also insert user into Room to avoid foreign key crash
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                db.userDao().insertUser(
                                    User(
                                        id = user.uid,
                                        name = "Firebase User", //or fetch from Firestore later
                                        email = email,
                                        password = "",
                                        createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                                    )
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        toast("Login successful")
                        startActivity(Intent(this@LoginActivity, NavHostActivity::class.java))
                        finish()
                    }
                } else {
                    toast("Login failed: ${task.exception?.message}")
                }
            }
    }

    private fun toast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
}
