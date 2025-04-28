package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome2)

        val nextBtn = findViewById<Button>(R.id.nextButton)

// Wait 2 seconds, then slide in
        Handler(Looper.getMainLooper()).postDelayed({
            nextBtn.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.btn_slide_fade_in)
            )
            nextBtn.visibility = View.VISIBLE  // just in case it was hidden
        }, 1500) // 2000ms = 2 seconds

// Idle nudge after the button is shown
        val pulse = AnimationUtils.loadAnimation(this, R.anim.btn_pulse)
        val handler = Handler(Looper.getMainLooper())
        val pulseRunnable = object : Runnable {
            override fun run() {
                nextBtn.startAnimation(pulse)
                handler.postDelayed(this, 3000)   // pulse every 4s
            }
        }
        handler.postDelayed(pulseRunnable, 5000) // start nudging after 6s (after slide finishes + some gap)

        nextBtn.setOnClickListener {
            handler.removeCallbacks(pulseRunnable)
            startActivity(Intent(this, WelcomeActivity3::class.java))
            finish()
        }

    }
}
