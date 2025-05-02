package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.thryftapp.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding //view binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater) //inflate layout
        setContentView(binding.root) //set content view

        binding.resetPasswordButton.setOnClickListener {
            Toast.makeText(this, "Reset link sent to your email!", Toast.LENGTH_SHORT).show() //show toast
        }

        binding.signUpFromForgotLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java)) //navigate to signup
            finish() //close current activity
        }
    }
}
