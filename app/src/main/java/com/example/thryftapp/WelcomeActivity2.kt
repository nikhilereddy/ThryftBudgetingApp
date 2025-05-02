package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.thryftapp.databinding.ActivityWelcome2Binding

class WelcomeActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityWelcome2Binding //view binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcome2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        //wait 1.5s then show and animate next button
        Handler(Looper.getMainLooper()).postDelayed({
            binding.nextButton.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.btn_slide_fade_in)
            )
            binding.nextButton.visibility = View.VISIBLE
        }, 1500)

        //start pulse effect after 5s to attract user attention
        val pulse = AnimationUtils.loadAnimation(this, R.anim.btn_pulse)
        val handler = Handler(Looper.getMainLooper())
        val pulseRunnable = object : Runnable {
            override fun run() {
                binding.nextButton.startAnimation(pulse)
                handler.postDelayed(this, 3000) //repeat every 3s
            }
        }
        handler.postDelayed(pulseRunnable, 5000)

        //navigate to next welcome screen on click
        binding.nextButton.setOnClickListener {
            handler.removeCallbacks(pulseRunnable) //stop pulsing
            startActivity(Intent(this, WelcomeActivity3::class.java))
            finish()
        }
    }
}
