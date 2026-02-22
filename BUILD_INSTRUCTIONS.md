# Building the Dumbify APK

## Prerequisites

### Option 1: Android Studio (Recommended for Beginners)

1. **Download Android Studio**
   - Visit: https://developer.android.com/studio
   - Download and install for your OS
   - Minimum version: Arctic Fox (2020.3.1) or newer

2. **Install Android SDK**
   - Open Android Studio
   - Go to Tools → SDK Manager
   - Install:
     - Android SDK Platform 34 (Android 14)
     - Android SDK Build-Tools 34.0.0
     - Android SDK Command-line Tools

### Option 2: Command Line (For Advanced Users)

1. **Install Java JDK 17**
   ```bash
   # Ubuntu/Debian
   sudo apt install openjdk-17-jdk
   
   # macOS (using Homebrew)
   brew install openjdk@17
   
   # Windows
   # Download from: https://adoptium.net/
   ```

2. **Install Android SDK**
   ```bash
   # Download Android SDK command-line tools
   wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
   
   # Extract
   unzip commandlinetools-linux-9477386_latest.zip -d ~/Android
   
   # Set environment variable
   export ANDROID_HOME=~/Android/sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   
   # Install required components
   sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   ```

## Building with Android Studio

1. **Open Project**
   ```
   File → Open → Select 'dumbify' folder
   ```

2. **Wait for Gradle Sync**
   - Android Studio will automatically sync Gradle
   - This may take a few minutes on first run
   - Downloads required dependencies

3. **Build APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

4. **Locate APK**
   - Once build completes, click "locate" in the notification
   - Or find it at: `app/build/outputs/apk/debug/app-debug.apk`

5. **Install on Device**
   - Connect Android device via USB
   - Enable USB Debugging on device
   - Click Run (green play button) in Android Studio
   - Or use: `Run → Run 'app'`

## Building from Command Line

### Quick Build

```bash
# Make build script executable
chmod +x build.sh

# Run build script
./build.sh
```

### Manual Build

```bash
# Clean previous builds
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# APK will be at:
# app/build/outputs/apk/debug/app-debug.apk

# Build release APK (unsigned)
./gradlew assembleRelease

# Release APK will be at:
# app/build/outputs/apk/release/app-release-unsigned.apk
```

### Install on Device

```bash
# Install debug APK
./gradlew installDebug

# Or manually with adb
adb install app/build/outputs/apk/debug/app-debug.apk
```

## APK Output Locations

After successful build:

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Building Signed Release APK

For production/distribution, you need a signed APK:

1. **Create Keystore**
   ```bash
   keytool -genkey -v -keystore dumbify-release.keystore \
     -alias dumbify -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Create keystore.properties**
   ```
   storePassword=YOUR_KEYSTORE_PASSWORD
   keyPassword=YOUR_KEY_PASSWORD
   keyAlias=dumbify
   storeFile=../dumbify-release.keystore
   ```

3. **Update app/build.gradle.kts**
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               val keystorePropertiesFile = rootProject.file("keystore.properties")
               val keystoreProperties = Properties()
               keystoreProperties.load(FileInputStream(keystorePropertiesFile))
               
               storeFile = file(keystoreProperties["storeFile"] as String)
               storePassword = keystoreProperties["storePassword"] as String
               keyAlias = keystoreProperties["keyAlias"] as String
               keyPassword = keystoreProperties["keyPassword"] as String
           }
       }
       
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
               isMinifyEnabled = true
               proguardFiles(...)
           }
       }
   }
   ```

4. **Build Signed Release**
   ```bash
   ./gradlew assembleRelease
   ```

## Troubleshooting

### Build Fails: "SDK location not found"

**Solution**: Set ANDROID_HOME
```bash
export ANDROID_HOME=/path/to/android/sdk
```

### Build Fails: "Java version mismatch"

**Solution**: Use Java 17
```bash
# Check Java version
java -version

# Set JAVA_HOME if needed
export JAVA_HOME=/path/to/jdk-17
```

### Gradle Download Fails

**Solution**: Download manually
```bash
# Download Gradle 8.2
wget https://services.gradle.org/distributions/gradle-8.2-bin.zip

# Extract to gradle/wrapper/
mkdir -p gradle/wrapper
unzip gradle-8.2-bin.zip -d gradle/wrapper/
```

### Build Takes Too Long

**Solution**: Increase Gradle memory
```bash
# Create/edit gradle.properties
echo "org.gradle.jvmargs=-Xmx2048m -XX:MaxPermSize=512m" >> gradle.properties
```

### "Permission Denied" on gradlew

**Solution**: Make it executable
```bash
chmod +x gradlew
```

### Missing Android SDK Components

**Solution**: Install via sdkmanager
```bash
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

## Alternative: Build APK Online

If you don't want to install Android Studio locally:

1. **GitHub Actions** (requires GitHub repository)
   - Push code to GitHub
   - Set up GitHub Actions workflow
   - APK builds automatically on push

2. **Appetize.io** (online Android emulator)
   - Upload APK
   - Test in browser

3. **Build APK as a Service**
   - Use services like CircleCI, Travis CI
   - Requires configuration

## Verifying the APK

After building:

```bash
# Check APK info
aapt dump badging app/build/outputs/apk/debug/app-debug.apk

# Check APK size
ls -lh app/build/outputs/apk/debug/app-debug.apk

# List APK contents
unzip -l app/build/outputs/apk/debug/app-debug.apk
```

## Next Steps After Building

1. **Install on Device**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Grant Permissions**
   - Open Dumbify app
   - Follow permission prompts
   - Enable Usage Stats
   - Enable Accessibility Service

3. **Configure**
   - Add Gemini API key (optional)
   - Enable monitoring
   - Enable DNS filter

4. **Test**
   - Use a social media app
   - Check if notifications appear
   - Verify time limits work

## FAQ

**Q: Can I build without Android Studio?**
A: Yes, use command line with Android SDK tools.

**Q: How big is the APK?**
A: Debug APK: ~5-10 MB, Release APK: ~3-5 MB (with ProGuard)

**Q: Can I build on Windows?**
A: Yes, use `gradlew.bat` instead of `./gradlew`

**Q: Do I need root to install?**
A: No, standard Android installation works.

**Q: Can I sideload the APK?**
A: Yes, enable "Unknown Sources" in Android settings.

## Support

If you encounter issues:
1. Check error messages carefully
2. Google the specific error
3. Check Android Studio's "Build" tab for details
4. Review Gradle logs in `build/` directory
5. Open an issue on GitHub

---

Happy building!
