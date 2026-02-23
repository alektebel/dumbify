# Dumbify - Digital Wellbeing Android App

A powerful Android app that helps you stay focused and productive by intelligently monitoring app usage, blocking distracting content, and providing AI-powered insights.

## Features

### 1. Smart App Usage Monitoring
- Real-time tracking of all app usage
- Automatic categorization of apps (productive vs. distracting)
- Configurable time limits per app
- Warning notifications when approaching limits
- Automatic app closing when limits are exceeded

### 2. AI-Powered Analysis (Multiple Providers)
**Choose your AI provider:**
- **Google Gemini** - Free tier available, easy API key setup
- **GitHub Copilot Pro** - Access GPT-4o, Claude 3.5 Sonnet, Llama 3.1, and more

**Features:**
- Real-time insights about your app usage behavior
- Personalized recommendations to improve focus
- Daily summary reports with actionable suggestions
- Context-aware warnings based on your patterns
- Switch between AI providers anytime in settings

**Supported Models (GitHub Copilot):**
- GPT-4o & GPT-4o-mini (OpenAI)
- Claude 3.5 Sonnet (Anthropic)
- Meta Llama 3.1 (Meta)
- o1-preview & o1-mini (OpenAI reasoning models)

### 3. DNS-Based Content Filtering
- Built-in VPN service for DNS filtering
- Blocks access to immoral and distracting websites
- Pre-configured blocklists for:
  - Gambling sites
  - Adult content
  - Other time-wasting sites
- Easy to add custom blocked domains

### 4. Productivity Focus
Pre-configured productive apps include:
- WhatsApp (for communication)
- Kindle and Google Books (for reading)
- X/Twitter (limited for posting)
- LinkedIn (limited for networking)
- Notion, Evernote (for productivity)
- Outlook and Google Docs

### 5. Customizable Time Limits
- Set daily limits for social media apps
- Configure warning thresholds
- Enable/disable automatic app closing
- Separate limits for different app categories

## Installation

### Quick Install (Automated Builds)

**Download Pre-Built APK:**
1. Go to [Releases](https://github.com/alektebel/dumbify/releases)
2. Download the latest `app-debug.apk`
3. Enable "Install from Unknown Sources" in Android settings
4. Install the APK on your device

**Note**: APKs are automatically built on every commit via GitHub Actions.

### Build from Source

#### Prerequisites
- Android 8.0 (API 26) or higher
- Android Studio Arctic Fox or newer (for development)
- Java 17

### Build Instructions

1. Clone this repository:
```bash
git clone https://github.com/yourusername/dumbify.git
cd dumbify
```

2. Open the project in Android Studio

3. Sync Gradle files

4. Build and run on your device or emulator:
```bash
./gradlew installDebug
```

### APK Installation

If you have a pre-built APK:
1. Enable "Install from Unknown Sources" in your Android settings
2. Copy the APK to your device
3. Open and install it

## Setup and Configuration

### First Launch

1. **Grant Usage Stats Permission**
   - Settings → Apps → Special Access → Usage Access
   - Enable "Dumbify"

2. **Enable Accessibility Service**
   - Settings → Accessibility
   - Find and enable "Dumbify"
   - This allows the app to automatically close apps when limits are exceeded

3. **Configure VPN for DNS Filtering**
   - Tap "Enable DNS Filter" in the app
   - Accept the VPN connection request
   - This creates a local VPN to filter DNS requests

4. **Set Up AI Features**
   
   **Option A: Google Gemini (Free)**
   - Get a free API key from [Google AI Studio](https://ai.google.dev)
   - Go to Settings in Dumbify
   - Select "Google Gemini" as AI provider
   - Enter your Gemini API key
   - Save settings
   
   **Option B: GitHub Copilot Pro (Recommended)**
   - Requires [GitHub Copilot Pro subscription](https://github.com/features/copilot) ($10/month)
   - Follow the detailed setup guide: [GITHUB_OAUTH_SETUP.md](GITHUB_OAUTH_SETUP.md)
   - Go to Settings in Dumbify
   - Select "GitHub Copilot Pro" as AI provider
   - Click "Connect GitHub Account"
   - Authorize in your browser
   - Get access to: GPT-4o, Claude 3.5 Sonnet, Llama 3.1, o1-preview, and more!

### App Configuration

#### Default Settings

The app comes with sensible defaults:

**Productive Apps (No Limits):**
- WhatsApp
- Kindle
- Google Books
- Notion
- Evernote
- GitHub

**Social Media (30-minute daily limit):**
- X/Twitter
- LinkedIn

**Entertainment (15-minute daily limit):**
- Instagram
- Facebook

**Blocked:**
- TikTok

#### Customizing App Limits

Edit the configuration in `AppConfigRepository.kt`:

```kotlin
// Example: Change LinkedIn limit to 45 minutes
configs["com.linkedin.android"] = AppConfig(
    packageName = "com.linkedin.android",
    category = AppCategory.SOCIAL_MEDIA,
    timeLimit = 45,  // minutes
    warningThreshold = 35,
    autoClose = true
)
```

#### Adding Blocked Domains

Edit the blocked domains list in `AppConfigRepository.kt`:

```kotlin
BlockedDomain("example.com", "custom_category")
```

## How It Works

### Architecture

```
┌─────────────────────────────────────────────┐
│           Dumbify Android App               │
├─────────────────────────────────────────────┤
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │   UsageMonitorService                │  │
│  │   - Tracks app usage via UsageStats  │  │
│  │   - Monitors time limits             │  │
│  │   - Triggers AI analysis             │  │
│  └──────────────────────────────────────┘  │
│                    │                        │
│                    ▼                        │
│  ┌──────────────────────────────────────┐  │
│  │   AiAnalyzer                         │  │
│  │   - Gemini AI integration            │  │
│  │   - Pattern analysis                 │  │
│  │   - Smart warnings                   │  │
│  └──────────────────────────────────────┘  │
│                    │                        │
│                    ▼                        │
│  ┌──────────────────────────────────────┐  │
│  │   NotificationManager                │  │
│  │   - Warning notifications            │  │
│  │   - AI insights                      │  │
│  │   - Time limit alerts                │  │
│  └──────────────────────────────────────┘  │
│                    │                        │
│                    ▼                        │
│  ┌──────────────────────────────────────┐  │
│  │   AppControlAccessibilityService     │  │
│  │   - Closes apps when limit reached   │  │
│  │   - Returns to home screen           │  │
│  └──────────────────────────────────────┘  │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │   DnsFilterService (VPN)             │  │
│  │   - Intercepts DNS requests          │  │
│  │   - Blocks immoral/distracting sites │  │
│  │   - Returns NXDOMAIN for blocked     │  │
│  └──────────────────────────────────────┘  │
│                                             │
└─────────────────────────────────────────────┘
```

### Components

1. **UsageMonitorService**: Foreground service that continuously monitors app usage using Android's UsageStatsManager

2. **AiAnalyzer**: Integrates with Google's Gemini AI to provide intelligent insights about usage patterns

3. **DnsFilterService**: VPN-based DNS filtering to block access to distracting websites at the network level

4. **AppControlAccessibilityService**: Uses Android Accessibility API to automatically close apps when time limits are exceeded

5. **AppConfigRepository**: Manages app configurations, time limits, and blocked domains using SharedPreferences

## Privacy & Security

### Data Storage
- All data is stored locally on your device
- No cloud sync or external data transmission
- Preferences stored in encrypted SharedPreferences

### AI Processing
- AI analysis only occurs when you provide an API key
- Requests sent directly to Google's Gemini API
- No intermediary servers
- You can disable AI features entirely

### Permissions Explained

| Permission | Purpose |
|------------|---------|
| `PACKAGE_USAGE_STATS` | Track which apps you use and for how long |
| `FOREGROUND_SERVICE` | Keep monitoring service running in background |
| `POST_NOTIFICATIONS` | Send time limit warnings and insights |
| `BIND_ACCESSIBILITY_SERVICE` | Automatically close apps when limits exceeded |
| `BIND_VPN_SERVICE` | Filter DNS requests to block websites |
| `INTERNET` | Required for DNS filtering and AI analysis |

## Customization

### Adding New Productive Apps

1. Find the app's package name using:
```bash
adb shell pm list packages | grep -i "app_name"
```

2. Add to `DEFAULT_PRODUCTIVE_APPS` in `AppConfigRepository.kt`:
```kotlin
val DEFAULT_PRODUCTIVE_APPS = listOf(
    "com.your.app.package",
    // ... existing apps
)
```

### Modifying AI Prompts

Edit the prompts in `AiAnalyzer.kt`:
```kotlin
private fun buildUsagePrompt(...): String {
    return """
        Your custom prompt here
    """.trimIndent()
}
```

### Changing Check Interval

In `UsageMonitorService.kt`:
```kotlin
companion object {
    private const val CHECK_INTERVAL = 5000L // milliseconds
}
```

## Troubleshooting

### App closing doesn't work
- Ensure Accessibility Service is enabled
- Some apps may prevent being closed programmatically
- The app will still send notifications and warnings

### DNS filtering not blocking sites
- Verify VPN is active (check notification bar)
- Some apps use hardcoded DNS or DNS-over-HTTPS
- Add domains to the blocklist in `AppConfigRepository.kt`

### AI features not working
- Verify you have entered a valid Gemini API key
- Check your internet connection
- Review API quota at Google AI Studio

### Notifications not appearing
- Grant notification permission
- Disable battery optimization for Dumbify
- Check Do Not Disturb settings

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Future Enhancements

- [ ] Statistics dashboard with graphs
- [ ] Weekly/monthly usage reports
- [ ] Export data to CSV
- [ ] Widget for quick status view
- [ ] Scheduled focus modes
- [ ] Integration with calendar apps
- [ ] Group app limits (e.g., all social media combined)
- [ ] Offline AI using TensorFlow Lite
- [ ] Dark mode theme
- [ ] Multi-user profiles
- [x] GitHub OAuth integration for Copilot Pro
- [x] Multiple AI provider support
- [x] Automated APK builds via GitHub Actions

## Documentation

- **[GITHUB_OAUTH_SETUP.md](GITHUB_OAUTH_SETUP.md)** - Complete guide for setting up GitHub OAuth and accessing Copilot Pro models
- **[BUILD_APK.md](BUILD_APK.md)** - Instructions for building the APK from source
- **[APK_SETUP_SUMMARY.md](APK_SETUP_SUMMARY.md)** - Overview of automated build and release process
- **[releases/README.md](releases/README.md)** - Information about APK releases and downloads

## License

MIT License - feel free to use and modify for your needs

## Disclaimer

This app is designed to help with digital wellbeing. It requires several sensitive permissions to function properly. Use at your own discretion. The developers are not responsible for any data loss or privacy concerns.

## Credits

- Built with Kotlin and Android SDK
- AI powered by Google Gemini
- Icons from Material Design

## Support

For issues, questions, or feature requests, please open an issue on GitHub.

---

**Stay focused. Stay productive. Dumbify your phone.**
