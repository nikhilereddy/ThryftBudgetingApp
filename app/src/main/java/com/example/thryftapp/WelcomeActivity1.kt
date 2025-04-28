package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity1 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_welcome1)

        // Set up Get Started button
        val btnGetStarted = findViewById<Button>(R.id.getStartedBtn)

        // Wait 2 seconds then animate slide-in
        Handler(Looper.getMainLooper()).postDelayed({
            btnGetStarted.visibility = View.VISIBLE
            btnGetStarted.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.btn_slide_fade_in)
            )
        }, 1500)

        // After another 4s (6s total), start pulsing nudge
        val pulse = AnimationUtils.loadAnimation(this, R.anim.btn_pulse)
        val handler = Handler(Looper.getMainLooper())
        val pulseRunnable = object : Runnable {
            override fun run() {
                btnGetStarted.startAnimation(pulse)
                handler.postDelayed(this, 3000)
            }
        }
        handler.postDelayed(pulseRunnable, 5000)

        btnGetStarted.setOnClickListener {
            handler.removeCallbacks(pulseRunnable)
            startActivity(Intent(this, WelcomeActivity2::class.java))
            finish()
        }
    }
}
