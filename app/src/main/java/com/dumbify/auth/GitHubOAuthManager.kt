package com.dumbify.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.dumbify.repository.AppConfigRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Manages GitHub OAuth authentication flow
 * 
 * Flow:
 * 1. startOAuthFlow() - Opens browser with GitHub authorization page
 * 2. User authorizes app on GitHub
 * 3. GitHub redirects to dumbify://oauth/callback?code=...
 * 4. OAuthCallbackActivity captures the code
 * 5. exchangeCodeForToken() - Exchanges code for access token
 * 6. Token is stored securely in AppConfigRepository
 */
class GitHubOAuthManager(private val context: Context) {
    
    private val repository = AppConfigRepository(context)
    private val httpClient = OkHttpClient()
    private val gson = Gson()
    
    companion object {
        private const val TAG = "GitHubOAuthManager"
        
        // Generate a random state parameter for CSRF protection
        fun generateState(): String {
            return java.util.UUID.randomUUID().toString()
        }
    }
    
    /**
     * Starts the OAuth flow by opening the GitHub authorization page in the browser
     * 
     * @param activity The activity to start the browser intent from
     * @param state Optional CSRF protection state parameter
     */
    fun startOAuthFlow(activity: Activity, state: String = generateState()) {
        // Save state for verification when callback returns
        repository.oauthState = state
        
        val authUrl = buildAuthUrl(state)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
        
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start OAuth flow", e)
        }
    }
    
    /**
     * Builds the GitHub authorization URL with required parameters
     */
    private fun buildAuthUrl(state: String): String {
        return Uri.parse(OAuthConfig.AUTH_URL)
            .buildUpon()
            .appendQueryParameter("client_id", OAuthConfig.CLIENT_ID)
            .appendQueryParameter("redirect_uri", OAuthConfig.REDIRECT_URI)
            .appendQueryParameter("scope", OAuthConfig.SCOPE)
            .appendQueryParameter("state", state)
            .build()
            .toString()
    }
    
    /**
     * Exchanges the authorization code for an access token
     * 
     * IMPORTANT: This implementation requires a backend proxy server to securely
     * handle the client secret. For production use, implement a server endpoint
     * that handles the token exchange.
     * 
     * Alternative: Use GitHub Device Flow OAuth which doesn't require a client secret
     * https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow
     * 
     * @param code The authorization code from GitHub
     * @return OAuthResult containing the access token or error
     */
    suspend fun exchangeCodeForToken(code: String): OAuthResult = withContext(Dispatchers.IO) {
        try {
            // WARNING: This is a simplified implementation for demonstration
            // In production, use a backend server to handle token exchange
            // to keep your client secret secure
            
            val requestBody = FormBody.Builder()
                .add("client_id", OAuthConfig.CLIENT_ID)
                .add("code", code)
                .add("redirect_uri", OAuthConfig.REDIRECT_URI)
                // DO NOT hardcode client_secret in production apps
                // Use a backend proxy server instead
                .build()
            
            val request = Request.Builder()
                .url(OAuthConfig.TOKEN_URL)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .build()
            
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Token exchange failed: ${response.code}")
                return@withContext OAuthResult.Error("Failed to exchange code for token")
            }
            
            val tokenResponse = gson.fromJson(responseBody, OAuthTokenResponse::class.java)
            
            if (tokenResponse.error != null) {
                Log.e(TAG, "OAuth error: ${tokenResponse.error} - ${tokenResponse.error_description}")
                return@withContext OAuthResult.Error(tokenResponse.error_description ?: tokenResponse.error)
            }
            
            // Store the access token securely
            repository.githubAccessToken = tokenResponse.access_token
            repository.githubTokenScope = tokenResponse.scope
            
            Log.i(TAG, "Successfully obtained GitHub access token")
            OAuthResult.Success(tokenResponse.access_token, tokenResponse.scope)
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error during token exchange", e)
            OAuthResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during token exchange", e)
            OAuthResult.Error("Unexpected error: ${e.message}")
        }
    }
    
    /**
     * Checks if user is authenticated with GitHub
     */
    fun isAuthenticated(): Boolean {
        return !repository.githubAccessToken.isNullOrEmpty()
    }
    
    /**
     * Logs out by clearing the stored access token
     */
    fun logout() {
        repository.githubAccessToken = null
        repository.githubTokenScope = null
        repository.oauthState = null
        Log.i(TAG, "User logged out from GitHub")
    }
    
    /**
     * Verifies the state parameter for CSRF protection
     */
    fun verifyState(receivedState: String): Boolean {
        val savedState = repository.oauthState
        return savedState != null && savedState == receivedState
    }
}
