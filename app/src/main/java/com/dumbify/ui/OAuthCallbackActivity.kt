package com.dumbify.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dumbify.auth.GitHubOAuthManager
import com.dumbify.auth.OAuthResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Handles OAuth callback from GitHub
 * 
 * This activity is launched when GitHub redirects back to the app after
 * the user authorizes (or denies) the OAuth request.
 * 
 * The callback URL format is: dumbify://oauth/callback?code=...&state=...
 */
class OAuthCallbackActivity : AppCompatActivity() {
    
    // Use lazy initialization to avoid lateinit issues
    private val oauthManager by lazy { GitHubOAuthManager(this) }
    
    // Properly managed coroutine scope
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        private const val TAG = "OAuthCallbackActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle the OAuth callback
        handleOAuthCallback(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cancel coroutines to prevent leaks
        activityScope.cancel()
    }
    
    private fun handleOAuthCallback(intent: Intent?) {
        val data: Uri? = intent?.data
        
        if (data == null) {
            Log.e(TAG, "No data in callback intent")
            showError("Invalid OAuth callback")
            finish()
            return
        }
        
        Log.d(TAG, "Received OAuth callback: $data")
        
        // Check for errors in the callback
        val error = data.getQueryParameter("error")
        if (error != null) {
            val errorDescription = data.getQueryParameter("error_description") ?: error
            Log.e(TAG, "OAuth error: $error - $errorDescription")
            showError("Authorization failed: $errorDescription")
            finish()
            return
        }
        
        // Get the authorization code and state
        val code = data.getQueryParameter("code")
        val state = data.getQueryParameter("state")
        
        if (code == null) {
            Log.e(TAG, "No authorization code in callback")
            showError("Invalid authorization code")
            finish()
            return
        }
        
        // Verify state for CSRF protection
        if (state == null || !oauthManager.verifyState(state)) {
            Log.e(TAG, "Invalid state parameter - possible CSRF attack")
            showError("Security validation failed")
            finish()
            return
        }
        
        // Exchange code for access token
        exchangeCodeForToken(code)
    }
    
    private fun exchangeCodeForToken(code: String) {
        activityScope.launch {
            try {
                Log.d(TAG, "Exchanging authorization code for access token...")
                
                val result = oauthManager.exchangeCodeForToken(code)
                
                when (result) {
                    is OAuthResult.Success -> {
                        Log.i(TAG, "Successfully authenticated with GitHub")
                        Toast.makeText(
                            this@OAuthCallbackActivity,
                            "Successfully connected to GitHub!",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // Navigate back to settings
                        navigateToSettings()
                    }
                    
                    is OAuthResult.Error -> {
                        Log.e(TAG, "Token exchange failed: ${result.message}")
                        showError("Authentication failed: ${result.message}")
                    }
                }
                
                finish()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during token exchange", e)
                showError("Unexpected error: ${e.message}")
                finish()
            }
        }
    }
    
    private fun navigateToSettings() {
        val intent = Intent(this, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
