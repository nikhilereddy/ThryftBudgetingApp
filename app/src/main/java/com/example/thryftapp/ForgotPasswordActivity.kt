package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.thryftapp.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var firebaseAuth: FirebaseAuth

    //handle forgot password screen logic
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance() //initialize firebase auth
        Log.d("ForgotPassword", "firebase auth initialized") //log init

        //handle reset button click
        binding.resetPasswordButton.setOnClickListener {
            val email = binding.forgotEmailEditText.text.toString().trim()

            //check if email field is empty
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email address.", Toast.LENGTH_SHORT).show()
                Log.d("ForgotPassword", "email field empty") //log validation
            } else {
                //send password reset email
                firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                this,
                                "Reset link sent to your email!",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.d("ForgotPassword", "reset link sent to $email") //log success
                        } else {
                            Toast.makeText(
                                this,
                                "Error: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.d("ForgotPassword", "reset failed: ${task.exception?.message}") //log error
                        }
                    }
            }
        }


        binding.signUpFromForgotLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }
}
