/*
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

    private lateinit var binding: ActivitySignupBinding //view binding
    private val userDao by lazy {
        AppDatabase.getDatabase(this).userDao() //get user dao
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root) //set layout

        //go to login screen
        val goToLogin = {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        binding.backButton.setOnClickListener { goToLogin() }
        binding.signInLink.setOnClickListener { goToLogin() }

        //signup button click handler
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
                    createUser(name, email, password) //create user
                }
            }
        }
    }

    //inserts user in background
    private fun createUser(name: String, email: String, password: String) {
        lifecycleScope.launch {
            //check if email already registered
            val existing = withContext(Dispatchers.IO) {
                userDao.getUserByEmail(email)
            }
            if (existing != null) {
                toast("Email already registered")
                return@launch
            }

            val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()) //timestamp

            //insert new user
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
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() //show toast
    }
}
*/
package com.example.thryftapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.thryftapp.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private val userDao by lazy {
        AppDatabase.getDatabase(this).userDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        if (!NetworkUtils.isOnline(this)) {
            showNoInternetDialog()
            disableInputs()
        }

        val goToLogin = {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.backButton.setOnClickListener { goToLogin() }
        binding.signInLink.setOnClickListener { goToLogin() }

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

    private fun disableInputs() {
        binding.signupButton.isEnabled = false
        binding.emailEditText.isEnabled = false
        binding.passwordEditText.isEnabled = false
        binding.confirmPasswordEditText.isEnabled = false
        binding.fullNameEditText.isEnabled = false
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

    private fun createUser(name: String, email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    val firebaseUid = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener

                    val newUser = User(
                        id = firebaseUid,
                        name = name,
                        email = email,
                        password = password,
                        createdAt = date
                    )

                    // 💾 Save to Room
                    lifecycleScope.launch(Dispatchers.IO) {
                        userDao.insertUser(newUser)
                    }

                    // 🔥 Save to Firestore
                    val userMap = hashMapOf(
                        "id" to firebaseUid,
                        "name" to name,
                        "email" to email,
                        "createdAt" to date
                    )

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(firebaseUid)
                        .set(userMap)
                        .addOnSuccessListener {
                            toast("Account created! Please sign in")
                            startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            toast("Firestore error: ${e.message}")
                        }

                } else {
                    toast("Sign-up failed: ${task.exception?.message}")
                }
            }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
