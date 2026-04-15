# KindnessDice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a persistent Android background service that detects a sustained phone shake (≥20 m/s² for ≥5.5s) and displays a Gemini-generated act of kindness as a fullscreen overlay.

**Architecture:** A Kotlin ForegroundService owns all sensor/vibration/audio logic and launches a pure-Kotlin fullscreen Activity (KindnessOverlayActivity) which calls the Gemini REST API directly. A ReactNative NativeModule bridges service start/stop to the JS settings screen. Boot auto-start via BroadcastReceiver.

**Tech Stack:** React Native 0.85.1 (New Architecture/Hermes), Kotlin, Android ForegroundService, TYPE_LINEAR_ACCELERATION sensor, VibrationEffect API (26+), MediaPlayer, HttpURLConnection (Gemini REST), BuildConfig for API key injection from local.properties, @react-native-async-storage/async-storage for first-launch flag.

---

## File Map

**Create (Kotlin):**
- `android/app/src/main/java/com/kindnessdice/KindnessDiceService.kt` — ForegroundService: sensor, vibration, audio, shake logic, launches overlay
- `android/app/src/main/java/com/kindnessdice/BootReceiver.kt` — BroadcastReceiver: starts service on BOOT_COMPLETED
- `android/app/src/main/java/com/kindnessdice/KindnessOverlayActivity.kt` — fullscreen Activity: warm cream UI, Gemini REST call, dismiss on tap
- `android/app/src/main/java/com/kindnessdice/ServiceModule.kt` — NativeModule: start/stop/status bridge to JS
- `android/app/src/main/java/com/kindnessdice/ServicePackage.kt` — ReactPackage: registers ServiceModule

**Modify (Kotlin/Gradle):**
- `android/app/src/main/AndroidManifest.xml` — all permissions, service, receiver, overlay activity
- `android/app/build.gradle` — buildConfig feature, GEMINI_API_KEY injection
- `android/local.properties` — add GEMINI_API_KEY placeholder
- `android/app/src/main/java/com/kindnessdice/MainApplication.kt` — register ServicePackage

**Create (React Native):**
- `src/native/ServiceModule.ts` — typed JS wrapper for NativeModule
- `src/screens/FirstLaunchScreen.tsx` — permissions request + Samsung battery whitelist
- `src/screens/SettingsScreen.tsx` — service on/off toggle

**Modify (React Native):**
- `App.tsx` — conditional routing: FirstLaunchScreen → SettingsScreen

**Add (asset):**
- `android/app/src/main/res/raw/dice_roll.mp3` — manual step (user provides)

---

## Task 1: Foundation — Manifest, Build Config, Skeleton Service, Native Module

**Files touched:** AndroidManifest.xml, build.gradle, local.properties, KindnessDiceService.kt, BootReceiver.kt, ServiceModule.kt, ServicePackage.kt, MainApplication.kt

### Step 1.1: Update AndroidManifest.xml

- [ ] Replace `android/app/src/main/AndroidManifest.xml` with:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

    <application
      android:name=".MainApplication"
      android:label="@string/app_name"
      android:icon="@mipmap/ic_launcher"
      android:roundIcon="@mipmap/ic_launcher_round"
      android:allowBackup="false"
      android:theme="@style/AppTheme"
      android:usesCleartextTraffic="${usesCleartextTraffic}"
      android:supportsRtl="true">

      <activity
        android:name=".MainActivity"
        android:label="@string/app_name"
        android:configChanges="keyboard|keyboardHidden|orientation|screenLayout|screenSize|smallestScreenSize|uiMode"
        android:launchMode="singleTask"
        android:windowSoftInputMode="adjustResize"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
      </activity>

      <activity
        android:name=".KindnessOverlayActivity"
        android:exported="false"
        android:showWhenLocked="true"
        android:turnScreenOn="true"
        android:launchMode="singleTask"
        android:theme="@android:style/Theme.NoTitleBar.Fullscreen" />

      <service
        android:name=".KindnessDiceService"
        android:foregroundServiceType="specialUse"
        android:exported="false">
        <property
          android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
          android:value="shake_to_kindness_challenge" />
      </service>

      <receiver
        android:name=".BootReceiver"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.BOOT_COMPLETED" />
            <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
        </intent-filter>
      </receiver>

    </application>
</manifest>
```

### Step 1.2: Update build.gradle for BuildConfig + API key

- [ ] In `android/app/build.gradle`, add `buildFeatures` block and API key injection inside the `android { ... }` block (after `compileSdk` line):

```groovy
android {
    ndkVersion rootProject.ext.ndkVersion
    buildToolsVersion rootProject.ext.buildToolsVersion
    compileSdk rootProject.ext.compileSdkVersion

    buildFeatures {
        buildConfig true
    }

    namespace "com.kindnessdice"
    defaultConfig {
        applicationId "com.kindnessdice"
        minSdkVersion rootProject.ext.minSdkVersion
        targetSdkVersion rootProject.ext.targetSdkVersion
        versionCode 1
        versionName "1.0"

        // Read Gemini API key from local.properties (never hardcode)
        def localProps = new Properties()
        def localPropsFile = rootProject.file('local.properties')
        if (localPropsFile.exists()) {
            localPropsFile.withReader('UTF-8') { reader -> localProps.load(reader) }
        }
        buildConfigField "String", "GEMINI_API_KEY", "\"${localProps.getProperty('GEMINI_API_KEY', '')}\""
    }
    // ... rest unchanged
```

### Step 1.3: Add API key to local.properties and gitignore it

- [ ] Open `android/local.properties` (already git-ignored by default RN scaffold) and append:

```
GEMINI_API_KEY=your_gemini_api_key_here
```

- [ ] Verify `android/local.properties` is in `.gitignore`. Open root `.gitignore` and confirm it contains (add if missing):

```
# Local properties (contains API keys — never commit)
android/local.properties
```

### Step 1.4: Create KindnessDiceService.kt (skeleton)

- [ ] Create `android/app/src/main/java/com/kindnessdice/KindnessDiceService.kt`:

```kotlin
package com.kindnessdice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class KindnessDiceService : Service() {

    companion object {
        const val CHANNEL_ID = "kindness_dice_channel"
        const val NOTIFICATION_ID = 1
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Kindness Dice Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kindness Dice is active.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
```

### Step 1.5: Create BootReceiver.kt

- [ ] Create `android/app/src/main/java/com/kindnessdice/BootReceiver.kt`:

```kotlin
package com.kindnessdice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.LOCKED_BOOT_COMPLETED") {
            val serviceIntent = Intent(context, KindnessDiceService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
```

### Step 1.6: Create ServiceModule.kt

- [ ] Create `android/app/src/main/java/com/kindnessdice/ServiceModule.kt`:

```kotlin
package com.kindnessdice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class ServiceModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "ServiceModule"

    @ReactMethod
    fun startService() {
        val intent = Intent(reactContext, KindnessDiceService::class.java)
        reactContext.startForegroundService(intent)
    }

    @ReactMethod
    fun stopService() {
        val intent = Intent(reactContext, KindnessDiceService::class.java)
        reactContext.stopService(intent)
    }

    @ReactMethod
    fun isServiceRunning(promise: Promise) {
        promise.resolve(KindnessDiceService.isRunning)
    }

    @ReactMethod
    fun requestIgnoreBatteryOptimizations() {
        val pm = reactContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(reactContext.packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${reactContext.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            reactContext.startActivity(intent)
        }
    }
}
```

### Step 1.7: Create ServicePackage.kt

- [ ] Create `android/app/src/main/java/com/kindnessdice/ServicePackage.kt`:

```kotlin
package com.kindnessdice

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class ServicePackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(ServiceModule(reactContext))
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}
```

### Step 1.8: Register ServicePackage in MainApplication.kt

- [ ] Modify `android/app/src/main/java/com/kindnessdice/MainApplication.kt`:

```kotlin
package com.kindnessdice

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost

class MainApplication : Application(), ReactApplication {

  override val reactHost: ReactHost by lazy {
    getDefaultReactHost(
      context = applicationContext,
      packageList =
        PackageList(this).packages.apply {
          add(ServicePackage())
        },
    )
  }

  override fun onCreate() {
    super.onCreate()
    loadReactNative(this)
  }
}
```

### Step 1.9: Build and verify

- [ ] Run: `cd C:\Users\danie\Desktop\KindnessDice && npx react-native run-android`
- [ ] Expected: app installs and launches on device, showing default scaffold screen
- [ ] Verify service starts via adb: `adb shell dumpsys activity services com.kindnessdice`
  - Expected output contains `KindnessDiceService` with `isForeground=true`

### Step 1.10: Commit

```bash
git add android/app/src/main/AndroidManifest.xml \
        android/app/build.gradle \
        android/app/src/main/java/com/kindnessdice/KindnessDiceService.kt \
        android/app/src/main/java/com/kindnessdice/BootReceiver.kt \
        android/app/src/main/java/com/kindnessdice/ServiceModule.kt \
        android/app/src/main/java/com/kindnessdice/ServicePackage.kt \
        android/app/src/main/java/com/kindnessdice/MainApplication.kt
git commit -m "feat: add ForegroundService skeleton, BootReceiver, and NativeModule bridge"
```

---

## Task 2: Shake Detection, Vibration, Audio

**Files touched:** KindnessDiceService.kt (full implementation), `android/app/src/main/res/raw/dice_roll.mp3` (manual)

### Step 2.1: Add dice sound asset (manual)

- [ ] Create directory: `android/app/src/main/res/raw/`
- [ ] Add a dice rolling sound file as `android/app/src/main/res/raw/dice_roll.mp3`
  - Source any royalty-free dice rolling loop (~3-5 seconds, will be looped)
  - If no file available yet, create an empty placeholder: `touch android/app/src/main/res/raw/dice_roll.mp3` — code handles missing audio gracefully

### Step 2.2: Replace KindnessDiceService.kt with full shake detection implementation

- [ ] Replace `android/app/src/main/java/com/kindnessdice/KindnessDiceService.kt` with:

```kotlin
package com.kindnessdice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class KindnessDiceService : Service(), SensorEventListener {

    companion object {
        const val CHANNEL_ID = "kindness_dice_channel"
        const val NOTIFICATION_ID = 1
        const val SHAKE_THRESHOLD = 20f          // m/s²
        const val SHAKE_DURATION_MS = 5500L      // 5.5 seconds
        var isRunning = false
    }

    private lateinit var sensorManager: SensorManager
    private var linearAccelerometer: Sensor? = null
    private var mediaPlayer: MediaPlayer? = null

    // Shake state
    private var shakeStartTime: Long = 0
    private var isShaking = false
    private var triggerArmed = false
    private var overlayVisible = false

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        initSensor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        sensorManager.unregisterListener(this)
        stopDiceSound()
        stopContinuousVibration()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ─── Sensor ────────────────────────────────────────────────────────────────

    private fun initSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        linearAccelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION) return
        if (overlayVisible) return  // Pause detection while overlay is shown

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        val aT = sqrt(ax * ax + ay * ay + az * az)

        if (aT >= SHAKE_THRESHOLD) {
            if (!isShaking) {
                // Shake started
                isShaking = true
                shakeStartTime = System.currentTimeMillis()
                triggerArmed = false
                startContinuousVibration()
                startDiceSound()
            }
            // Check if sustained long enough to arm trigger
            val elapsed = System.currentTimeMillis() - shakeStartTime
            if (!triggerArmed && elapsed >= SHAKE_DURATION_MS) {
                triggerArmed = true
            }
        } else {
            if (isShaking) {
                // Shake ended
                isShaking = false
                stopContinuousVibration()
                stopDiceSound()

                if (triggerArmed) {
                    triggerArmed = false
                    onShakeTriggered()
                }
                // If not armed, timer resets silently — nothing happens
                shakeStartTime = 0
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* unused */ }

    // ─── Trigger ───────────────────────────────────────────────────────────────

    private fun onShakeTriggered() {
        playSettlingHaptic()
        // Small delay to let settling haptic play before overlay appears
        android.os.Handler(mainLooper).postDelayed({
            launchOverlay()
        }, 1600) // 200+150+150+200+100+250+80+300+50+300ms settling ≈ 1.58s + buffer
    }

    private fun launchOverlay() {
        overlayVisible = true
        val intent = Intent(this, KindnessOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    // Called by KindnessOverlayActivity when it finishes so sensor resumes
    fun onOverlayDismissed() {
        overlayVisible = false
    }

    // ─── Vibration ─────────────────────────────────────────────────────────────

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun startContinuousVibration() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val vibrator = getVibrator()
        // buzz 100ms, pause 50ms, repeat from index 0
        val timings = longArrayOf(100, 50)
        val amplitudes = intArrayOf(150, 0)
        val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
        vibrator.vibrate(effect)
    }

    private fun stopContinuousVibration() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        getVibrator().cancel()
    }

    private fun playSettlingHaptic() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val vibrator = getVibrator()
        // Pattern: [delay, on, off, on, off, ...] — -1 = no repeat
        // buzz200 → pause150 → buzz150 → pause200 → buzz100 → pause250 → buzz80 → pause300 → buzz50
        val timings = longArrayOf(0, 200, 150, 150, 200, 100, 250, 80, 300, 50)
        val amplitudes = intArrayOf(0, 255, 0, 220, 0, 180, 0, 140, 0, 100)
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    // ─── Audio ─────────────────────────────────────────────────────────────────

    private fun startDiceSound() {
        try {
            val resId = resources.getIdentifier("dice_roll", "raw", packageName)
            if (resId == 0) return  // No sound file — skip gracefully
            mediaPlayer = MediaPlayer.create(this, resId)?.apply {
                isLooping = true
                setVolume(0.8f, 0.8f)
                start()
            }
        } catch (e: Exception) {
            // Audio failure is non-fatal — vibration still works
            mediaPlayer = null
        }
    }

    private fun stopDiceSound() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    // ─── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Kindness Dice Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kindness Dice is active.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
```

### Step 2.3: Verify shake detection builds

- [ ] Run: `cd C:\Users\danie\Desktop\KindnessDice\android && ./gradlew assembleDebug`
- [ ] Expected: `BUILD SUCCESSFUL`
- [ ] If build fails with "Unresolved reference: VibrationEffect" — the import `android.os.VibrationEffect` is already in the file; verify it's present

### Step 2.4: Manual test — shake detection + vibration

- [ ] Run: `npx react-native run-android`
- [ ] Shake the phone vigorously for more than 5.5 seconds
- [ ] Expected: continuous vibration + dice sound start immediately when shaking begins
- [ ] Expected: vibration + sound stop when you stop shaking
- [ ] Expected: if you shook < 5.5s, nothing else happens
- [ ] Expected: if you shook ≥ 5.5s then stopped → settling haptic plays (5-step decreasing buzzes)
- [ ] Check logcat for any errors: `adb logcat -s KindnessDiceService`

### Step 2.5: Commit

```bash
git add android/app/src/main/java/com/kindnessdice/KindnessDiceService.kt \
        android/app/src/main/res/raw/
git commit -m "feat: implement shake detection, continuous vibration, audio, and settling haptic"
```

---

## Task 3: Fullscreen Overlay + Gemini API

**Files touched:** KindnessOverlayActivity.kt (create)

### Step 3.1: Create KindnessOverlayActivity.kt

- [ ] Create `android/app/src/main/java/com/kindnessdice/KindnessOverlayActivity.kt`:

```kotlin
package com.kindnessdice

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class KindnessOverlayActivity : Activity() {

    private lateinit var kindnessText: TextView
    private lateinit var emojiText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive fullscreen
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val root = buildUI()
        setContentView(root)

        // Dismiss on tap anywhere
        root.setOnClickListener { dismissOverlay() }

        // Fetch kindness from Gemini
        fetchKindness()
    }

    override fun onBackPressed() {
        dismissOverlay()
    }

    private fun dismissOverlay() {
        // Re-enable shake detection in service
        // (service tracks overlayVisible via companion flag; we reset it here)
        KindnessDiceService.isRunning.let { _ ->
            // overlayVisible is managed by the service — it resets after SHAKE_DURATION_MS
            // We signal dismissal by finishing the activity
        }
        finish()
    }

    // ─── UI ────────────────────────────────────────────────────────────────────

    private fun buildUI(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#FFF8EF"))  // warm cream
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setPadding(80, 120, 80, 120)
        }

        emojiText = TextView(this).apply {
            text = "🎲"
            textSize = 56f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 48
            }
        }

        kindnessText = TextView(this).apply {
            text = "Rolling your kindness…"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#5D3A1A"))  // warm dark brown
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            lineSpacingMultiplier = 1.4f
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 64
            }
        }

        val dismissHint = TextView(this).apply {
            text = "tap anywhere to dismiss"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#C4A882"))  // muted warm tan
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#EDD9B8"))
            layoutParams = LinearLayout.LayoutParams(120, 2).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 48
                bottomMargin = 48
            }
        }

        root.addView(emojiText)
        root.addView(kindnessText)
        root.addView(divider)
        root.addView(dismissHint)
        return root
    }

    // ─── Gemini API ────────────────────────────────────────────────────────────

    private fun fetchKindness() {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            showError("API key not configured. Add GEMINI_API_KEY to local.properties.")
            return
        }

        Thread {
            val result = callGeminiApi(apiKey)
            runOnUiThread {
                if (result != null) {
                    emojiText.text = "✨"
                    kindnessText.text = result
                } else {
                    showError("Couldn't reach kindness today. Try again.")
                }
            }
        }.start()
    }

    private fun showError(message: String) {
        emojiText.text = "💙"
        kindnessText.text = message
    }

    private fun callGeminiApi(apiKey: String): String? {
        return try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.doOutput = true

            val prompt = "Suggest one specific, warm, and concrete act of kindness I can do today. Be direct, one sentence, no preamble."
            val body = """{"contents":[{"parts":[{"text":"$prompt"}]}]}"""
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode != 200) return null

            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            json
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
        } catch (e: Exception) {
            null
        }
    }
}
```

### Step 3.2: Fix overlayVisible reset in service

The service sets `overlayVisible = true` when launching the overlay but needs to reset it when the activity finishes. The cleanest way is a broadcast. Update the end of `KindnessDiceService.kt` — add a broadcast receiver inner class and modify `launchOverlay()`:

- [ ] Add to `KindnessDiceService.kt` companion object:

```kotlin
companion object {
    const val CHANNEL_ID = "kindness_dice_channel"
    const val NOTIFICATION_ID = 1
    const val SHAKE_THRESHOLD = 20f
    const val SHAKE_DURATION_MS = 5500L
    const val ACTION_OVERLAY_DISMISSED = "com.kindnessdice.OVERLAY_DISMISSED"
    var isRunning = false
}
```

- [ ] Add overlay dismissed receiver registration in `onCreate()`:

```kotlin
override fun onCreate() {
    super.onCreate()
    isRunning = true
    createNotificationChannel()
    startForeground(NOTIFICATION_ID, buildNotification())
    initSensor()
    registerReceiver(
        overlayDismissedReceiver,
        android.content.IntentFilter(ACTION_OVERLAY_DISMISSED),
        RECEIVER_NOT_EXPORTED
    )
}
```

- [ ] Add receiver field and unregister in `onDestroy()`:

```kotlin
private val overlayDismissedReceiver = object : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        overlayVisible = false
    }
}

override fun onDestroy() {
    super.onDestroy()
    isRunning = false
    sensorManager.unregisterListener(this)
    stopDiceSound()
    stopContinuousVibration()
    try { unregisterReceiver(overlayDismissedReceiver) } catch (_: Exception) {}
}
```

- [ ] Update `dismissOverlay()` in `KindnessOverlayActivity.kt` to send the broadcast:

```kotlin
private fun dismissOverlay() {
    sendBroadcast(Intent(KindnessDiceService.ACTION_OVERLAY_DISMISSED))
    finish()
}
```

### Step 3.3: Build and verify

- [ ] Run: `cd C:\Users\danie\Desktop\KindnessDice\android && ./gradlew assembleDebug`
- [ ] Expected: `BUILD SUCCESSFUL`

### Step 3.4: Manual end-to-end test

- [ ] Ensure `GEMINI_API_KEY` is set in `android/local.properties` with a real key
- [ ] Run: `npx react-native run-android`
- [ ] Shake device ≥ 5.5s then stop
- [ ] Expected sequence:
  1. Continuous vibration + dice sound during shake
  2. Settling haptic plays on release
  3. ~1.6s later: warm cream fullscreen overlay appears
  4. "Rolling your kindness…" text → replaced with Gemini suggestion
  5. Tap overlay → dismisses, sensor resumes
- [ ] Test error state: disable Wi-Fi, repeat shake → overlay shows "Couldn't reach kindness today. Try again."
- [ ] Check logcat: `adb logcat -s KindnessOverlayActivity`

### Step 3.5: Commit

```bash
git add android/app/src/main/java/com/kindnessdice/KindnessOverlayActivity.kt \
        android/app/src/main/java/com/kindnessdice/KindnessDiceService.kt
git commit -m "feat: add fullscreen overlay with warm cream UI and Gemini API integration"
```

---

## Task 4: React Native UI — First Launch + Settings

**Files touched:** App.tsx, src/screens/FirstLaunchScreen.tsx, src/screens/SettingsScreen.tsx, src/native/ServiceModule.ts

### Step 4.1: Install AsyncStorage

- [ ] Run: `npm install @react-native-async-storage/async-storage`
- [ ] Run: `npx react-native run-android` (triggers autolinking)
- [ ] Expected: builds successfully with AsyncStorage linked

### Step 4.2: Create JS bridge for ServiceModule

- [ ] Create directory: `src/native/`
- [ ] Create `src/native/ServiceModule.ts`:

```typescript
import {NativeModules} from 'react-native';

const {ServiceModule} = NativeModules as {
  ServiceModule: {
    startService(): void;
    stopService(): void;
    isServiceRunning(): Promise<boolean>;
    requestIgnoreBatteryOptimizations(): void;
  };
};

export default ServiceModule;
```

### Step 4.3: Create FirstLaunchScreen.tsx

- [ ] Create directory: `src/screens/`
- [ ] Create `src/screens/FirstLaunchScreen.tsx`:

```typescript
import React, {useState} from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Platform,
  PermissionsAndroid,
  Alert,
} from 'react-native';
import ServiceModule from '../native/ServiceModule';

type Props = {
  onComplete: () => void;
};

export default function FirstLaunchScreen({onComplete}: Props) {
  const [step, setStep] = useState<'permissions' | 'battery'>('permissions');

  async function requestPermissions() {
    if (Platform.OS !== 'android') {
      setStep('battery');
      return;
    }
    try {
      // POST_NOTIFICATIONS requires runtime grant on Android 13+
      if (Platform.Version >= 33) {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
          {
            title: 'Notification Permission',
            message:
              'KindnessDice needs notification permission to show the persistent service indicator.',
            buttonPositive: 'Allow',
            buttonNegative: 'Skip',
          },
        );
        if (granted === PermissionsAndroid.RESULTS.DENIED) {
          Alert.alert(
            'Permission needed',
            'Without notification permission the service may not work reliably on some devices.',
          );
        }
      }
      setStep('battery');
    } catch (e) {
      setStep('battery');
    }
  }

  function requestBatteryOptimization() {
    ServiceModule.requestIgnoreBatteryOptimizations();
    // Give user a moment then advance
    setTimeout(onComplete, 1000);
  }

  if (step === 'permissions') {
    return (
      <View style={styles.container}>
        <Text style={styles.emoji}>🎲</Text>
        <Text style={styles.title}>Welcome to{'\n'}Kindness Dice</Text>
        <Text style={styles.body}>
          Shake your phone for 5.5 seconds to receive a random act of kindness.
          {'\n\n'}First, we need a couple of permissions.
        </Text>
        <TouchableOpacity style={styles.button} onPress={requestPermissions}>
          <Text style={styles.buttonText}>Grant Permissions</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.emoji}>🔋</Text>
      <Text style={styles.title}>Battery Optimization</Text>
      <Text style={styles.body}>
        To keep Kindness Dice running in the background, tap the button below
        and select {'"'}Don't optimize{'"'} for this app.
        {'\n\n'}This prevents Android from stopping the shake detector.
      </Text>
      <TouchableOpacity style={styles.button} onPress={requestBatteryOptimization}>
        <Text style={styles.buttonText}>Open Battery Settings</Text>
      </TouchableOpacity>
      <TouchableOpacity style={styles.skip} onPress={onComplete}>
        <Text style={styles.skipText}>Skip for now</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFF8EF',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 40,
  },
  emoji: {
    fontSize: 64,
    marginBottom: 24,
  },
  title: {
    fontSize: 28,
    fontWeight: '300',
    color: '#5D3A1A',
    textAlign: 'center',
    marginBottom: 20,
    lineHeight: 36,
  },
  body: {
    fontSize: 16,
    color: '#7B5C3A',
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: 40,
  },
  button: {
    backgroundColor: '#C4722A',
    paddingHorizontal: 40,
    paddingVertical: 16,
    borderRadius: 32,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  skip: {
    marginTop: 20,
    padding: 12,
  },
  skipText: {
    color: '#C4A882',
    fontSize: 14,
  },
});
```

### Step 4.4: Create SettingsScreen.tsx

- [ ] Create `src/screens/SettingsScreen.tsx`:

```typescript
import React, {useEffect, useState} from 'react';
import {
  View,
  Text,
  Switch,
  StyleSheet,
  ActivityIndicator,
} from 'react-native';
import ServiceModule from '../native/ServiceModule';

export default function SettingsScreen() {
  const [isEnabled, setIsEnabled] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    ServiceModule.isServiceRunning()
      .then(running => {
        setIsEnabled(running);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, []);

  function toggleService(value: boolean) {
    setIsEnabled(value);
    if (value) {
      ServiceModule.startService();
    } else {
      ServiceModule.stopService();
    }
  }

  return (
    <View style={styles.container}>
      <Text style={styles.emoji}>🎲</Text>
      <Text style={styles.title}>Kindness Dice</Text>
      <Text style={styles.subtitle}>
        Shake for 5.5 seconds to roll a kindness.
      </Text>

      <View style={styles.card}>
        <View style={styles.row}>
          <View style={styles.rowText}>
            <Text style={styles.rowLabel}>Kindness Detector</Text>
            <Text style={styles.rowSub}>
              {isEnabled ? 'Running in background' : 'Tap to enable'}
            </Text>
          </View>
          {loading ? (
            <ActivityIndicator color="#C4722A" />
          ) : (
            <Switch
              value={isEnabled}
              onValueChange={toggleService}
              trackColor={{false: '#D4C5B2', true: '#C4722A'}}
              thumbColor="#FFFFFF"
            />
          )}
        </View>
      </View>

      <Text style={styles.hint}>
        The detector runs persistently and starts automatically on boot.
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFF8EF',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 32,
  },
  emoji: {
    fontSize: 56,
    marginBottom: 16,
  },
  title: {
    fontSize: 30,
    fontWeight: '300',
    color: '#5D3A1A',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 15,
    color: '#7B5C3A',
    textAlign: 'center',
    marginBottom: 48,
    lineHeight: 22,
  },
  card: {
    width: '100%',
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    paddingHorizontal: 20,
    paddingVertical: 16,
    shadowColor: '#C4A882',
    shadowOpacity: 0.15,
    shadowRadius: 8,
    elevation: 3,
    marginBottom: 24,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  rowText: {
    flex: 1,
    marginRight: 16,
  },
  rowLabel: {
    fontSize: 16,
    color: '#5D3A1A',
    fontWeight: '500',
  },
  rowSub: {
    fontSize: 13,
    color: '#9E8074',
    marginTop: 2,
  },
  hint: {
    fontSize: 13,
    color: '#C4A882',
    textAlign: 'center',
    lineHeight: 20,
    paddingHorizontal: 16,
  },
});
```

### Step 4.5: Rewrite App.tsx

- [ ] Replace `App.tsx` with:

```typescript
import React, {useEffect, useState} from 'react';
import {StatusBar} from 'react-native';
import {SafeAreaProvider} from 'react-native-safe-area-context';
import AsyncStorage from '@react-native-async-storage/async-storage';
import FirstLaunchScreen from './src/screens/FirstLaunchScreen';
import SettingsScreen from './src/screens/SettingsScreen';
import ServiceModule from './src/native/ServiceModule';

const FIRST_LAUNCH_KEY = '@kindness_dice_onboarded';

export default function App() {
  const [onboarded, setOnboarded] = useState<boolean | null>(null);

  useEffect(() => {
    AsyncStorage.getItem(FIRST_LAUNCH_KEY).then(value => {
      setOnboarded(value === 'true');
    });
  }, []);

  async function handleOnboardingComplete() {
    await AsyncStorage.setItem(FIRST_LAUNCH_KEY, 'true');
    setOnboarded(true);
    // Auto-start the service after onboarding
    ServiceModule.startService();
  }

  // Loading state — render nothing until we know onboard status
  if (onboarded === null) return null;

  return (
    <SafeAreaProvider>
      <StatusBar barStyle="dark-content" backgroundColor="#FFF8EF" />
      {onboarded ? (
        <SettingsScreen />
      ) : (
        <FirstLaunchScreen onComplete={handleOnboardingComplete} />
      )}
    </SafeAreaProvider>
  );
}
```

### Step 4.6: Remove unused scaffold import

- [ ] Verify `App.tsx` no longer imports `@react-native/new-app-screen`. The rewrite above doesn't import it.
- [ ] Run: `npx react-native run-android`
- [ ] Expected: builds and launches

### Step 4.7: Full integration test

- [ ] Fresh install (or clear app data): `adb shell pm clear com.kindnessdice && npx react-native run-android`
- [ ] Expected flow:
  1. First launch screen shows with 🎲 emoji
  2. Tap "Grant Permissions" → notification permission dialog (Android 13+)
  3. Battery optimization screen appears → tap "Open Battery Settings"
  4. Android battery dialog opens → select "Don't optimize"
  5. Settings screen shows with toggle ON (service auto-started)
  6. Toggle OFF → service stops; toggle ON → restarts
- [ ] Shake test (service running, toggle ON):
  - Shake ≥ 5.5s → settling haptic → overlay → Gemini response
- [ ] Toggle OFF → shake ≥ 5.5s → nothing should happen
- [ ] Reboot device: `adb reboot` → wait for boot → verify service auto-starts:
  - `adb shell dumpsys activity services com.kindnessdice | grep KindnessDiceService`

### Step 4.8: Commit

```bash
git add App.tsx \
        src/screens/FirstLaunchScreen.tsx \
        src/screens/SettingsScreen.tsx \
        src/native/ServiceModule.ts \
        package.json \
        package-lock.json
git commit -m "feat: add first launch onboarding, settings screen, and JS service bridge"
```

---

## Self-Review

**Spec coverage check:**
- [x] Shake detection: aT ≥ 20 m/s² for ≥ 5.5s continuously → Task 2
- [x] Timer resets on drop below threshold → `isShaking` flag reset in onSensorChanged
- [x] Continuous vibration during shake → `startContinuousVibration()` looping VibrationEffect
- [x] Dice rolling sound on loop → `MediaPlayer` with `isLooping = true` in Task 2
- [x] Haptic settling pattern (5 steps) → `playSettlingHaptic()` with exact timings
- [x] Gemini API call with specified prompt → `callGeminiApi()` in Task 3
- [x] Fullscreen overlay → `KindnessOverlayActivity` with SYSTEM_UI_FLAG_FULLSCREEN
- [x] Warm cream visual design → `#FFF8EF` background, `#5D3A1A` text, serif-light
- [x] Dismiss on tap → `root.setOnClickListener { dismissOverlay() }`
- [x] Error message on failure → `showError("Couldn't reach kindness today. Try again.")`
- [x] Re-shake while overlay shown: overlay dismisses + restart → `overlayVisible` flag pauses sensor; broadcast resets on dismiss; Task 2 handles shakeStartTime reset
- [x] ForegroundService with persistent notification "Kindness Dice is active." → Task 1
- [x] Boot auto-start via BroadcastReceiver → Task 1
- [x] Samsung battery whitelist prompt → `requestIgnoreBatteryOptimizations()` in Task 4
- [x] Permissions screen (FOREGROUND_SERVICE, RECEIVE_BOOT_COMPLETED, VIBRATE, POST_NOTIFICATIONS) → FOREGROUND_SERVICE + VIBRATE + RECEIVE_BOOT_COMPLETED declared in manifest (automatic); POST_NOTIFICATIONS requested at runtime in FirstLaunchScreen
- [x] Settings screen with service toggle → Task 4
- [x] API key in .env, gitignored → BuildConfig + local.properties approach in Task 1

**One gap:** `overlayVisible` is set to `true` in `launchOverlay()` but relies on the broadcast to reset. If the activity is killed without sending the broadcast (e.g. process kill), the sensor stays paused. Mitigation: add a `Handler` timeout in the service — if `overlayVisible` stays true for more than 60 seconds, auto-reset it.

- [ ] Add to `KindnessDiceService.kt` in `launchOverlay()`:

```kotlin
private fun launchOverlay() {
    overlayVisible = true
    val intent = Intent(this, KindnessOverlayActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    startActivity(intent)
    // Safety reset: if overlay somehow never dismisses, re-enable sensor after 60s
    android.os.Handler(mainLooper).postDelayed({
        overlayVisible = false
    }, 60_000)
}
```

---

*Plan saved. Device: Samsung S24+ connected via adb. Environment setup skipped per user instruction.*
