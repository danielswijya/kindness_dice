package com.kindnessdice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.sqrt

class KindnessDiceService : Service(), SensorEventListener {

    companion object {
        const val CHANNEL_ID = "kindness_dice_channel"
        const val NOTIFICATION_ID = 1001
        const val SHAKE_THRESHOLD = 12f
        const val SHAKE_DURATION_MS = 5500L
        const val SHAKE_GRACE_MS = 500L  // brief drops don't reset timer
        const val ACTION_OVERLAY_DISMISSED = "com.kindnessdice.OVERLAY_DISMISSED"
        var isRunning = false
    }

    private lateinit var sensorManager: SensorManager
    private var linearAccelerometer: Sensor? = null
    private var mediaPlayer: MediaPlayer? = null

    private var shakeStartTime: Long = 0
    private var lastAboveThreshold: Long = 0
    private var isShaking = false
    private var triggerArmed = false
    var overlayVisible = false

    private val overlayDismissedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            overlayVisible = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        initSensor()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(overlayDismissedReceiver, IntentFilter(ACTION_OVERLAY_DISMISSED), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(overlayDismissedReceiver, IntentFilter(ACTION_OVERLAY_DISMISSED))
        }
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
        try { unregisterReceiver(overlayDismissedReceiver) } catch (_: Exception) {}
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
        if (overlayVisible) return

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        val aT = sqrt(ax * ax + ay * ay + az * az)

        val now = System.currentTimeMillis()
        if (aT >= SHAKE_THRESHOLD) {
            lastAboveThreshold = now
            if (!isShaking) {
                isShaking = true
                shakeStartTime = now
                triggerArmed = false
                startContinuousVibration()
                startDiceSound()
            }
            val elapsed = now - shakeStartTime
            if (!triggerArmed && elapsed >= SHAKE_DURATION_MS) {
                triggerArmed = true
            }
        } else {
            if (isShaking && (now - lastAboveThreshold) > SHAKE_GRACE_MS) {
                isShaking = false
                stopContinuousVibration()
                stopDiceSound()
                if (triggerArmed) {
                    triggerArmed = false
                    onShakeTriggered()
                }
                shakeStartTime = 0
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ─── Trigger ───────────────────────────────────────────────────────────────

    private fun onShakeTriggered() {
        playSettlingHaptic()
        Handler(mainLooper).postDelayed({ launchOverlay() }, 1600)
    }

    private fun launchOverlay() {
        if (!Settings.canDrawOverlays(this)) return  // permission not granted yet
        overlayVisible = true

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            MATCH_PARENT, MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#FFF8EF"))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setPadding(80, 120, 80, 120)
        }

        val emojiText = TextView(this).apply {
            text = "🎲"
            textSize = 56f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = 48 }
        }

        val kindnessText = TextView(this).apply {
            text = "Rolling your kindness…"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#5D3A1A"))
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = 64 }
        }

        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#EDD9B8"))
            layoutParams = LinearLayout.LayoutParams(120, 2).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 48; bottomMargin = 48
            }
        }

        val hint = TextView(this).apply {
            text = "tap anywhere to dismiss"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#C4A882"))
        }

        root.addView(emojiText)
        root.addView(kindnessText)
        root.addView(divider)
        root.addView(hint)

        fun dismiss() {
            try { wm.removeView(root) } catch (_: Exception) {}
            overlayVisible = false
        }

        root.setOnClickListener { dismiss() }
        wm.addView(root, params)

        // Safety timeout
        Handler(mainLooper).postDelayed({ dismiss() }, 60_000)

        // Fetch kindness
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            emojiText.text = "💙"
            kindnessText.text = "Couldn't reach kindness today. Try again."
            return
        }
        Thread {
            val result = callGeminiApi(apiKey)
            Handler(mainLooper).post {
                if (result != null) {
                    emojiText.text = "✨"
                    kindnessText.text = result
                } else {
                    emojiText.text = "💙"
                    kindnessText.text = "Couldn't reach kindness today. Try again."
                }
            }
        }.start()
    }

    private fun callGeminiApi(apiKey: String): String? {
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            val body = """{"contents":[{"parts":[{"text":"Suggest one specific, warm, and concrete act of kindness I can do today. Be direct, one sentence, no preamble."}]}]}"""
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode != 200) {
                android.util.Log.e("KindnessDice", "Gemini HTTP ${conn.responseCode}: ${conn.errorStream?.bufferedReader()?.readText()}")
                return null
            }
            JSONObject(conn.inputStream.bufferedReader().readText())
                .getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts").getJSONObject(0)
                .getString("text").trim()
        } catch (e: Exception) {
            android.util.Log.e("KindnessDice", "Gemini exception: ${e::class.simpleName}: ${e.message}", e)
            null
        }
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
        val effect = VibrationEffect.createWaveform(
            longArrayOf(100, 50), intArrayOf(150, 0), 0
        )
        getVibrator().vibrate(effect)
    }

    private fun stopContinuousVibration() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        getVibrator().cancel()
    }

    private fun playSettlingHaptic() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val effect = VibrationEffect.createWaveform(
            longArrayOf(0, 200, 150, 150, 200, 100, 250, 80, 300, 50),
            intArrayOf(0, 255, 0, 220, 0, 180, 0, 140, 0, 100),
            -1
        )
        getVibrator().vibrate(effect)
    }

    // ─── Audio ─────────────────────────────────────────────────────────────────

    private fun startDiceSound() {
        try {
            val resId = resources.getIdentifier("dice_roll", "raw", packageName)
            if (resId == 0) return
            mediaPlayer = MediaPlayer.create(this, resId)?.apply {
                isLooping = true
                setVolume(0.8f, 0.8f)
                start()
            }
        } catch (_: Exception) {
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
            CHANNEL_ID, "Kindness Dice Service", NotificationManager.IMPORTANCE_LOW
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
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
