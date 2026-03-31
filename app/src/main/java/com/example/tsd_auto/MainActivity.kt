package com.example.tsd_auto

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button
    private lateinit var imeStatusText: TextView
    private lateinit var enableImeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        toggleButton = findViewById(R.id.toggleButton)
        imeStatusText = findViewById(R.id.imeStatusText)
        enableImeButton = findViewById(R.id.enableImeButton)

        updateStatus()
        updateImeStatus()

        toggleButton.setOnClickListener {
            openAccessibilitySettings()
        }

        enableImeButton.setOnClickListener {
            openImeSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        updateImeStatus()
    }

    private fun updateStatus() {
        val enabled = isAccessibilityServiceEnabled()
        statusText.text = if (enabled) "✅ Включена" else "❌ Отключена"
        toggleButton.text = if (enabled) "Выключить службу" else "Включить службу"
    }

    private fun updateImeStatus() {
        val isSelected = isImeSelected()
        imeStatusText.text = if (isSelected) "✅ Выбрана" else "❌ Не выбрана"
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = "${packageName}/.MyAccessibilityService"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(serviceName) == true
    }

    private fun isImeSelected(): Boolean {
        val currentIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return currentIme?.contains(packageName) == true
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(this, "Найдите и включите TSD Auto в списке служб", Toast.LENGTH_LONG).show()
    }

    private fun openImeSettings() {
        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        Toast.makeText(this, "Найдите и выберите TSD Auto в списке клавиатур", Toast.LENGTH_LONG).show()
    }
}