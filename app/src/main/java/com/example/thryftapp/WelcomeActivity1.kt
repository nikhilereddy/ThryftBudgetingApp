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

    private lateinit var binding: ActivityWelcome1Binding //view binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcome1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        //wait 1.5 seconds then show and animate the button
        Handler(Looper.getMainLooper()).postDelayed({
            binding.getStartedBtn.visibility = View.VISIBLE
            binding.getStartedBtn.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.btn_slide_fade_in)
            )
        }, 1500)

        //start pulsing effect after 5s to draw attention
        val pulse = AnimationUtils.loadAnimation(this, R.anim.btn_pulse)
        val handler = Handler(Looper.getMainLooper())
        val pulseRunnable = object : Runnable {
            override fun run() {
                binding.getStartedBtn.startAnimation(pulse)
                handler.postDelayed(this, 3000) //pulse every 3s
            }
        }
        handler.postDelayed(pulseRunnable, 5000)

        //on click move to next welcome screen
        binding.getStartedBtn.setOnClickListener {
            handler.removeCallbacks(pulseRunnable) //stop pulse
            startActivity(Intent(this, WelcomeActivity2::class.java))
            finish()
        }
    }
}
