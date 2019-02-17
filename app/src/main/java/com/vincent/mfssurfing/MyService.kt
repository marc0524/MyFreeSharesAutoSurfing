package com.vincent.mfssurfing

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.view.LayoutInflater
import android.webkit.WebView
import android.widget.TextView

class MyService : Service() {

    lateinit var txtStatus: TextView
    lateinit var webView: WebView
    val threadHelper: ThreadHelper = ThreadHelper()

    override fun onCreate() {
        super.onCreate()
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.content_surfing_region, null)

        txtStatus = view.findViewById(R.id.txtStatus)
        webView = view.findViewById(R.id.webView)
    }

    override fun onBind(intent: Intent?): IBinder {
        return MyBinder()
    }

    fun browseUrl(webView: WebView, url: String) {
        val operator = object : Operator {
            override fun execute() {
                webView.loadUrl(url)
            }
        }
        threadHelper.startThread(operator, 0)
    }

    fun updateText(textView: TextView, message: String) {
        val operator = object : Operator {
            override fun execute() {
                textView.text = message
            }
        }
        threadHelper.startThread(operator, 0)
    }

    internal inner class MyBinder : Binder() {
        fun getService() : MyService {
            return MyService()
        }
    }
}