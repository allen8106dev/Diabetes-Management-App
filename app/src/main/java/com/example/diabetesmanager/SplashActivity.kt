package com.example.diabetesmanager

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val splashTimeOut: Long = 2500 // 2.5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Hide action bar for full screen splash
        supportActionBar?.hide()

        // Initialize views
        val appLogo = findViewById<ImageView>(R.id.splash_logo)
        val appName = findViewById<TextView>(R.id.splash_app_name)
        val developerName = findViewById<TextView>(R.id.splash_developer_name)

        // Add animations
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)

        appLogo.startAnimation(fadeIn)
        appName.startAnimation(slideUp)
        developerName.startAnimation(slideUp)

        // Navigate to main activity after delay
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close splash activity

            // Add smooth transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, splashTimeOut)
    }

    // Prevent back button on splash screen
    override fun onBackPressed() {
        // Do nothing - user must wait for splash to complete
    }
}
