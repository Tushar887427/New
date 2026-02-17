# Building Android APK

This guide explains how to build the Android APK for the School Management System app.

## Prerequisites

- Node.js 18+ and npm
- Java JDK 17+
- Android Studio (optional, for local development)

## Building Locally

### 1. Install Dependencies
```bash
npm install
```

### 2. Build Web Assets
```bash
npm run build
```

### 3. Sync Capacitor
```bash
npm run cap:sync
```

### 4. Build APK
```bash
cd android
./gradlew assembleDebug
```

The APK will be generated at: `android/app/build/outputs/apk/debug/app-debug.apk`

## Building via GitHub Actions

The APK is automatically built via GitHub Actions on every push to main branches.

### Automatic Builds

1. **Push to branch**: APK is built automatically
2. **View workflow**: Go to Actions tab in GitHub
3. **Download APK**: Click on the workflow run and download the artifact

### Manual Build

You can trigger a manual build:

1. Go to GitHub repository
2. Click on "Actions" tab
3. Select "Build Android APK" workflow
4. Click "Run workflow"
5. Select branch and click "Run workflow"

### Downloading APK

After the workflow completes:

1. Click on the completed workflow run
2. Scroll to "Artifacts" section
3. Download "school-management-debug.apk"

## Release Builds

To create a signed release APK:

1. Tag your commit with a version:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. The workflow will build both debug and release APKs

3. For production, you'll need to sign the release APK:
   - Generate a keystore
   - Configure signing in `android/app/build.gradle`
   - Add secrets to GitHub Actions

## APK Signing (Production)

### Generate Keystore

```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

### Configure GitHub Secrets

Add these secrets to your GitHub repository:

- `KEYSTORE_FILE`: Base64 encoded keystore file
- `KEYSTORE_PASSWORD`: Keystore password
- `KEY_ALIAS`: Key alias
- `KEY_PASSWORD`: Key password

### Update build.gradle

Add signing configuration in `android/app/build.gradle`:

```gradle
android {
    signingConfigs {
        release {
            storeFile file(System.getenv("KEYSTORE_FILE"))
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS")
            keyPassword System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

## App Configuration

### App Details
- **App Name**: School Management
- **Package ID**: com.school.management
- **Version**: Defined in `android/app/build.gradle`

### Customization

Edit `capacitor.config.json` to customize:
- App name
- Package ID
- Server settings
- Plugins

## Troubleshooting

### Build Fails
- Check Node.js and Java versions
- Clean build: `cd android && ./gradlew clean`
- Rebuild: `npm run cap:build`

### APK Not Working
- Check permissions in `AndroidManifest.xml`
- Verify network security config
- Test on physical device if emulator fails

### Capacitor Issues
- Update Capacitor: `npm update @capacitor/android @capacitor/cli @capacitor/core`
- Sync again: `npm run cap:sync`

## Testing

### Install on Device

1. Enable "Unknown Sources" on Android device
2. Transfer APK to device
3. Install and test

### Using ADB

```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

## Additional Resources

- [Capacitor Documentation](https://capacitorjs.com/docs)
- [Android Developer Guide](https://developer.android.com/guide)
- [GitHub Actions for Android](https://github.com/actions/setup-java)
