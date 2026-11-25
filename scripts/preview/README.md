# Work'ed preview APK — how it was built

`apk/Worked-preview.apk` was produced **fully offline from GitHub-hosted
sources** (this sandbox cannot reach dl.google.com / Maven). Pipeline:

1. **Template APK** — open-source WebView template
   [`rupok/webview`](https://github.com/rupok/webview) (`WebView.apk`,
   io.codetic.webview, appcompat v7, MIT-style sample). Cloned via git
   (real blobs, not LFS).
2. **`build_wrapper_apk.py`** — binary-patches the APK:
   - `classes.dex`: `io/codetic/webview` → `app/worked/webview` (18→18 chars,
     in-place), const-string `http://codetic.net/demo/webview-app/` →
     `file:///android_asset/index.html`, recomputes SHA-1 (offset 32..) and
     Adler-32 (offset 12..) per the DEX spec.
   - `AndroidManifest.xml` (AXML): package string → `app.worked.webview`
     (UTF-16 pool, same length), `targetSdkVersion` 21 → **29**,
     `minSdkVersion` 11 → **18** (chunk-walk attribute patch).
   - `resources.arsc`: package name (UTF-16) + app name `WebView` → `Work'ed`
     (7→7 chars).
   - launcher icons replaced with a generated Work'ed icon (Pillow).
   - adds `assets/index.html` = `public/assets/aistudio/Work_ed_af60d700.html`
     (the app's own bundled web preview).
   - drops the old `META-INF/*` signature.
3. **`sign_apk.py`** — re-signs with the JAR (v1) scheme:
   self-signed RSA-2048 cert in `apk/preview-sign.jks`
   (store password `worked`), SHA-1 entry digests, SHA-256 PKCS#7 via
   `openssl smime -sign -binary -outform DER`.
   v1-only is accepted by Android installers because targetSdk < 30.

Verified with androguard (manifest/dex re-parse, bytecode disassembly shows
`setJavaScriptEnabled(true)` + `loadUrl("file:///android_asset/index.html")`)
and `openssl smime -verify` (signature chain OK).

Rebuild:

```bash
git clone --depth 1 https://github.com/rupok/webview.git   # template source
python3 build_wrapper_apk.py webview/WebView.apk \
    ../../public/assets/aistudio/Work_ed_af60d700.html out.apk
python3 sign_apk.py out.apk ../../apk/Worked-preview.apk \
    ../../apk/preview-sign.jks worked
```

Deps: `pip install pillow` (androguard optional, for verification).
