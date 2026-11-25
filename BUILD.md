# Building Work'ed — APK guide

This repo contains the **Work'ed** Android app (AI Studio export: Kotlin +
Jetpack Compose + Firebase). There are two ways to get an APK out of it:

| Artifact | What it is | How to get it |
|---|---|---|
| `apk/Worked-preview.apk` | **Ready now.** Installable WebView wrapper around the app's bundled HTML preview (`public/assets/aistudio/Work_ed_af60d700.html`) | Already built — download & install |
| `apk/Worked-release.apk` | **The real native app** (full Compose + Firebase build) | Run `scripts/build-native-apk.sh` (Linux/macOS) or `scripts/build-native-apk.ps1` (Windows) on a machine with normal internet access |

---

## 1. The preview APK (already built)

`apk/Worked-preview.apk` (~830 KB) is a self-contained APK built **inside this
sandbox without Google's servers**, by repackaging an open-source WebView
template app:

- loads `file:///android_asset/index.html` (the Work'ed HTML preview bundled in this repo)
- JS + DOM storage enabled, progress bar, offline error page, back-button history
- package `app.worked.webview`, label **Work'ed**, generated launcher icon
- minSdk 18, targetSdk 29 → installs on Android 4.3 → Android 16
- signed (JAR/v1) with `apk/preview-sign.jks` (store password: `worked`)

**Install:** copy the APK to your phone and open it ("install unknown apps"
permission may be needed), or `adb install apk/Worked-preview.apk`.

**Limitations:** it runs the *web preview* of the app, not the native Kotlin
UI — so CDN assets (Tailwind/Google Fonts) need internet, and features that
call Gemini/Firestore server APIs behave like the browser preview.

Rebuild it any time with:

```bash
python3 scripts/preview/build_wrapper_apk.py <template.apk> <index.html> out.apk
python3 scripts/preview/sign_apk.py out.apk Worked-preview.apk apk/preview-sign.jks worked
```

(The wrapper template APK and tool sources are documented in
`scripts/preview/README.md`.)

---

## 2. The full native APK (one command, your machine)

The complete Compose + Firebase build needs Google Maven / Maven Central /
services.gradle.org / dl.google.com, which this sandbox cannot reach (its
network is restricted to GitHub, npm and PyPI). On **your** machine those are
reachable, and the committed script bootstraps the entire toolchain
automatically — JDK 21, Android SDK (platform 36.1, build-tools), Gradle 9.3.1
via wrapper, licenses, keystores — then builds:

```bash
# Linux / macOS
./scripts/build-native-apk.sh            # → apk/Worked-release.apk (signed)
RELEASE=false ./scripts/build-native-apk.sh   # debug APK instead

# Windows (PowerShell)
powershell -ExecutionPolicy Bypass -File scripts\build-native-apk.ps1
```

Requirements: ~6 GB free disk, internet, and ~10–20 minutes on first run
(Gradle downloads all dependencies). Nothing else is required — no Android
Studio, no GitHub Actions.

Everything the Gradle build needs is already committed here:

- signing key `my-upload-key.jks` — alias **upload**, store/key password **worked2026**
  (override with `STORE_PASSWORD` / `KEY_PASSWORD` env vars)
- debug keystore `debug.keystore` — alias **androiddebugkey**, password **android**
- Gradle wrapper (`gradlew`, `gradle/wrapper/gradle-wrapper.jar` + properties)
- `app/google-services.json`, `.env.example` for the secrets plugin

If `platforms;android-36.1` is not available on your SDK mirror, the script
falls back to `android-36` and patches `app/build.gradle.kts` automatically.

### Building in Android Studio instead
Open the repo as an existing project → *Build → Generate Signed App Bundle /
APK → APK* → use `my-upload-key.jks` (password `worked2026`, alias `upload`)
→ release. Or just press Run with a device attached.

---

## 3. Why two paths?

The sandbox that produced this branch has an egress allow-list (GitHub, npm,
PyPI only). Building the native app requires resolving ~300 Maven artifacts
from `dl.google.com` / `repo.maven.apache.org`, which are blocked there — the
Gradle build cannot even start. The preview APK avoids all of that by using a
prebuilt template APK plus self-contained patch/sign tooling (pure Python +
openssl), so an installable APK could still be produced in-sandbox. The
native build is fully prepared and runs wherever the network allows it.
