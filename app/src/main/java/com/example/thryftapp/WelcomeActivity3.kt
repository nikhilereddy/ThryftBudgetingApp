package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity3 : AppCompatActivity() {

    private lateinit var consentGroup: LinearLayout
    private lateinit var nextButton: Button
    private lateinit var checkboxAgree: CheckBox
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var nudgeRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome3)

        // Find Views
        consentGroup = findViewById(R.id.consentGroup)
        nextButton = findViewById(R.id.nextButton)
        checkboxAgree = findViewById(R.id.checkboxAgree)

        // Hide initially
        consentGroup.visibility = View.INVISIBLE
        nextButton.visibility = View.INVISIBLE

        // 1.5s delay to reveal consent group
        Handler(Looper.getMainLooper()).postDelayed({
            val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            consentGroup.visibility = View.VISIBLE
            consentGroup.startAnimation(fadeIn)

            // 2s later show button with slide
            Handler(Looper.getMainLooper()).postDelayed({
                val slideIn = AnimationUtils.loadAnimation(this, R.anim.btn_slide_fade_in)
                nextButton.visibility = View.VISIBLE
                nextButton.startAnimation(slideIn)
            }, 500)

        }, 1500)

        // Set up checkbox and button behavior
        checkboxAgree.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                handler.removeCallbacks(nudgeRunnable) // Stop nudging
                nextButton.isEnabled = true
                nextButton.alpha = 1f
            } else {
                nextButton.isEnabled = false
                nextButton.alpha = 0.5f
            }
        }

        nextButton.setOnClickListener {
            if (checkboxAgree.isChecked) {
                val prefs = getSharedPreferences("thryft_prefs", MODE_PRIVATE)
                prefs.edit().putBoolean("welcome_seen", true).apply()

                startActivity(Intent(this, SignUpActivity::class.java))
                finish()
            }


    }

        // Define the nudge animation
        nudgeRunnable = object : Runnable {
            override fun run() {
                if (!checkboxAgree.isChecked) {
                    val shake = AnimationUtils.loadAnimation(this@WelcomeActivity3, R.anim.shake)
                    checkboxAgree.startAnimation(shake)
                    handler.postDelayed(this, 3000) // Nudge every 3s
                }
            }
        }

        // Start first nudge timer after 3s
        handler.postDelayed(nudgeRunnable, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(nudgeRunnable) // Important to prevent memory leak
    }
}
