package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private val userDao by lazy { AppDatabase.getDatabase(this).userDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailEt = findViewById<EditText>(R.id.emailEditText)
        val passwordEt = findViewById<EditText>(R.id.passwordInput)
        val loginBtn = findViewById<Button>(R.id.signInButton)
        val signUpLink = findViewById<TextView>(R.id.signupText)
        val forgotPasswordLink = findViewById<TextView>(R.id.forgotPasswordText)

        loginBtn.setOnClickListener {
            val email = emailEt.text.toString().trim().lowercase()
            val password = passwordEt.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }
        }

        signUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        forgotPasswordLink.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                userDao.loginUser(email, password)
            }

            if (user == null) {
                Toast.makeText(this@LoginActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@LoginActivity, "Welcome ${user.name}!", Toast.LENGTH_SHORT).show()

                // Here you can save session if you want
                // Example:
                // val prefs = getSharedPreferences("thryft_prefs", MODE_PRIVATE)
                // prefs.edit().putString("logged_in_user_email", user.email).apply()

                // Save user session
                val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
                prefs.edit()
                    .putInt("user_id", user.id)         // save user ID
                    .putString("user_name", user.name)   // save user name
                    .putString("user_email", user.email) // save user email
                    .putBoolean("is_logged_in", true)    // mark as logged in
                    .apply()

                // Move to DashboardActivity (we'll create this later)
                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                finish()
            }
        }
    }
}
