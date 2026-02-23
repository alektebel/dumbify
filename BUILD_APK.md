# Building the APK

This guide explains how to build the Dumbify APK file for distribution.

## Prerequisites

1. **Android SDK** installed with the following components:
   - Android SDK Platform 34
   - Android SDK Build-Tools 34.0.0
   - Android SDK Command-line Tools

2. **Java Development Kit (JDK)** 17 or higher

## Setup

### 1. Accept Android SDK Licenses

If you haven't already, accept the Android SDK licenses:

```bash
sdkmanager --licenses
```

Or on Windows:
```cmd
sdkmanager.bat --licenses
```

Press 'y' to accept all licenses.

### 2. Configure SDK Location

Create a `local.properties` file in the project root (if it doesn't exist):

```properties
sdk.dir=/path/to/your/android-sdk
```

Common SDK locations:
- **Linux**: `/home/username/Android/Sdk` or `/usr/lib/android-sdk`
- **macOS**: `/Users/username/Library/Android/sdk`
- **Windows**: `C:\Users\username\AppData\Local\Android\Sdk`

## Building the APK

### Debug Build (for testing)

```bash
./gradlew assembleDebug
```

The APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Release Build (for distribution)

```bash
./gradlew assembleRelease
```

The APK will be generated at:
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

**Note**: Release builds should be signed for distribution.

## Signing the APK (Production)

For production releases, you need to sign the APK with your keystore:

### 1. Create a Keystore (first time only)

```bash
keytool -genkey -v -keystore dumbify-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias dumbify-key
```

**Important**: Keep this file secure and never commit it to Git!

### 2. Configure Signing in build.gradle.kts

Add to `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../dumbify-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "dumbify-key"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... existing config
        }
    }
}
```

### 3. Build Signed Release

```bash
export KEYSTORE_PASSWORD=your_keystore_password
export KEY_PASSWORD=your_key_password
./gradlew assembleRelease
```

## Adding APK to GitHub Releases

### Option 1: Manual Upload (Recommended)

1. Build the release APK
2. Go to your GitHub repository
3. Click "Releases" → "Create a new release"
4. Upload the APK file
5. Add release notes

### Option 2: Commit to Repository

If you want to commit the APK directly to the repo:

1. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

2. Copy to releases directory:
   ```bash
   mkdir -p releases
   cp app/build/outputs/apk/debug/app-debug.apk releases/dumbify-v1.0-debug.apk
   ```

3. Commit and push:
   ```bash
   git add releases/dumbify-v1.0-debug.apk
   git commit -m "Add APK build v1.0"
   git push
   ```

**Note**: The `.gitignore` has been configured to allow APKs in the `releases/` directory.

## Automated Building with GitHub Actions

Create `.github/workflows/build-apk.yml`:

```yaml
name: Build APK

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
    
    - name: Build Debug APK
      run: ./gradlew assembleDebug
    
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
    
    - name: Create Release
      uses: softprops/action-gh-release@v1
      with:
        files: app/build/outputs/apk/debug/app-debug.apk
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

This will automatically build and release APKs when you push a version tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Verifying the Build

After building, verify the APK:

```bash
# Check APK info
aapt dump badging app/build/outputs/apk/debug/app-debug.apk

# List APK contents
unzip -l app/build/outputs/apk/debug/app-debug.apk

# Check APK size
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting

### "SDK location not found"
- Ensure `local.properties` exists with correct `sdk.dir` path
- Verify Android SDK is installed

### "License not accepted"
- Run `sdkmanager --licenses` and accept all

### "Build failed" or compilation errors
- Clean build: `./gradlew clean`
- Invalidate caches and rebuild
- Check that all dependencies are available

### Gradle daemon issues
- Stop all daemons: `./gradlew --stop`
- Try again

## Build Variants

The project supports multiple build variants:

- **debug** - Debugging enabled, logs visible, not optimized
- **release** - Optimized, ProGuard enabled, signed for distribution

## Next Steps

After building:
1. Test the APK on a physical device
2. Run through all features with GitHub OAuth
3. Test on multiple Android versions (API 26+)
4. Create release notes documenting changes
5. Upload to GitHub Releases or Google Play Store

## Resources

- [Android Developer Guide - Build Your App](https://developer.android.com/studio/build)
- [Signing Your App](https://developer.android.com/studio/publish/app-signing)
- [GitHub Actions for Android](https://github.com/marketplace?type=actions&query=android)
