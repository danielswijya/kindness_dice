# Kindness Dice

> yup...shake your phone for 5.5 seconds - and receive a random act of kindness.

![Platform](https://img.shields.io/badge/platform-Android-green)
![React Native](https://img.shields.io/badge/React%20Native-0.79-blue)
![Gemini](https://img.shields.io/badge/AI-Gemini%202.5%20Flash-orange)

---

## What it does

Kindness Dice is an Android app that turns a simple physical gesture into a daily kindness prompt. Shake your phone vigorously for 5.5 seconds — the app detects the motion, buzzes with haptic feedback, rolls a dice sound, then shows a fullscreen overlay with a warm, AI-generated act of kindness powered by Google Gemini.

The shake detector runs as a persistent foreground service, so it works even when the app is in the background or the screen is off. It auto-starts on device boot.

---

## How it works

```
Shake (5.5s sustained) → Foreground Service detects motion
                       → Settling haptic fires
                       → 1.6s delay
                       → WindowManager overlay appears
                       → Gemini 2.5 Flash generates kindness prompt
                       → Overlay displays result
```

- **Shake detection** uses `TYPE_LINEAR_ACCELERATION` sensor at game rate (threshold: 12 m/s², grace period: 500ms)
- **Overlay** is drawn directly via `WindowManager` using `TYPE_APPLICATION_OVERLAY` — works over any app, even lock screen
- **Gemini call** is made from a background thread inside the service; result updates the overlay in real time
- **Auto-start** via `BootReceiver` listening to `BOOT_COMPLETED` and `LOCKED_BOOT_COMPLETED`

---

## Tech stack

| Layer | Technology |
|---|---|
| UI | React Native (TypeScript) |
| Native service | Kotlin foreground service |
| Shake detection | Android `SensorManager` |
| Overlay | Android `WindowManager` |
| AI | Google Gemini 2.5 Flash REST API |
| Storage | AsyncStorage (onboarding state) |

---

## Getting started

### Prerequisites

- Node.js 18+
- JDK 17+
- Android SDK (API 33+)
- Android device or emulator with shake sensor support
- A [Google AI Studio](https://aistudio.google.com) API key with Gemini enabled

### Setup

```bash
# 1. Clone the repo
git clone https://github.com/danielswijya/kindness_dice.git
cd kindness_dice

# 2. Install JS dependencies
npm install

# 3. Add your Gemini API key
echo "GEMINI_API_KEY=your_key_here" >> android/local.properties

# 4. Build and install on device
cd android && ./gradlew app:installDebug
```

### First launch

On first launch the app walks you through three permission steps:

1. **Notifications** — required for the foreground service indicator
2. **Battery optimization** — disable to prevent Android from killing the shake detector
3. **Appear on top** — required for the fullscreen overlay

---

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Gemini API calls |
| `VIBRATE` | Haptic feedback during shake |
| `RECEIVE_BOOT_COMPLETED` | Auto-start service on device boot |
| `FOREGROUND_SERVICE` | Run shake detector in background |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required for custom foreground service type |
| `POST_NOTIFICATIONS` | Show foreground service notification |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Keep service alive in background |
| `SYSTEM_ALERT_WINDOW` | Draw fullscreen overlay over other apps |

---

## Project structure

```
├── App.tsx                          # Root component, onboarding gate
├── src/
│   ├── screens/
│   │   ├── FirstLaunchScreen.tsx    # Onboarding permission flow
│   │   └── SettingsScreen.tsx       # Main screen, service toggle
│   └── native/
│       └── ServiceModule.ts         # JS bridge type definitions
└── android/app/src/main/
    ├── AndroidManifest.xml
    └── java/com/kindnessdice/
        ├── KindnessDiceService.kt   # Foreground service, shake detection, overlay, Gemini
        ├── KindnessOverlayActivity.kt # Fullscreen overlay activity (unused by service)
        ├── ServiceModule.kt         # React Native native module
        ├── ServicePackage.kt        # RN package registration
        ├── BootReceiver.kt          # Auto-start on boot
        ├── MainActivity.kt
        └── MainApplication.kt
```

---

## License

MIT
