package com.example.apexcity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.apexcity.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var prefs: SharedPreferences
    private val ANIMATION_DURATION = 1000L
    private var isPulsing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("ApexCityPrefs", MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("isFirstLaunch", true)

        if (isFirstLaunch) {
            // First launch - show full animation
            showFirstLaunchAnimation()
        } else {
            // Subsequent launches - quick fade in
            showSubsequentLaunchAnimation()
        }

        // Button click
        binding.getStartedButton.setOnClickListener {
            // Stop pulsing animation
            isPulsing = false

            // Mark as not first launch
            prefs.edit().putBoolean("isFirstLaunch", false).apply()

            // Animate button click
            binding.getStartedButton.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100L)
                .withEndAction {
                    // Navigate to main activity
                    startActivity(Intent(this, com.example.apexcity.ui.MainActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }
                .start()
        }
    }

    private fun showFirstLaunchAnimation() {
        // Make views visible but transparent initially
        binding.backgroundImage.visibility = View.VISIBLE
        binding.getStartedButton.visibility = View.VISIBLE

        // Set initial states
        binding.backgroundImage.alpha = 0f
        binding.backgroundImage.scaleX = 1.2f
        binding.backgroundImage.scaleY = 1.2f

        binding.getStartedButton.alpha = 0f
        binding.getStartedButton.scaleX = 0.5f
        binding.getStartedButton.scaleY = 0.5f

        // Animate background image - zoom out with fade in
        binding.backgroundImage.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Animate button after delay - scale and fade in
        Handler(Looper.getMainLooper()).postDelayed({
            binding.getStartedButton.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    // Start pulse animation after button appears
                    pulseButton()
                }
                .start()
        }, 800L)
    }

    private fun showSubsequentLaunchAnimation() {
        // Make views visible
        binding.backgroundImage.visibility = View.VISIBLE
        binding.getStartedButton.visibility = View.VISIBLE

        // Set initial alpha
        binding.backgroundImage.alpha = 0f
        binding.getStartedButton.alpha = 0f

        // Reset any scale transformations
        binding.backgroundImage.scaleX = 1f
        binding.backgroundImage.scaleY = 1f

        // Quick fade in for background
        binding.backgroundImage.animate()
            .alpha(1f)
            .setDuration(400L)
            .start()

        // Quick fade in for button
        binding.getStartedButton.animate()
            .alpha(1f)
            .setDuration(400L)
            .setStartDelay(200L)
            .withEndAction {
                // Start pulse animation
                pulseButton()
            }
            .start()
    }

    private fun pulseButton() {
        if (!isPulsing) {
            isPulsing = true
            doPulse()
        }
    }

    private fun doPulse() {
        // Pulse out (grow)
        binding.getStartedButton.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(800L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (isPulsing) {
                    // Pulse in (shrink back)
                    binding.getStartedButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(800L)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction {
                            if (isPulsing) {
                                // Repeat the pulse
                                doPulse()
                            }
                        }
                        .start()
                }
            }
            .start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop pulsing when activity is destroyed
        isPulsing = false
    }
}