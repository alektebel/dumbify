# GitHub OAuth Integration for Dumbify

This document explains how to set up and use GitHub OAuth to access GitHub Copilot Pro models in the Dumbify app.

## Features Added

- **OAuth 2.0 Authentication**: Secure GitHub authentication flow
- **Multiple AI Providers**: Switch between Google Gemini and GitHub Copilot models
- **GitHub Models Support**: Access to GPT-4o, Claude 3.5 Sonnet, Llama 3.1, o1-preview, and more
- **Secure Token Storage**: OAuth tokens stored in SharedPreferences
- **Provider Selection UI**: Easy switching between AI providers in settings

## Setup Instructions

### 1. Create a GitHub OAuth App

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click "New OAuth App"
3. Fill in the application details:
   - **Application name**: Dumbify (or your preferred name)
   - **Homepage URL**: `https://github.com/yourusername/dumbify` (or your repo URL)
   - **Authorization callback URL**: `dumbify://oauth/callback`
4. Click "Register application"
5. Copy your **Client ID**
6. (Optional) Generate a **Client Secret** if you plan to use a backend proxy

### 2. Configure the App

Open `/app/src/main/java/com/dumbify/auth/OAuthConfig.kt` and replace the placeholder:

```kotlin
const val CLIENT_ID = "YOUR_GITHUB_OAUTH_CLIENT_ID"
```

With your actual GitHub OAuth App Client ID:

```kotlin
const val CLIENT_ID = "Ov23liAbcDefGhIjKlMnOp"  // Your actual client ID
```

### 3. Build and Run

```bash
./gradlew assembleDebug
```

## Usage

### Connecting GitHub Account

1. Open the Dumbify app
2. Go to **Settings**
3. Select **"GitHub Copilot Pro"** as the AI provider
4. Click **"Connect GitHub Account"**
5. Authorize the app in your browser
6. You'll be redirected back to the app automatically

### Using GitHub Models

Once connected, the AI analyzer will automatically use GitHub Models for:
- Usage pattern analysis
- Daily insights
- Motivational warnings

Available models (configurable in `GitHubModelsClient.kt`):
- `gpt-4o` - OpenAI's most capable model
- `gpt-4o-mini` - Fast, cost-effective model (default)
- `claude-3.5-sonnet` - Anthropic's Claude
- `meta-llama-3.1-405b-instruct` - Meta's Llama
- `o1-preview` - OpenAI's reasoning model
- `o1-mini` - Smaller reasoning model

### Switching Between Providers

You can switch between Gemini and GitHub at any time:
1. Go to **Settings**
2. Select your preferred provider (Gemini or GitHub)
3. The selection is saved automatically

## Architecture

### Key Components

```
┌─────────────────────────────────────────┐
│         SettingsActivity                │
│  ┌───────────────────────────────────┐  │
│  │  Provider Selection UI            │  │
│  │  • Gemini API Key Input           │  │
│  │  • GitHub OAuth Button            │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│      GitHubOAuthManager                 │
│  • startOAuthFlow()                     │
│  • exchangeCodeForToken()               │
│  • verifyState() (CSRF protection)      │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│      OAuthCallbackActivity              │
│  • Handles: dumbify://oauth/callback    │
│  • Exchanges code for token             │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│      AppConfigRepository                │
│  • Stores: githubAccessToken            │
│  • Stores: selectedAiProvider           │
│  • Stores: oauthState (CSRF)            │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│      AiProviderFactory                  │
│  • Creates: GeminiProvider              │
│  • Creates: GitHubModelsProvider        │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│      AiAnalyzer                         │
│  • Uses selected provider               │
│  • Analyzes usage patterns              │
│  • Generates insights                   │
└─────────────────────────────────────────┘
```

### Files Created/Modified

**New Files:**
- `app/src/main/java/com/dumbify/auth/OAuthConfig.kt` - OAuth configuration
- `app/src/main/java/com/dumbify/auth/GitHubOAuthManager.kt` - OAuth flow handler
- `app/src/main/java/com/dumbify/api/GitHubModelsClient.kt` - GitHub Models API client
- `app/src/main/java/com/dumbify/api/AiProvider.kt` - Provider abstraction
- `app/src/main/java/com/dumbify/ui/OAuthCallbackActivity.kt` - OAuth callback handler

**Modified Files:**
- `app/build.gradle.kts` - Added networking dependencies
- `app/src/main/AndroidManifest.xml` - Added OAuth intent filter
- `app/src/main/java/com/dumbify/repository/AppConfigRepository.kt` - Added OAuth storage
- `app/src/main/java/com/dumbify/util/AiAnalyzer.kt` - Refactored to use providers
- `app/src/main/java/com/dumbify/ui/SettingsActivity.kt` - Added OAuth UI
- `app/src/main/res/layout/activity_settings.xml` - Added provider selection UI

## Security Considerations

### Current Implementation

⚠️ **Important**: The current implementation uses a **client-side OAuth flow** which requires the GitHub client secret to be embedded in the app. This is **NOT RECOMMENDED** for production use.

### Recommended Production Approach

For production, implement a **backend proxy server** that:
1. Stores the client secret securely
2. Handles token exchange server-side
3. Returns the access token to the app

Example flow:
```
App → GitHub OAuth → App receives code → 
App sends code to YOUR_BACKEND → 
Backend exchanges code for token → 
Backend returns token to app
```

Alternatively, use **GitHub Device Flow OAuth**:
- No client secret needed
- User enters a code on GitHub website
- Safer for native mobile apps
- Documentation: https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow

### Additional Security Enhancements

Consider implementing:
- **EncryptedSharedPreferences** for token storage (dependency already added)
- **Token refresh** mechanism
- **Token expiration** handling
- **Certificate pinning** for API requests

## Dependencies Added

```kotlin
// Networking for GitHub OAuth and API
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Encrypted SharedPreferences for secure token storage
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

## Testing

### Testing OAuth Flow

1. Ensure you have a valid GitHub OAuth App configured
2. Install the app: `./gradlew installDebug`
3. Open Settings and click "Connect GitHub Account"
4. Authorize in browser
5. Verify redirect back to app
6. Check Settings shows "Connected"

### Testing GitHub Models

1. Connect your GitHub account
2. Select "GitHub Copilot Pro" as provider
3. Use the app normally
4. Check logcat for API calls: `adb logcat -s GitHubModelsClient`

## Troubleshooting

### "Invalid authorization code" error
- Check that your OAuth callback URL is exactly: `dumbify://oauth/callback`
- Verify it matches in both GitHub settings and `OAuthConfig.kt`

### "Security validation failed"
- Clear app data and try again
- This is a CSRF protection error with the state parameter

### "API request failed: 401"
- Your GitHub token may be invalid or expired
- Disconnect and reconnect your account
- Ensure you have GitHub Copilot Pro subscription

### Models not accessible
- GitHub Models API requires a Copilot Pro subscription
- Check that your account has the necessary permissions
- Try using a different model in `GitHubModelsClient.kt`

## GitHub Copilot Pro Requirement

To use GitHub Models API, you need:
- **GitHub Copilot Pro subscription** ($10/month as of 2024)
- Or **GitHub Copilot for Business** (enterprise accounts)

Models available may vary based on your subscription level and GitHub's availability.

## Future Enhancements

- [ ] Implement backend proxy for secure token exchange
- [ ] Add token refresh mechanism
- [ ] Use EncryptedSharedPreferences
- [ ] Add model selection UI (choose between GPT-4o, Claude, etc.)
- [ ] Implement usage tracking and cost monitoring
- [ ] Add offline mode fallback
- [ ] Support custom system prompts per model

## Resources

- [GitHub Models Documentation](https://docs.github.com/en/github-models)
- [GitHub OAuth Apps](https://docs.github.com/en/apps/oauth-apps)
- [GitHub Copilot Pricing](https://github.com/features/copilot)
- [Azure AI Models](https://ai.azure.com/) (GitHub Models uses Azure infrastructure)

## License

Same as the main Dumbify project.
