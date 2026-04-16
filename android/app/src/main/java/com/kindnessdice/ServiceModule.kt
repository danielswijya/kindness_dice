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
        try {
            val intent = Intent(reactContext, KindnessDiceService::class.java)
            reactContext.startForegroundService(intent)
        } catch (e: Exception) {
            // Ignore — service may already be running or context unavailable
        }
    }

    @ReactMethod
    fun stopService() {
        try {
            val intent = Intent(reactContext, KindnessDiceService::class.java)
            reactContext.stopService(intent)
        } catch (e: Exception) {
            // Ignore — service may already be stopped
        }
    }

    @ReactMethod
    fun isServiceRunning(promise: Promise) {
        promise.resolve(KindnessDiceService.isRunning)
    }

    @ReactMethod
    fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(reactContext)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${reactContext.packageName}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            reactContext.startActivity(intent)
        }
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
