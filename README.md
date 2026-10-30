# CarTV Free (Open-Source Car Media & Screen Mirroring)

A 100% free, open-source alternative to [CarTV](https://cartv.app/) with zero in-app purchases, zero ads, and zero subscriptions.

Contains full native projects for both **iOS (iPhone / CarPlay)** and **Android (Head Units / Phones / Tablets)**.

---

## Features

- **M3U / M3U8 IPTV Player:** Stream thousands of free live TV channels, sports, and international broadcasts.
- **Car Screen Mirroring:** Stream your phone screen at 30 fps to your car dashboard or Tesla browser over local Wi-Fi / Personal Hotspot (`http://<phone-ip>:8080`).
- **Picture-in-Picture (PiP):** Keep watching live streams in a corner window while navigating.
- **Offline / Local Media:** Hardware-accelerated video playback with touch-friendly vehicle controls.
- **100% Local & Private:** No user tracking, no telemetry, and no accounts required.

---

## Automated Cloud Build (100% Free)

You do not need an Android SDK, Xcode, or a Mac computer on your local machine to build the apps. The included GitHub Actions workflow builds both binaries automatically in the cloud.

### Step 1: Create a GitHub Repository and Push

Run these commands in PowerShell or Terminal inside this directory:

```bash
git init
git add .
git commit -m "Initial commit of CarTV Free"
gh repo create cartv-free --public --source=. --push
```

### Step 2: Download Your Free Binaries

1. Go to your new repository on GitHub: `https://github.com/<your-username>/cartv-free`
2. Click the **Actions** tab.
3. Select the latest run of **Build CarTV (Android & iOS)**.
4. Under **Artifacts** at the bottom of the page, download:
   - `CarTV-iOS-IPA` (for iPhone)
   - `CarTV-Android-APK` (for Android)

---

## How to Install on iPhone (No Developer Account Required)

1. Download [Sideloadly](https://sideloadly.io/) on your Windows PC or Mac.
2. Connect your iPhone via USB cable.
3. Drag the downloaded `CarTV.ipa` into Sideloadly.
4. Enter your standard free Apple ID email.
5. Click **Start**. The app will install onto your iPhone.
6. On your iPhone, go to **Settings > General > VPN & Device Management** and tap **Trust** on your profile.

---

## How to Install on Android

1. Transfer the downloaded `app-debug.apk` to your Android phone, tablet, or car head unit (via USB drive, browser download, or local network).
2. Tap the APK file to install (allow "Install from Unknown Sources" if prompted).

---

## How to Use Car Screen Mirroring

1. Enable **Personal Hotspot** on your phone (or connect both your phone and car display to the same Wi-Fi).
2. Open **CarTV** on your phone and tap **"Start Car Mirroring"** (or **"Mirror to Car"**).
3. On your car display's web browser, open the URL displayed on your phone (e.g., `http://172.20.10.1:8080`).
4. Your phone's screen will mirror onto your car dashboard in real time.
