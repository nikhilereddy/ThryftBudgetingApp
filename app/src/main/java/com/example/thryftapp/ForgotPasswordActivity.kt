package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val resetButton = findViewById<Button>(R.id.resetPasswordButton)
        val signUpLink = findViewById<TextView>(R.id.signUpFromForgotLink)

        resetButton.setOnClickListener {
            Toast.makeText(this, "Reset link sent to your email!", Toast.LENGTH_SHORT).show()
        }

        signUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }
}
