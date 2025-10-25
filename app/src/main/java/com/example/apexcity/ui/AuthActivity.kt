package com.example.apexcity.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.apexcity.R
import com.example.apexcity.data.api.RetrofitClient
import com.example.apexcity.data.model.LoginRequest
import com.example.apexcity.data.model.RegisterRequest
import com.example.apexcity.data.model.GoogleSignInRequest
import com.example.apexcity.databinding.ActivityAuthBinding
import com.example.apexcity.utils.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        setupGoogleSignIn()
        setupUI()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupUI() {
        // Back button
        binding.backButton.setOnClickListener {
            finish()
        }

        // Tab switching
        binding.loginTab.setOnClickListener {
            showLoginForm()
        }

        binding.signupTab.setOnClickListener {
            showSignupForm()
        }

        // Login button
        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()

            if (validateLogin(email, password)) {
                performLogin(email, password)
            }
        }

        // Signup button
        binding.signupButton.setOnClickListener {
            val name = binding.signupName.text.toString().trim()
            val email = binding.signupEmail.text.toString().trim()
            val phone = binding.signupPhone.text.toString().trim()
            val password = binding.signupPassword.text.toString().trim()

            if (validateSignup(name, email, phone, password)) {
                performSignup(name, email, phone, password)
            }
        }

        // Google Sign In
        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun showLoginForm() {
        binding.loginTab.setBackgroundResource(R.drawable.tab_selected)
        binding.loginTab.setTextColor(getColor(R.color.black))
        binding.signupTab.background = null
        binding.signupTab.setTextColor(getColor(R.color.light_gray))

        binding.loginForm.visibility = View.VISIBLE
        binding.signupForm.visibility = View.GONE
    }

    private fun showSignupForm() {
        binding.signupTab.setBackgroundResource(R.drawable.tab_selected)
        binding.signupTab.setTextColor(getColor(R.color.black))
        binding.loginTab.background = null
        binding.loginTab.setTextColor(getColor(R.color.light_gray))

        binding.loginForm.visibility = View.GONE
        binding.signupForm.visibility = View.VISIBLE
    }

    private fun validateLogin(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.loginEmail.error = "Email is required"
            return false
        }
        if (password.isEmpty()) {
            binding.loginPassword.error = "Password is required"
            return false
        }
        return true
    }

    private fun validateSignup(name: String, email: String, phone: String, password: String): Boolean {
        if (name.isEmpty()) {
            binding.signupName.error = "Name is required"
            return false
        }
        if (email.isEmpty()) {
            binding.signupEmail.error = "Email is required"
            return false
        }
        if (phone.isEmpty()) {
            binding.signupPhone.error = "Phone is required"
            return false
        }
        if (password.length < 6) {
            binding.signupPassword.error = "Password must be at least 6 characters"
            return false
        }
        return true
    }

    private fun performLogin(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    sessionManager.saveAuthToken(authResponse.token)
                    sessionManager.saveUserData(
                        authResponse.id,
                        authResponse.name,
                        authResponse.email,
                        authResponse.role
                    )
                    navigateToMain()
                } else {
                    Toast.makeText(this@AuthActivity, "Login failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performSignup(name: String, email: String, phone: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.register(
                    RegisterRequest(name, email, password, phone)
                )
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    sessionManager.saveAuthToken(authResponse.token)
                    sessionManager.saveUserData(
                        authResponse.id,
                        authResponse.name,
                        authResponse.email,
                        authResponse.role
                    )
                    navigateToMain(isNewUser = true)
                } else {
                    Toast.makeText(this@AuthActivity, "Signup failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                handleGoogleSignIn(account)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGoogleSignIn(account: GoogleSignInAccount) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.googleSignIn(
                    GoogleSignInRequest(account.idToken!!)
                )
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    sessionManager.saveAuthToken(authResponse.token)
                    sessionManager.saveUserData(
                        authResponse.id,
                        authResponse.name,
                        authResponse.email,
                        authResponse.role
                    )
                    navigateToMain()
                } else {
                    Toast.makeText(this@AuthActivity, "Google sign in failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMain(isNewUser: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("is_new_user", isNewUser)
        startActivity(intent)
        finish()
    }
}