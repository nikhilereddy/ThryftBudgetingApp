package com.example.thryftapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

class SplashActivity : AppCompatActivity() {

    private var animationEnded = false
    private var delayCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)



        // Find Views
        val topCircle = findViewById<View>(R.id.topRightCircle)
        val bottomCircle = findViewById<View>(R.id.bottomLeftCircle)
        val logoText = findViewById<TextView>(R.id.logoText)
        val tagline = findViewById<TextView>(R.id.tagline)
        val lottieView = findViewById<LottieAnimationView>(R.id.splashloader)

        // Load Animations
        val slideTop = AnimationUtils.loadAnimation(this, R.anim.slide_in_top)
        val slideBottom = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        // Start Animations
        topCircle.startAnimation(slideTop)
        bottomCircle.startAnimation(slideBottom)
        logoText.startAnimation(slideTop)
        tagline.startAnimation(slideBottom)
        lottieView.startAnimation(fadeIn)

        // Play Lottie when ready
        lottieView.addLottieOnCompositionLoadedListener {
            lottieView.repeatCount = LottieDrawable.INFINITE
            lottieView.speed = 1.8f
            lottieView.playAnimation()
        }

        // When Lottie finishes (if needed)
        lottieView.addAnimatorListener(object : AnimatorListenerAdapter() {
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
                moveToNext() // move anyway after delay even if animation is still looping
            }
        }, 4000)
    }

    private fun moveToNext() {
        val prefs = getSharedPreferences("thryft_prefs", MODE_PRIVATE)
        val welcomeSeen = prefs.getBoolean("welcome_seen", false)

        if (welcomeSeen) {
            // User has already completed welcome flow, go directly to login
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
        } else {
            // First time user, show Welcome screens
            startActivity(Intent(this@SplashActivity, WelcomeActivity1::class.java))
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

}
