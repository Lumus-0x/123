package com.example.tsd_auto

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var accessibilityStatus: TextView
    private lateinit var keyboardStatus: TextView
    private lateinit var btnEnableAccessibility: MaterialButton
    private lateinit var btnEnableKeyboard: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        accessibilityStatus = findViewById(R.id.accessibilityStatus)
        keyboardStatus = findViewById(R.id.keyboardStatus)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        btnEnableKeyboard = findViewById(R.id.btnEnableKeyboard)

        updateStatus()
        updateImeStatus()

        btnEnableAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }

        btnEnableKeyboard.setOnClickListener {
            openImeSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        updateImeStatus()
    }

    private fun updateStatus() {
        val enabled = isAccessibilityEnabled() && isAccessibilityServiceEnabled()

        accessibilityStatus.text = if (enabled) {
            "🟢 " + getString(R.string.status_enabled)
        } else {
            "🔴 " + getString(R.string.status_disabled)
        }
    }

    private fun updateImeStatus() {
        val enabled = isImeSelected()

        keyboardStatus.text = if (enabled) {
            "🟢 " + getString(R.string.status_enabled)
        } else {
            "🔴 " + getString(R.string.status_disabled)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedService = "$packageName/${MyAccessibilityService::class.java.name}"

        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val services = enabledServices.split(":")

        return services.any { it.equals(expectedService, ignoreCase = true) }
    }

    private fun isAccessibilityEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            ) == 1
        } catch (e: Exception) {
            false
        }
    }

    private fun isImeSelected(): Boolean {
        val currentIme = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
        return currentIme?.contains(packageName) == true
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(
            this,
            getString(R.string.toast_enable_service),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun openImeSettings() {
        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        Toast.makeText(
            this,
            getString(R.string.toast_enable_keyboard),
            Toast.LENGTH_LONG
        ).show()
    }
}