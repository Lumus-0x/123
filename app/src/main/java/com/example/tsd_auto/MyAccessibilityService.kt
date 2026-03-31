package com.example.tsd_auto

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import java.util.regex.Pattern

class MyAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingInsertRunnable: Runnable? = null
    private var lastExtractedNumber: String? = null
    private var lastProcessedText: String? = null
    private var isProcessing = false

    companion object {
        private const val TAG = "MyAccessibilityService"
        private const val DELAY_MS = 1500L
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "tsd_auto_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "✅ Служба доступности запущена")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        setServiceInfo(info)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTextChanged(event)
            }
        }
    }

    private fun handleTextChanged(event: AccessibilityEvent) {
        val source = event.source ?: return
        val text = event.text?.firstOrNull()?.toString() ?: return

        if (text.startsWith("CEN;")) {
            Log.d(TAG, "📱 Обнаружен штрихкод: $text")

            if (text != lastProcessedText) {
                isProcessing = false
            }

            val pattern = Pattern.compile("^CEN;(\\d+);")
            val matcher = pattern.matcher(text)

            if (matcher.find()) {
                val number = matcher.group(1)
                if (number != null && !isProcessing) {
                    isProcessing = true
                    lastProcessedText = text
                    lastExtractedNumber = number

                    pendingInsertRunnable?.let { handler.removeCallbacks(it) }
                    pendingInsertRunnable = Runnable {
                        insertNumberAndSendEnter()
                    }
                    handler.postDelayed(pendingInsertRunnable!!, DELAY_MS)
                    Log.d(TAG, "⏰ Через ${DELAY_MS}мс вставим: $number")
                }
            }
        }
        source.recycle()
    }

    private fun insertNumberAndSendEnter() {
        val number = lastExtractedNumber ?: run {
            resetState()
            return
        }
        Log.d(TAG, "📝 Вставляем номер: $number")

        try {
            val root = rootInActiveWindow
            if (root == null) {
                Log.w(TAG, "❌ Не удалось получить root окно")
                resetState()
                return
            }

            val focusNode = findFocusNode(root)
            if (focusNode == null) {
                Log.w(TAG, "❌ Не найден элемент в фокусе")
                root.recycle()
                resetState()
                return
            }

            if (focusNode.isEditable) {
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, number)
                }
                val success = focusNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                if (success) {
                    Log.d(TAG, "✅ Номер $number успешно вставлен")

                    // 等待一小段时间后发送Enter
                    handler.postDelayed({
                        sendEnterViaIME()
                    }, 200)
                } else {
                    Log.w(TAG, "❌ Не удалось вставить номер")
                }
            } else {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("scanned", number))
                Log.d(TAG, "✅ Номер скопирован в буфер обмена")
            }

            focusNode.recycle()
            root.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка", e)
        } finally {
            resetState()
        }
    }

    private fun sendEnterViaIME() {
        Log.d(TAG, "⌨️ Запрашиваем отправку Enter через IME...")

        // 调用自定义键盘发送Enter
        CustomKeyboardIME.sendEnter()
    }

    private fun findFocusNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused) {
            return AccessibilityNodeInfo.obtain(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFocusNode(child)
            if (result != null) {
                child.recycle()
                return result
            }
            child.recycle()
        }
        return null
    }

    private fun resetState() {
        isProcessing = false
        lastExtractedNumber = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.notification_title))
        .setContentText(getString(R.string.notification_text))
        .setSmallIcon(android.R.drawable.ic_menu_camera)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    override fun onInterrupt() {
        Log.d(TAG, "⚠️ Служба прервана")
        resetState()
        pendingInsertRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}