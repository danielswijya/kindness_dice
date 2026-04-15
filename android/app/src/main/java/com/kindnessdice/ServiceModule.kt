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
