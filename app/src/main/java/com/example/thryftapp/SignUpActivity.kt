package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.thryftapp.databinding.ActivitySignupBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val userDao by lazy {
        AppDatabase.getDatabase(this).userDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --------- Buttons ----------
        val goToLogin = {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        binding.backButton.setOnClickListener { goToLogin() }
        binding.signInLink.setOnClickListener { goToLogin() }

        // --------- Sign Up button ----------
        binding.signupButton.setOnClickListener {
            val name = binding.fullNameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim().lowercase(Locale.ROOT)
            val password = binding.passwordEditText.text.toString()
            val confirmPw = binding.confirmPasswordEditText.text.toString()

            when {
                name.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                    toast("All fields are required")
                }
                password != confirmPw -> {
                    toast("Passwords don’t match")
                }
                else -> {
                    createUser(name, email, password)
                }
            }
        }
    }

    /** inserts user in background */
    private fun createUser(name: String, email: String, password: String) {
        lifecycleScope.launch {
            // 1. check if email already registered
            val existing = withContext(Dispatchers.IO) {
                userDao.getUserByEmail(email)
            }
            if (existing != null) {
                toast("Email already registered")
                return@launch
            }

            val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

            // 2. insert into database
            val newUser = User(
                name = name,
                email = email,
                password = password,
                createdAt = date
            )
            withContext(Dispatchers.IO) {
                userDao.insertUser(newUser)
            }

            toast("Account created! Please sign in")
            startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
            finish()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
