package com.example.apexcity.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.apexcity.R
import com.example.apexcity.databinding.ActivityMainBinding
import com.example.apexcity.ui.fragments.*
import com.example.apexcity.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            navigateToAuth()
            return
        }

        setupUI()
        setupNavigation()
        setupDrawer()

        // Check if new user
        val isNewUser = intent.getBooleanExtra("is_new_user", false)
        if (isNewUser) {
            // Navigate to report issue for first-time users
            loadFragment(ReportIssueSelectionFragment())
        } else {
            // Load home fragment
            loadFragment(HomeFragment())
        }
    }

    private fun setupUI() {
        // Update user info in drawer
        val headerView = binding.navigationView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.drawerUserName).text = sessionManager.getUserName()
        headerView.findViewById<TextView>(R.id.drawerUserEmail).text = sessionManager.getUserEmail()

        // Profile icon click
        binding.profileIcon.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        // Notification icon click
        binding.notificationIcon.setOnClickListener {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_reports -> {
                    loadFragment(MyReportsFragment())
                    true
                }
                R.id.nav_map -> {
                    loadFragment(MapFragment())
                    true
                }
                R.id.nav_chat -> {
                    loadFragment(ChatFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun setupDrawer() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_leaderboard -> {
                    loadFragment(LeaderboardFragment())
                }
                R.id.nav_rate -> {
                    Toast.makeText(this, "Rate Us", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_contribute -> {
                    Toast.makeText(this, "Contribute", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_logout -> {
                    showLogoutDialog()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.navHostFragment, fragment)
            .commit()
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                sessionManager.clearSession()
                navigateToAuth()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToAuth() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}