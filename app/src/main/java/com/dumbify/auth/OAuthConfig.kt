package com.dumbify.auth

/**
 * OAuth configuration for GitHub authentication
 * 
 * To use this:
 * 1. Create a GitHub OAuth App at https://github.com/settings/developers
 * 2. Set Authorization callback URL to: dumbify://oauth/callback
 * 3. Replace CLIENT_ID with your app's client ID
 * 4. For production, use BuildConfig or a secure method to store the client secret
 */
object OAuthConfig {
    const val CLIENT_ID = "YOUR_GITHUB_OAUTH_CLIENT_ID"
    const val REDIRECT_URI = "dumbify://oauth/callback"
    const val AUTH_URL = "https://github.com/login/oauth/authorize"
    const val TOKEN_URL = "https://github.com/login/oauth/access_token"
    
    // Scopes needed for GitHub Copilot access
    // Note: GitHub Copilot API access may require specific scopes
    const val SCOPE = "user:email,copilot"
    
    // GitHub Models API endpoint
    const val GITHUB_MODELS_API_URL = "https://models.inference.ai.azure.com"
}

data class OAuthTokenResponse(
    val access_token: String,
    val token_type: String,
    val scope: String,
    val error: String? = null,
    val error_description: String? = null
)

sealed class OAuthResult {
    data class Success(val accessToken: String, val scope: String) : OAuthResult()
    data class Error(val message: String) : OAuthResult()
}
