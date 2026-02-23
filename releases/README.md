# Dumbify APK Releases

This directory contains pre-built APK files for easy installation on Android devices.

## Current Release

The APK will be automatically built by GitHub Actions when you:
1. Push to the main/master branch
2. Create a version tag (e.g., `v1.0.0`)

## Download Latest APK

### Option 1: GitHub Releases (Recommended)
Go to the [Releases](https://github.com/yourusername/dumbify/releases) page to download the latest APK.

### Option 2: GitHub Actions Artifacts
1. Go to [Actions](https://github.com/yourusername/dumbify/actions)
2. Click on the latest successful build
3. Download the `dumbify-debug-apk` artifact

## Manual Build

If you prefer to build the APK yourself, see [BUILD_APK.md](../BUILD_APK.md) for instructions.

## Installation

1. **Download** the `app-debug.apk` file
2. **Enable** "Install from Unknown Sources" on your Android device:
   - Settings → Security → Unknown Sources (or Install Unknown Apps)
3. **Transfer** the APK to your device (if downloaded on PC)
4. **Install** by opening the APK file on your device
5. **Grant permissions** when prompted:
   - Usage Stats Access
   - Accessibility Service
   - Notifications
   - VPN (for DNS filtering)

## Creating a Release

To create a new release with APK:

```bash
# Tag the release
git tag -a v1.0.0 -m "Release version 1.0.0"

# Push the tag
git push origin v1.0.0
```

GitHub Actions will automatically:
- Build the APK
- Create a GitHub Release
- Attach the APK to the release

## Version History

| Version | Date | Changes | Download |
|---------|------|---------|----------|
| v1.0.0  | TBD  | Initial release with GitHub OAuth | [Download](https://github.com/yourusername/dumbify/releases/tag/v1.0.0) |

## Verification

After downloading, verify the APK:

### Check APK Details
```bash
aapt dump badging app-debug.apk | grep -E 'package|versionCode|versionName'
```

### Expected Output
```
package: name='com.dumbify' versionCode='1' versionName='1.0'
```

## Security Notes

- **Debug APKs** are signed with a debug keystore and should only be used for testing
- **Production releases** should use a proper release keystore
- Always download APKs from official sources only
- Verify the APK signature if security is a concern

## Troubleshooting

### "App not installed" error
- Uninstall any previous version first
- Ensure you have enough storage space
- Enable "Install from Unknown Sources"

### "Parse Error" 
- File may be corrupted during download
- Re-download the APK
- Ensure your Android version is 8.0 or higher

### Permissions not working
- Go to Settings → Apps → Dumbify
- Manually grant required permissions
- Restart the app

## Support

For issues or questions:
- [Open an Issue](https://github.com/yourusername/dumbify/issues)
- See [GITHUB_OAUTH_SETUP.md](../GITHUB_OAUTH_SETUP.md) for OAuth setup
- See [BUILD_APK.md](../BUILD_APK.md) for build instructions
