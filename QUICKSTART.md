# Quick Start Guide

## For End Users

### Installation from Source

1. **Install Android Studio**
   - Download from https://developer.android.com/studio
   - Install and open Android Studio

2. **Clone and Build**
   ```bash
   git clone https://github.com/yourusername/dumbify.git
   cd dumbify
   ```

3. **Open in Android Studio**
   - File → Open → Select the dumbify folder
   - Wait for Gradle sync to complete

4. **Connect Your Phone**
   - Enable Developer Options on your Android device
   - Enable USB Debugging
   - Connect via USB

5. **Run the App**
   - Click the green "Run" button in Android Studio
   - Or run: `./gradlew installDebug`

### First-Time Setup

After installation:

1. **Enable Usage Stats**
   - Open Dumbify
   - When prompted, tap "Grant"
   - In Android Settings, find and enable "Dumbify"

2. **Enable Accessibility**
   - Tap "Enable Accessibility Service"
   - In Accessibility settings, enable "Dumbify"

3. **Enable DNS Filter**
   - Toggle "Enable DNS Filter"
   - Accept the VPN connection request

4. **Configure AI (Optional)**
   - Get API key from https://ai.google.dev
   - Tap Settings → Enter API key → Save

5. **Start Monitoring**
   - Toggle "Start Monitoring"
   - App will now track usage and send notifications

## For Developers

### Setup Development Environment

```bash
# Clone repository
git clone https://github.com/yourusername/dumbify.git
cd dumbify

# Open in Android Studio or use command line
./gradlew build

# Run tests
./gradlew test

# Install on connected device
./gradlew installDebug

# View logs
adb logcat | grep Dumbify
```

### Project Structure

```
dumbify/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/dumbify/
│   │       │   ├── model/          # Data models
│   │       │   ├── service/        # Background services
│   │       │   ├── ui/             # Activities and UI
│   │       │   ├── repository/     # Data management
│   │       │   ├── util/           # Utilities and AI
│   │       │   └── receiver/       # Broadcast receivers
│   │       ├── res/                # Resources
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

### Key Files to Modify

- **App limits**: `repository/AppConfigRepository.kt`
- **AI prompts**: `util/AiAnalyzer.kt`
- **Blocked domains**: `repository/AppConfigRepository.kt`
- **Check interval**: `service/UsageMonitorService.kt`
- **UI layouts**: `res/layout/*.xml`

### Testing

1. **Manual Testing**
   ```bash
   # Install and run
   ./gradlew installDebug
   
   # Open an app and wait 5 seconds
   # Check if Dumbify detected it
   adb logcat | grep "UsageMonitor"
   ```

2. **Test Notifications**
   - Install app
   - Use a limited app (e.g., Instagram) for 10+ minutes
   - Should receive warning notification

3. **Test App Closing**
   - Use a limited app until time limit
   - App should return to home screen

4. **Test DNS Filtering**
   - Enable DNS filter
   - Try accessing a blocked domain in browser
   - Should see connection error

## Customization Examples

### Add a New Productive App

1. Find package name:
   ```bash
   adb shell pm list packages | grep notion
   # Output: package:notion.id
   ```

2. Add to `AppConfigRepository.kt`:
   ```kotlin
   val DEFAULT_PRODUCTIVE_APPS = listOf(
       "notion.id",  // Add this line
       "com.whatsapp",
       // ... rest
   )
   ```

### Change Social Media Limit

In `AppConfigRepository.kt`:
```kotlin
// Change from 30 to 60 minutes
configs["com.twitter.android"] = AppConfig(
    packageName = "com.twitter.android",
    category = AppCategory.SOCIAL_MEDIA,
    timeLimit = 60,  // Changed
    warningThreshold = 45,  // Changed
    autoClose = true
)
```

### Add Custom Blocked Domain

In `AppConfigRepository.kt`:
```kotlin
val DEFAULT_BLOCKED_DOMAINS = listOf(
    BlockedDomain("reddit.com", "distraction"),  // Add this
    BlockedDomain("bet365.com", "gambling"),
    // ... rest
)
```

## Troubleshooting

### Build Errors

```bash
# Clean and rebuild
./gradlew clean
./gradlew build

# Update Gradle wrapper
./gradlew wrapper --gradle-version=8.2
```

### Permission Errors

- Ensure all permissions are granted in Android Settings
- Check AndroidManifest.xml for required permissions
- Some features need special access (Usage Stats, Accessibility)

### Service Not Starting

```bash
# Check if service is running
adb shell dumpsys activity services | grep Dumbify

# Force stop and restart
adb shell am force-stop com.dumbify
./gradlew installDebug
```

### AI Not Working

- Verify API key is saved (Settings → API Key)
- Check internet connection
- View logs: `adb logcat | grep AiAnalyzer`
- Verify API quota at Google AI Studio

## Getting Help

- Check the main README.md
- Review code comments
- Open an issue on GitHub
- Check Android documentation

## Next Steps

After basic setup:
1. Customize app limits for your needs
2. Add your frequently used apps to productive list
3. Configure DNS blocklist
4. Set up AI with your API key
5. Monitor for a few days and adjust limits

---

Happy coding!
