package com.kindnessdice

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class KindnessOverlayActivity : Activity() {

    private lateinit var kindnessText: TextView
    private lateinit var emojiText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        val root = buildUI()
        setContentView(root)
        root.setOnClickListener { dismissOverlay() }
        fetchKindness()
    }

    override fun onBackPressed() {
        dismissOverlay()
    }

    private fun dismissOverlay() {
        sendBroadcast(android.content.Intent(KindnessDiceService.ACTION_OVERLAY_DISMISSED))
        finish()
    }

    private fun buildUI(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#FFF8EF"))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setPadding(80, 120, 80, 120)
        }
        emojiText = TextView(this).apply {
            text = "🎲"
            textSize = 56f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = 48 }
        }
        kindnessText = TextView(this).apply {
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
                topMargin = 48
                bottomMargin = 48
            }
        }
        val dismissHint = TextView(this).apply {
            text = "tap anywhere to dismiss"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#C4A882"))
        }
        root.addView(emojiText)
        root.addView(kindnessText)
        root.addView(divider)
        root.addView(dismissHint)
        return root
    }

    private fun fetchKindness() {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            showError("API key not configured.")
            return
        }
        Thread {
            val result = callGeminiApi(apiKey)
            runOnUiThread {
                if (result != null) {
                    emojiText.text = "✨"
                    kindnessText.text = result
                } else {
                    showError("Couldn't reach kindness today. Try again.")
                }
            }
        }.start()
    }

    private fun showError(message: String) {
        emojiText.text = "💙"
        kindnessText.text = message
    }

    private fun callGeminiApi(apiKey: String): String? {
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            val body = """{"contents":[{"parts":[{"text":"Suggest one specific, warm, and concrete act of kindness I can do today. Be direct, one sentence, no preamble."}]}]}"""
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode != 200) return null
            val response = conn.inputStream.bufferedReader().readText()
            JSONObject(response)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
        } catch (e: Exception) {
            null
        }
    }
}
