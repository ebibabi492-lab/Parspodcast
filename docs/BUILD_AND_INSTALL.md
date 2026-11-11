# راهنمای ساخت و نصب استودیوی پارس‌پادکست (ParsPodcast Studio)
# Build and Installation Guide

**نسخه:** ۱.۰.۰  
**سیستم‌عامل هدف:** بومی اندروید ۱۵ (API 35)، معماری ARM64/x86_64  
**دستگاه مرجع:** Daria Band (Dimensity 7050, 8GB RAM)  

---

## ۱. پیش‌نیازهای محیط توسعه (Prerequisites)

برای کامپایل و بیلد موفق پروژه بومی اندروید موارد زیر الزامی است:
- **Java Development Kit (JDK):** نسخه ۱۷ (OpenJDK 17 یا Eclipse Temurin 17)
- **Android SDK:**
  - `compileSdk`: 35 (Android 15 Vanilla Ice Cream)
  - `minSdk`: 26 (Android 8.0 Oreo)
  - `targetSdk`: 35
  - Android SDK Build-Tools 35.0.0
- **Android NDK:**
  - معماری‌های هدف: `arm64-v8a` و `x86_64` (مورد نیاز موتورهای محلی C++ Sherpa-ONNX و LiteRT-LM)
- **Gradle:**
  - نسخه Gradle 8.8+ با Android Gradle Plugin 8.8.0 و Kotlin 2.1.0

---

## ۲. دستورات ساخت با Gradle

از شاخه ریشه پروژه یا شاخه `android/`:

### الف) کامپایل و اجرای آزمون‌های واحد (Unit Tests)
```bash
cd android
./gradlew testDebugUnitTest
```

### ب) بیلد نسخه عیب‌یابی (Debug APK)
```bash
cd android
./gradlew assembleDebug
```
* **مسیر خروجی:** `android/app/build/outputs/apk/debug/app-debug.apk`

### ج) بیلد نسخه رسمی نهایی (Release APK)
```bash
cd android
./gradlew assembleRelease
```
* **مسیر خروجی:** `android/app/build/outputs/apk/release/app-release-unsigned.apk`

### د) بیلد بسته توزیع فروشگاه (Android App Bundle - AAB)
```bash
cd android
./gradlew bundleRelease
```
* **مسیر خروجی:** `android/app/build/outputs/bundle/release/app-release.aab`

---

## ۳. دستورالعمل نصب بر روی دستگاه Daria Band

### نصب نسخه Debug از طریق ADB:
```bash
adb connect <IP_ADDRESS>:5555  # یا اتصال از طریق کابل USB-C با فعال بودن USB Debugging
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

### نصب بدون کامپیوتر (سایدلود مستقیم):
1. فایل `app-debug.apk` را بر روی کارت حافظه یا حافظه داخلی دستگاه کپی کنید.
2. با برنامه مدیریت فایل اندروید، روی فایل ضربه زده و مجوز «نصب برنامه‌های ناشناس» را تأیید فرمایید.

---

## ۴. امضای دیجیتال نسخه نهایی (Release Signing)
به دلایل امنیتی، کلید امضای خصوصی ریلیز نباید در گیت کامیت شود. برای امضای نسخه انتشار:
```bash
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore /path/to/your/release.keystore \
  android/app/build/outputs/apk/release/app-release-unsigned.apk alias_name

zipalign -v 4 \
  android/app/build/outputs/apk/release/app-release-unsigned.apk \
  android/app/build/outputs/apk/release/parspodcast-v1.0-release.apk
```
