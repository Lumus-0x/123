package com.example.tsd_auto

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.util.Log

class CustomKeyboardIME : InputMethodService() {

    companion object {
        private const val TAG = "CustomKeyboardIME"
        var instance: CustomKeyboardIME? = null

        // 外部调用的方法 - 发送Enter键
        fun sendEnter() {
            instance?.let { ime ->
                ime.sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                Log.d(TAG, "✅ Enter отправлен через InputMethodService")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "✅ IME服务创建")
    }

    override fun onCreateInputView(): View {
        // 返回一个几乎不可见的视图
        val view = View(this)
        view.setBackgroundColor(0x01000000) // 几乎全透明
        view.isFocusable = false

        // 设置尺寸为1x1像素（最小尺寸）
        view.layoutParams = android.widget.LinearLayout.LayoutParams(1, 1)

        Log.d(TAG, "✅ IME输入视图创建（不可见）")
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.d(TAG, "IME开始输入")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "IME服务销毁")
    }
}