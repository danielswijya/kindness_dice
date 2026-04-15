package com.kindnessdice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class KindnessDiceService : Service(), SensorEventListener {

    companion object {
        const val CHANNEL_ID = "kindness_dice_channel"
        const val NOTIFICATION_ID = 1001
        const val SHAKE_THRESHOLD = 20f
        const val SHAKE_DURATION_MS = 5500L
        const val ACTION_OVERLAY_DISMISSED = "com.kindnessdice.OVERLAY_DISMISSED"
        var isRunning = false
    }

    private lateinit var sensorManager: SensorManager
    private var linearAccelerometer: Sensor? = null
    private var mediaPlayer: MediaPlayer? = null

    private var shakeStartTime: Long = 0
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
        registerReceiver(
            overlayDismissedReceiver,
            IntentFilter(ACTION_OVERLAY_DISMISSED),
            RECEIVER_NOT_EXPORTED
        )
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

        if (aT >= SHAKE_THRESHOLD) {
            if (!isShaking) {
                isShaking = true
                shakeStartTime = System.currentTimeMillis()
                triggerArmed = false
                startContinuousVibration()
                startDiceSound()
            }
            val elapsed = System.currentTimeMillis() - shakeStartTime
            if (!triggerArmed && elapsed >= SHAKE_DURATION_MS) {
                triggerArmed = true
            }
        } else {
            if (isShaking) {
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
        overlayVisible = true
        val intent = Intent(this, KindnessOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        Handler(mainLooper).postDelayed({ overlayVisible = false }, 60_000)
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
