package com.example.thryftapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton = view.findViewById<ImageView>(R.id.backButton)
        val notificationSwitch = view.findViewById<Switch>(R.id.notificationSwitch)
        val darkModeSwitch = view.findViewById<Switch>(R.id.darkModeSwitch)
        val logoutLayout = view.findViewById<LinearLayout>(R.id.logoutLayout)

        val prefs = requireContext().getSharedPreferences("thryft_settings", 0)
        val sessionPrefs = requireContext().getSharedPreferences("thryft_session", 0)
        val themePrefs = requireContext().getSharedPreferences("thryft_theme", 0)

        // Restore notification toggle state
        val enabled = prefs.getBoolean("notifications_enabled", true)
        notificationSwitch.isChecked = enabled

        // Create notification channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "thryft_channel",
                "Thryft Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = requireContext().getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        // Save notification toggle state
        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
            Toast.makeText(
                context,
                if (isChecked) "Notifications enabled" else "Notifications disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Restore dark mode state
        val isDark = themePrefs.getBoolean("dark_mode", false)
        darkModeSwitch.isChecked = isDark
        applyTheme(isDark)

        // Handle theme toggle
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            themePrefs.edit().putBoolean("dark_mode", isChecked).apply()
            applyTheme(isChecked)
            Toast.makeText(
                context,
                if (isChecked) "Dark Mode Enabled" else "Light Mode Enabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 🔴 Logout logic
        logoutLayout.setOnClickListener {
            sessionPrefs.edit().clear().apply()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Back navigation
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun applyTheme(darkMode: Boolean) {
        val mode = if (darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
