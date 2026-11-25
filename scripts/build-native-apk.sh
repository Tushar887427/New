#!/usr/bin/env bash
# =============================================================================
# Work'ed — build the FULL native Android APK (Jetpack Compose + Firebase)
# on a Linux or macOS machine with normal internet access. No Android Studio,
# no GitHub Actions required.
#
#   ./build/build-native-apk.sh            # release APK (signed)
#   RELEASE=false ./build/build-native-apk.sh   # debug APK only
#
# The script bootstraps everything it needs into .build/ (JDK 21, Android SDK,
# Gradle via wrapper), generates local.properties, and runs the Gradle build.
# Keystores already committed at repo root are reused:
#   my-upload-key.jks  (alias "upload", store/key password: worked2026)
#   debug.keystore     (alias "androiddebugkey", password: android)
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
BUILD_TOOLS_DIR="$ROOT/.build"
mkdir -p "$BUILD_TOOLS_DIR"

log() { printf '\n\033[1;32m==> %s\033[0m\n' "$*"; }

# ----------------------------------------------------------------- 1. Java 17+
if [ -x "$BUILD_TOOLS_DIR/jdk/bin/java" ]; then
  export JAVA_HOME="$BUILD_TOOLS_DIR/jdk"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
if ! command -v java >/dev/null 2>&1; then
  log "Downloading Temurin JDK 21 (no java found)"
  OS="$(uname -s | tr '[:upper:]' '[:lower:]')"     # linux | darwin
  ARCH="$(uname -m)"                                # x86_64 | arm64/aarch64
  case "$OS" in
    linux)
      if [ "$ARCH" = "aarch64" ]; then J="OpenJDK21U-jdk_aarch64_linux_hotspot_21.0.5_11.tar.gz"
      else J="OpenJDK21U-jdk_x64_linux_hotspot_21.0.5_11.tar.gz"; fi ;;
    darwin)
      if [ "$ARCH" = "arm64" ]; then J="OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.5_11.tar.gz"
      else J="OpenJDK21U-jdk_x64_mac_hotspot_21.0.5_11.tar.gz"; fi ;;
    *) echo "Unsupported OS for auto-JDK; install JDK 17+ and re-run." >&2; exit 1 ;;
  esac
  curl -fL "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/$J" \
       -o "$BUILD_TOOLS_DIR/jdk.tar.gz"
  mkdir -p "$BUILD_TOOLS_DIR/jdk-x"
  tar -xzf "$BUILD_TOOLS_DIR/jdk.tar.gz" -C "$BUILD_TOOLS_DIR/jdk-x" --strip-components=1
  mv "$BUILD_TOOLS_DIR/jdk-x" "$BUILD_TOOLS_DIR/jdk"
  export JAVA_HOME="$BUILD_TOOLS_DIR/jdk"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
log "Java: $(java -version 2>&1 | head -1)"

# ------------------------------------------------------------ 2. Android SDK
SDK=""
for cand in "$ANDROID_HOME" "$ANDROID_SDK_ROOT"; do
  [ -n "${cand:-}" ] && [ -d "$cand" ] && SDK="$cand" && break
done
if [ -z "$SDK" ] && [ -f local.properties ]; then
  p=$(sed -n 's/^sdk.dir=//p' local.properties | tr -d '\r')
  [ -n "$p" ] && [ -d "$p" ] && SDK="$p"
fi
if [ -z "$SDK" ]; then
  SDK="$BUILD_TOOLS_DIR/android-sdk"
  log "Bootstrapping Android SDK into $SDK"
  OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
  case "$OS" in
    linux)  CT="commandlinetools-linux-11076708_latest.zip" ;;
    darwin) CT="commandlinetools-mac-11076708_latest.zip" ;;
    *) echo "Use build-native-apk.ps1 on Windows." >&2; exit 1 ;;
  esac
  mkdir -p "$SDK/cmdline-tools"
  curl -fL "https://dl.google.com/android/repository/$CT" -o "$BUILD_TOOLS_DIR/ct.zip"
  unzip -q "$BUILD_TOOLS_DIR/ct.zip" -d "$BUILD_TOOLS_DIR/ct-x"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$BUILD_TOOLS_DIR/ct-x/cmdline-tools" "$SDK/cmdline-tools/latest"
  SDKM="$SDK/cmdline-tools/latest/bin/sdkmanager"
  yes | "$SDKM" --sdk_root="$SDK" --licenses >/dev/null || true
  "$SDKM" --sdk_root="$SDK" "platform-tools" >/dev/null
  if "$SDKM" --sdk_root="$SDK" "platforms;android-36.1" >/dev/null 2>&1; then
    log "Installed platform android-36.1"
  else
    log "android-36.1 not available — falling back to android-36 (patching compileSdk)"
    "$SDKM" --sdk_root="$SDK" "platforms;android-36" >/dev/null
    sed -i.bak 's/compileSdk { version = release(36) { minorApiLevel = 1 } }/compileSdk = 36/' app/build.gradle.kts
  fi
  "$SDKM" --sdk_root="$SDK" "build-tools;36.0.0" >/dev/null || true
  "$SDKM" --sdk_root="$SDK" "build-tools;35.0.0" >/dev/null || true
  echo "sdk.dir=$SDK" > local.properties
fi
log "Android SDK: $SDK"

# ------------------------------------------------------ 3. env + keystores
[ -f .env ] || cp .env.example .env
[ -f my-upload-key.jks ] || keytool -genkeypair -keystore my-upload-key.jks -alias upload \
  -storepass worked2026 -keypass worked2026 -keyalg RSA -keysize 2048 -validity 10950 \
  -dname "CN=Work'ed, OU=Worked, O=Worked, C=IN"
[ -f debug.keystore ] || keytool -genkeypair -keystore debug.keystore -alias androiddebugkey \
  -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10950 \
  -dname "CN=Android Debug,O=Android,C=US"

# ------------------------------------------------------------- 4. Gradle build
export STORE_PASSWORD="${STORE_PASSWORD:-worked2026}"
export KEY_PASSWORD="${KEY_PASSWORD:-worked2026}"
export KEYSTORE_PATH="$ROOT/my-upload-key.jks"

log "Building with Gradle (first run downloads Gradle 9.3.1 + Maven dependencies)"
chmod +x gradlew
if [ "${RELEASE:-true}" = "true" ]; then
  ./gradlew --no-daemon assembleRelease
else
  ./gradlew --no-daemon assembleDebug
fi

# ------------------------------------------------------------------ 5. Collect
mkdir -p apk
if [ "${RELEASE:-true}" = "true" ]; then
  cp app/build/outputs/apk/release/app-release.apk apk/Worked-release.apk
  log "DONE: apk/Worked-release.apk"
else
  cp app/build/outputs/apk/debug/app-debug.apk apk/Worked-debug.apk
  log "DONE: apk/Worked-debug.apk"
fi
ls -la apk/
