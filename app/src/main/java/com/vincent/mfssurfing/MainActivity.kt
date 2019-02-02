package com.vincent.mfssurfing

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import org.apache.commons.lang3.StringUtils

class MainActivity : AppCompatActivity() {
    // https://www.jianshu.com/p/aa499cc64f72

    @BindView(R.id.txtStatus) lateinit var txtStatus: TextView
    @BindView(R.id.webView) lateinit var webView: WebView
    @BindView(R.id.navBar) lateinit var navBar: BottomNavigationView

    private var isSurfRunning: Boolean = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        with(webView) {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            loadUrl(getString(R.string.url_mfs_home))
        }

        setupNavigationBar()
    }

    private fun setupNavigationBar() {
        navBar.setOnNavigationItemSelectedListener { item: MenuItem ->
            when(item.itemId) {
                R.id.navHome -> goMFSHome()
                R.id.navStartStop -> switchSurfingOperation(item)
                R.id.navSettings -> Toast.makeText(applicationContext, item.title, Toast.LENGTH_SHORT).show()
            }
            true
        }

        navBar.setOnNavigationItemReselectedListener { item: MenuItem ->
            when(item.itemId) {
                R.id.navHome -> goMFSHome()
                R.id.navStartStop -> switchSurfingOperation(item)
            }
        }
    }

    private fun goMFSHome() {
        if (!StringUtils.equals(webView.originalUrl, getString(R.string.url_mfs_home))) {
            webView.webViewClient = WebViewClient()
            webView.loadUrl(getString(R.string.url_mfs_home))
        }
    }

    private fun switchSurfingOperation(item: MenuItem) {
        if (isSurfRunning) {
            item.title = getString(R.string.start_surfing)
        } else {
            item.title = getString(R.string.stop_surfing)
        }
        isSurfRunning = !isSurfRunning
    }
}
