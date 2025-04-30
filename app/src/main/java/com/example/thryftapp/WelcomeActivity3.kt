package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.thryftapp.databinding.ActivityWelcome3Binding

class WelcomeActivity3 : AppCompatActivity() {

    private lateinit var binding: ActivityWelcome3Binding
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var nudgeRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcome3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide initially
        binding.consentGroup.visibility = View.INVISIBLE
        binding.nextButton.visibility = View.INVISIBLE

        // 1.5s delay to reveal consent group
        Handler(Looper.getMainLooper()).postDelayed({
            val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            binding.consentGroup.visibility = View.VISIBLE
            binding.consentGroup.startAnimation(fadeIn)

            // 2s later show button with slide
            Handler(Looper.getMainLooper()).postDelayed({
                val slideIn = AnimationUtils.loadAnimation(this, R.anim.btn_slide_fade_in)
                binding.nextButton.visibility = View.VISIBLE
                binding.nextButton.startAnimation(slideIn)
            }, 500)

        }, 1500)

        // Checkbox behavior
        binding.checkboxAgree.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                handler.removeCallbacks(nudgeRunnable)
                binding.nextButton.isEnabled = true
                binding.nextButton.alpha = 1f
            } else {
                binding.nextButton.isEnabled = false
                binding.nextButton.alpha = 0.5f
            }
        }

        // Next button click
        binding.nextButton.setOnClickListener {
            if (binding.checkboxAgree.isChecked) {
                val prefs = getSharedPreferences("thryft_prefs", MODE_PRIVATE)
                prefs.edit().putBoolean("welcome_seen", true).apply()

                startActivity(Intent(this, SignUpActivity::class.java))
                finish()
            }
        }

        // Nudge animation
        nudgeRunnable = object : Runnable {
            override fun run() {
                if (!binding.checkboxAgree.isChecked) {
                    val shake = AnimationUtils.loadAnimation(this@WelcomeActivity3, R.anim.shake)
                    binding.checkboxAgree.startAnimation(shake)
                    handler.postDelayed(this, 3000)
                }
            }
        }
        handler.postDelayed(nudgeRunnable, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(nudgeRunnable)
    }
}
