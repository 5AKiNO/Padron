package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

object DeviceIdHelper {
    /**
     * Retrieves the unique hardware identifier for the Android device.
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            if (!androidId.isNullOrBlank()) {
                androidId
            } else {
                getFallbackDeviceId(context)
            }
        } catch (e: Exception) {
            getFallbackDeviceId(context)
        }
    }

    private fun getFallbackDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("device_identity_prefs", Context.MODE_PRIVATE)
        var fallback = prefs.getString("fallback_device_id", null)
        if (fallback.isNullOrBlank()) {
            fallback = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("fallback_device_id", fallback).apply()
        }
        return fallback
    }
}
