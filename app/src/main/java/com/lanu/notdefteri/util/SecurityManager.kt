package com.lanu.notdefteri.util

import android.content.Context
import android.content.SharedPreferences

object SecurityManager {
    private const val PREFS_NAME = "lanu_security_prefs"
    private const val KEY_MASTER_PIN = "master_pin"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isPinSet(context: Context): Boolean {
        val pin = getPrefs(context).getString(KEY_MASTER_PIN, null)
        return !pin.isNullOrBlank() && pin.length == 4
    }

    fun verifyPin(context: Context, enteredPin: String): Boolean {
        val savedPin = getPrefs(context).getString(KEY_MASTER_PIN, null)
        return savedPin != null && savedPin == enteredPin
    }

    fun setMasterPin(context: Context, newPin: String): Boolean {
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) return false
        getPrefs(context).edit().putString(KEY_MASTER_PIN, newPin).apply()
        return true
    }

    fun clearPin(context: Context) {
        getPrefs(context).edit().remove(KEY_MASTER_PIN).apply()
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }
}
