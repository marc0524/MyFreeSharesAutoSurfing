package com.vincent.mfssurfing

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.view.LayoutInflater
import android.webkit.WebView
import android.widget.TextView

class MyIntentService : IntentService("MyIntentService") {

    lateinit var txtStatus: TextView
    lateinit var webView: WebView

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

    fun browseUrl(url: String) {
        webView.loadUrl(url)
    }

    override fun onHandleIntent(intent: Intent?) {

    }

    internal inner class MyBinder : Binder() {
        fun getService() : MyIntentService {
            return MyIntentService()
        }

        fun say(): String {
            return "Hello"
        }
    }
}