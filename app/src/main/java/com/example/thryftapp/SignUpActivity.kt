package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SignUpActivity : AppCompatActivity() {

    private val userDao by lazy {            // lazy so DB builds only once
        AppDatabase.getDatabase(this).userDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // -------- views ----------
        val fullNameEt  = findViewById<EditText>(R.id.fullNameEditText)
        val emailEt     = findViewById<EditText>(R.id.emailEditText)
        val passEt      = findViewById<EditText>(R.id.passwordEditText)
        val confirmEt   = findViewById<EditText>(R.id.confirmPasswordEditText)
        val signUpBtn   = findViewById<Button>(R.id.signupButton)

        val backBtn     = findViewById<TextView>(R.id.backButton)
        val signInLink  = findViewById<TextView>(R.id.signInLink)
        // --------------------------

        // go back / to sign-in
        val goToLogin = {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        backBtn.setOnClickListener { goToLogin() }
        signInLink.setOnClickListener { goToLogin() }

        // ------------ SIGN-UP BUTTON -------------
        signUpBtn.setOnClickListener {
            val name      = fullNameEt.text.toString().trim()
            val email     = emailEt.text.toString().trim().lowercase(Locale.ROOT)
            val password  = passEt.text.toString()
            val confirmPw = confirmEt.text.toString()

            // basic validation
            when {
                name.isEmpty() || email.isEmpty() || password.isEmpty() ->
                    toast("All fields are required")
                password != confirmPw ->
                    toast("Passwords don’t match")
                else -> createUser(name, email, password)
            }
        }
    }

    /** inserts user in background */
    private fun createUser(name: String, email: String, password: String) {
        lifecycleScope.launch {
            // 1. check for duplicate email
            val existing = withContext(Dispatchers.IO) { userDao.getUserByEmail(email) }
            if (existing != null) {
                toast("Email already registered")
                return@launch
            }

            val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

            // 2. insert new user
            val newUser = User(
                name = name,
                email = email,
                password = password,
                createdAt = date
            )

            withContext(Dispatchers.IO) { userDao.insertUser(newUser) }

            toast("Account created! Please sign in")
            // 3. go to login screen
            startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
            finish()
        }
    }

    // tiny helper
    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
