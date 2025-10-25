package com.example.apexcity.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.apexcity.databinding.ActivityLandingBinding
import com.example.apexcity.utils.SessionManager

class LandingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLandingBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        binding.getStartedButton.setOnClickListener {
            startActivity(Intent(this, AuthActivity::class.java))
        }

        binding.browseButton.setOnClickListener {
            // Navigate to public view without login
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("show_public_view", true)
            })
        }
    }
}