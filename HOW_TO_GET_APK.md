# How to Get the Dumbify APK

## If GitHub Actions is Working

1. **Check Build Status**:
   - Go to: https://github.com/alektebel/dumbify/actions
   - Look for the latest "Build Android APK" workflow run
   - Wait for it to complete (usually 5-10 minutes)

2. **Download from Artifacts**:
   - Click on the completed workflow run
   - Scroll down to "Artifacts" section
   - Download `app-debug` or `app-release`
   - Extract the ZIP file to get the APK

## If GitHub Actions Fails

Don't worry! You can still build the APK locally very easily.

### Quick Local Build

**Requirements**:
- Android Studio (recommended) OR
- Java 17 + Android SDK command-line tools

**Steps**:

#### Option 1: Using Android Studio (Easiest)

1. **Install Android Studio**:
   ```
   Download from: https://developer.android.com/studio
   ```

2. **Clone and Open Project**:
   ```bash
   git clone https://github.com/alektebel/dumbify.git
   cd dumbify
   ```
   
3. **Open in Android Studio**:
   - File → Open → Select `dumbify` folder
   - Wait for Gradle sync (3-5 minutes first time)

4. **Build APK**:
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - APK location: `app/build/outputs/apk/debug/app-debug.apk`

#### Option 2: Command Line (For Linux/Mac)

1. **Install Prerequisites**:
   ```bash
   # Ubuntu/Debian
   sudo apt update
   sudo apt install openjdk-17-jdk wget unzip
   
   # macOS (using Homebrew)
   brew install openjdk@17
   ```

2. **Install Android SDK Command Line Tools**:
   ```bash
   # Download
   wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
   
   # Create directory
   mkdir -p ~/Android/cmdline-tools
   
   # Extract
   unzip commandlinetools-linux-9477386_latest.zip -d ~/Android/cmdline-tools
   mv ~/Android/cmdline-tools/cmdline-tools ~/Android/cmdline-tools/latest
   
   # Set environment variables (add to ~/.bashrc or ~/.zshrc)
   export ANDROID_HOME=~/Android
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   
   # Reload shell
   source ~/.bashrc  # or source ~/.zshrc
   
   # Install required SDK components
   sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   yes | sdkmanager --licenses
   ```

3. **Clone and Build**:
   ```bash
   git clone https://github.com/alektebel/dumbify.git
   cd dumbify
   ./build.sh
   ```

4. **Get APK**:
   - Location: `app/build/outputs/apk/debug/app-debug.apk`

#### Option 3: Windows Command Line

1. **Install Java 17**:
   - Download from: https://adoptium.net/
   - Install and add to PATH

2. **Install Android SDK**:
   - Download Android Studio (easiest way to get SDK)
   - Or download command-line tools from: https://developer.android.com/studio#command-tools

3. **Set Environment Variables**:
   ```cmd
   set ANDROID_HOME=C:\Users\YourName\AppData\Local\Android\Sdk
   set PATH=%PATH%;%ANDROID_HOME%\platform-tools
   ```

4. **Build**:
   ```cmd
   git clone https://github.com/alektebel/dumbify.git
   cd dumbify
   gradlew.bat assembleDebug
   ```

5. **Get APK**:
   - Location: `app\build\outputs\apk\debug\app-debug.apk`

## Pre-Built APK (If Available)

I can manually build and upload the APK to GitHub Releases:

1. Check: https://github.com/alektebel/dumbify/releases
2. Download the latest `app-debug.apk`
3. Install on your device

## Install APK on Android Device

Once you have the APK:

1. **Transfer to Phone**:
   - Via USB cable
   - Via email/cloud storage
   - Direct download on phone

2. **Enable Unknown Sources**:
   - Settings → Security → Install Unknown Apps
   - Allow your browser/file manager to install apps

3. **Install**:
   - Tap the APK file
   - Follow installation prompts
   - Grant permissions

4. **Run Dumbify**:
   - Open the app
   - Grant Usage Stats permission
   - Enable Accessibility Service
   - Enable DNS Filter
   - Start monitoring!

## Troubleshooting Build Issues

### "SDK location not found"
**Fix**: Set ANDROID_HOME environment variable
```bash
export ANDROID_HOME=/path/to/android/sdk
```

### "Java version mismatch"
**Fix**: Use Java 17
```bash
java -version  # Should show 17.x.x
```

### "Gradle download failed"
**Fix**: The gradle wrapper should download automatically, but if it fails:
```bash
# The gradle-wrapper.jar is already in the repository
# Just run: ./gradlew build
```

### "Permission denied on gradlew"
**Fix**:
```bash
chmod +x gradlew
```

## Quick Build Summary

**Fastest method**: Install Android Studio, open project, click Build → Build APK

**Most reliable method**: Android Studio

**Smallest download**: Command-line tools (but more complex setup)

## Need Help?

1. Read BUILD_INSTRUCTIONS.md in the repository
2. Check QUICKSTART.md for step-by-step guide
3. Open an issue on GitHub
4. The build process is straightforward once you have Android Studio installed

---

**Bottom Line**: Building locally with Android Studio is actually faster and easier than troubleshooting GitHub Actions!

**Time estimate**: 
- Android Studio install: 10 minutes
- First build: 5 minutes
- Total: 15 minutes to APK
