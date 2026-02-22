#!/bin/bash

# Dumbify Build Script
# This script builds the APK for the Dumbify Android app

echo "=========================================="
echo "  Dumbify Android App Build Script"
echo "=========================================="
echo ""

# Check if Android SDK is available
if [ -z "$ANDROID_HOME" ]; then
    echo "ERROR: ANDROID_HOME is not set!"
    echo ""
    echo "Please install Android SDK and set ANDROID_HOME environment variable."
    echo "Example: export ANDROID_HOME=/home/user/Android/Sdk"
    echo ""
    echo "Alternative: Open this project in Android Studio and build there."
    exit 1
fi

echo "Android SDK found at: $ANDROID_HOME"
echo ""

# Make sure gradlew is executable
chmod +x ./gradlew

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean

# Build debug APK
echo ""
echo "Building debug APK..."
./gradlew assembleDebug

# Check if build succeeded
if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "  BUILD SUCCESSFUL!"
    echo "=========================================="
    echo ""
    echo "Debug APK location:"
    echo "  app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "To install on connected device:"
    echo "  ./gradlew installDebug"
    echo ""
    echo "Or manually:"
    echo "  adb install app/build/outputs/apk/debug/app-debug.apk"
    echo ""
else
    echo ""
    echo "=========================================="
    echo "  BUILD FAILED!"
    echo "=========================================="
    echo ""
    echo "Please check the error messages above."
    echo ""
    exit 1
fi

# Optional: Build release APK
read -p "Do you want to build release APK? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "Building release APK..."
    ./gradlew assembleRelease
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "Release APK location:"
        echo "  app/build/outputs/apk/release/app-release-unsigned.apk"
        echo ""
        echo "Note: This APK is unsigned. To sign it, you need a keystore."
        echo "See: https://developer.android.com/studio/publish/app-signing"
        echo ""
    fi
fi

echo ""
echo "Build process completed!"
