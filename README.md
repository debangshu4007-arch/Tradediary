# Trade Diary

Android trade journal for recording trades, reviewing mistakes, tracking daily discipline, and running Gemini-based coaching over saved trade data.

## Requirements

- Android Studio
- JDK from Android Studio, or any compatible JDK configured as `JAVA_HOME`
- Android SDK with API 36 installed
- Optional: `GEMINI_API_KEY` in `.env` for AI Coach

## Local Setup

1. Open this folder in Android Studio.
2. Create `.env` in the project root:

```properties
GEMINI_API_KEY=your_gemini_api_key
```

3. Build a debug APK:

```powershell
.\gradlew.bat assembleDebug
```

4. Install it on a connected phone or emulator:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

The latest debug APK is also copied to `.build-outputs\app-debug.apk`.

## Verification

Run the host-side tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Build debug and release APKs:

```powershell
.\gradlew.bat assembleDebug assembleRelease
```

## Release APK

The default release build is unsigned unless release signing credentials are supplied.

Create a release/upload keystore:

```powershell
keytool -genkeypair -v -keystore my-upload-key.jks -storetype JKS -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Build a signed release APK:

```powershell
$env:KEYSTORE_PATH="C:\absolute\path\to\my-upload-key.jks"
$env:STORE_PASSWORD="your_store_password"
$env:KEY_PASSWORD="your_key_password"
.\gradlew.bat assembleRelease
```

The signed APK will be at:

```text
app\build\outputs\apk\release\app-release.apk
```

Verify the APK signature:

```powershell
apksigner verify --verbose app\build\outputs\apk\release\app-release.apk
```

## Play Store Shipping

For Google Play, build an Android App Bundle after setting the same release signing environment variables:

```powershell
.\gradlew.bat bundleRelease
```

Upload this file in Play Console:

```text
app\build\outputs\bundle\release\app-release.aab
```

Before production release, update `versionCode` and `versionName` in `app\build.gradle.kts`, test the signed build on a real device, and keep the upload keystore/passwords backed up securely.
