# =============================================================================
# Work'ed — build the FULL native Android APK (Jetpack Compose + Firebase)
# on Windows with normal internet access. No Android Studio, no GitHub Actions.
#
#   powershell -ExecutionPolicy Bypass -File build\build-native-apk.ps1
# =============================================================================
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root
$Tools = Join-Path $Root ".build"
New-Item -ItemType Directory -Force -Path $Tools | Out-Null

function Log($m) { Write-Host "`n==> $m" -ForegroundColor Green }

# --------------------------------------------------------------- 1. Java 17+
$JdkDir = Join-Path $Tools "jdk"
if (-not (Test-Path (Join-Path $JdkDir "bin\java.exe"))) {
  if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Log "Downloading Temurin JDK 21 (no java found)"
    $url = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/OpenJDK21U-jdk_x64_windows_hotspot_21.0.5_11.zip"
    Invoke-WebRequest -Uri $url -OutFile (Join-Path $Tools "jdk.zip")
    Expand-Archive (Join-Path $Tools "jdk.zip") -DestinationPath (Join-Path $Tools "jdk-x") -Force
    $inner = Get-ChildItem (Join-Path $Tools "jdk-x") | Select-Object -First 1
    Move-Item $inner.FullName $JdkDir
  }
}
if (Test-Path (Join-Path $JdkDir "bin\java.exe")) {
  $env:JAVA_HOME = $JdkDir
  $env:Path = "$JdkDir\bin;$env:Path"
}
Log ("Java: " + ((java -version 2>&1 | Select-Object -First 1)))

# ------------------------------------------------------------ 2. Android SDK
$Sdk = $env:ANDROID_HOME
if (-not $Sdk) { $Sdk = $env:ANDROID_SDK_ROOT }
if (-not $Sdk -and (Test-Path "local.properties")) {
  $p = (Get-Content local.properties | Where-Object { $_ -like "sdk.dir=*" }) -replace "sdk.dir=", ""
  if ($p -and (Test-Path $p)) { $Sdk = $p }
}
if (-not $Sdk) {
  $Sdk = Join-Path $Tools "android-sdk"
  Log "Bootstrapping Android SDK into $Sdk"
  $ct = "commandlinetools-windows-11076708_latest.zip"
  Invoke-WebRequest -Uri "https://dl.google.com/android/repository/$ct" -OutFile (Join-Path $Tools "ct.zip")
  Expand-Archive (Join-Path $Tools "ct.zip") -DestinationPath (Join-Path $Tools "ct-x") -Force
  New-Item -ItemType Directory -Force -Path (Join-Path $Sdk "cmdline-tools") | Out-Null
  Move-Item (Join-Path $Tools "ct-x\cmdline-tools") (Join-Path $Sdk "cmdline-tools\latest")
  $sdkm = Join-Path $Sdk "cmdline-tools\latest\bin\sdkmanager.bat"
  Yes | & $sdkm --sdk_root=$Sdk --licenses | Out-Null
  & $sdkm --sdk_root=$Sdk "platform-tools" | Out-Null
  try { & $sdkm --sdk_root=$Sdk "platforms;android-36.1" | Out-Null; Log "Installed platform android-36.1" }
  catch {
    Log "android-36.1 not available - falling back to android-36"
    & $sdkm --sdk_root=$Sdk "platforms;android-36" | Out-Null
    (Get-Content app\build.gradle.kts) -replace [regex]::Escape('compileSdk { version = release(36) { minorApiLevel = 1 } }'), 'compileSdk = 36' | Set-Content app\build.gradle.kts
  }
  & $sdkm --sdk_root=$Sdk "build-tools;36.0.0" | Out-Null
  & $sdkm --sdk_root=$Sdk "build-tools;35.0.0" | Out-Null
  "sdk.dir=$Sdk" | Set-Content local.properties
}
Log "Android SDK: $Sdk"

# ------------------------------------------------------ 3. env + keystores
if (-not (Test-Path ".env")) { Copy-Item ".env.example" ".env" }
if (-not (Test-Path "my-upload-key.jks")) {
  & keytool -genkeypair -keystore my-upload-key.jks -alias upload -storepass worked2026 -keypass worked2026 -keyalg RSA -keysize 2048 -validity 10950 -dname "CN=Work'ed, OU=Worked, O=Worked, C=IN"
}
if (-not (Test-Path "debug.keystore")) {
  & keytool -genkeypair -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10950 -dname "CN=Android Debug,O=Android,C=US"
}

# ------------------------------------------------------------- 4. Gradle build
$env:STORE_PASSWORD = "worked2026"
$env:KEY_PASSWORD   = "worked2026"
$env:KEYSTORE_PATH  = Join-Path $Root "my-upload-key.jks"

Log "Building with Gradle (first run downloads Gradle 9.3.1 + Maven dependencies)"
.\gradlew.bat --no-daemon assembleRelease
New-Item -ItemType Directory -Force -Path apk | Out-Null
Copy-Item app\build\outputs\apk\release\app-release.apk apk\Worked-release.apk -Force
Log "DONE: apk\Worked-release.apk"
Get-ChildItem apk
