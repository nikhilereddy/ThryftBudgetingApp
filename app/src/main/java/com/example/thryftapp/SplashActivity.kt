package com.example.thryftapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieDrawable
import com.example.thryftapp.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    private var animationEnded = false
    private var delayCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load Animations
        val slideTop = AnimationUtils.loadAnimation(this, R.anim.slide_in_top)
        val slideBottom = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        // Start Animations
        binding.topRightCircle.startAnimation(slideTop)
        binding.bottomLeftCircle.startAnimation(slideBottom)
        binding.logoText.startAnimation(slideTop)
        binding.tagline.startAnimation(slideBottom)
        binding.splashloader.startAnimation(fadeIn)

        // Play Lottie when ready
        binding.splashloader.addLottieOnCompositionLoadedListener {
            binding.splashloader.repeatCount = LottieDrawable.INFINITE
            binding.splashloader.speed = 1.8f
            binding.splashloader.playAnimation()
        }

        // When Lottie finishes
        binding.splashloader.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                animationEnded = true
                if (delayCompleted) moveToNext()
            }
        })

        // Force move after 4 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            delayCompleted = true
            if (animationEnded) {
                moveToNext()
            } else {
                moveToNext() // force move anyway
            }
        }, 4000)
    }

    private fun moveToNext() {
        val prefs = getSharedPreferences("thryft_prefs", MODE_PRIVATE)
        val welcomeSeen = prefs.getBoolean("welcome_seen", false)

        if (welcomeSeen) {
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
        } else {
            startActivity(Intent(this@SplashActivity, WelcomeActivity1::class.java))
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
