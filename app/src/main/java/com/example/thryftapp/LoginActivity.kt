package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.thryftapp.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding //view binding
    private val userDao by lazy { AppDatabase.getDatabase(this).userDao() } //user dao reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater) //inflate layout
        setContentView(binding.root) //set content view

        binding.signInButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim().lowercase() //get email input
            val password = binding.passwordInput.text.toString() //get password input

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show() //validate input
            } else {
                loginUser(email, password) //call login function
            }
        }

        binding.signupText.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java)) //navigate to signup
        }

        binding.forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java)) //navigate to forgot password
        }
    }

    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                userDao.loginUser(email, password) //attempt login from db
            }

            if (user == null) {
                Toast.makeText(this@LoginActivity, "Invalid email or password", Toast.LENGTH_SHORT).show() //login failed
            } else {
                Toast.makeText(this@LoginActivity, "Welcome ${user.name}!", Toast.LENGTH_SHORT).show() //login success

                val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE) //get prefs
                prefs.edit()
                    .putInt("user_id", user.id)
                    .putString("user_name", user.name)
                    .putString("user_email", user.email)
                    .putBoolean("is_logged_in", true)
                    .apply() //save user session

                startActivity(Intent(this@LoginActivity, NavHostActivity::class.java)) //go to nav host
                finish() //close login screen
            }
        }
    }
}
