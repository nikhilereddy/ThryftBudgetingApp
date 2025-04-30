package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.thryftapp.databinding.ActivityWelcome1Binding

class WelcomeActivity1 : AppCompatActivity() {

    private lateinit var binding: ActivityWelcome1Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcome1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Wait 2 seconds then animate slide-in
        Handler(Looper.getMainLooper()).postDelayed({
            binding.getStartedBtn.visibility = View.VISIBLE
            binding.getStartedBtn.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.btn_slide_fade_in)
            )
        }, 1500)

        // After another 4s (6s total), start pulsing nudge
        val pulse = AnimationUtils.loadAnimation(this, R.anim.btn_pulse)
        val handler = Handler(Looper.getMainLooper())
        val pulseRunnable = object : Runnable {
            override fun run() {
                binding.getStartedBtn.startAnimation(pulse)
                handler.postDelayed(this, 3000)
            }
        }
        handler.postDelayed(pulseRunnable, 5000)

        binding.getStartedBtn.setOnClickListener {
            handler.removeCallbacks(pulseRunnable)
            startActivity(Intent(this, WelcomeActivity2::class.java))
            finish()
        }
    }
}
