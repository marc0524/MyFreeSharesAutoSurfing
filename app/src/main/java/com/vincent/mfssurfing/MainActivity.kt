package com.vincent.mfssurfing

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.design.widget.BottomNavigationView
import android.view.MenuItem
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.select.Elements

class MainActivity : AppCompatActivity() {
    // https://www.jianshu.com/p/aa499cc64f72

    @BindView(R.id.txtStatus) lateinit var txtStatus: TextView
    @BindView(R.id.webView) lateinit var webView: WebView
    @BindView(R.id.navBar) lateinit var navBar: BottomNavigationView

    private var isSurfRunning: Boolean = false

    private lateinit var thread: Thread

    private val THREAD_KEY_OPERATION_WRAPPER = "operationWrapper"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        with(webView) {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            addJavascriptInterface(JavaScriptInterface(), "HTMLOUT")
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
            webView.webViewClient = WebViewClient()
            Toast.makeText(applicationContext, getString(R.string.surfing_stopped), Toast.LENGTH_SHORT).show()
        } else {
            if (SurfingSpace.fromUrlStartWith(webView.originalUrl) == null) {
                Toast.makeText(applicationContext, getString(R.string.go_to_surfing_space), Toast.LENGTH_SHORT).show()
                return
            }
            item.title = getString(R.string.stop_surfing)
            webView.webViewClient = getSpecialWebViewClient()
            webView.reload()
        }
        isSurfRunning = !isSurfRunning
    }

    private fun getSpecialWebViewClient(): WebViewClient {
        return object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.loadUrl("javascript:HTMLOUT.processHTML(document.documentElement.outerHTML);")
            }

        }
    }

    private fun processAdList(html: String) {
        val adLinkTags: Elements = Jsoup.parse(html)
            .select("a[href^=http://myfreeshares.com/scripts/runner.php?PA=]")

        for (tag in adLinkTags) {
            if (!StringUtils.containsIgnoreCase(tag.text(), "Donate")) {
                Toast.makeText(applicationContext,
                    StringUtils.substringAfter(tag.attr("href"), "PA="),
                    Toast.LENGTH_SHORT).show()
            }
        }

        Toast.makeText(applicationContext, "parse end", Toast.LENGTH_SHORT).show()
    }

    private fun processNoAd() {
        val operation = object : OperationWrapper {
            override fun execute() {
                Toast.makeText(applicationContext, getString(R.string.no_ads_available), Toast.LENGTH_SHORT).show()
                navBar.menu.findItem(R.id.navStartStop).title = getString(R.string.start_surfing)
                isSurfRunning = !isSurfRunning
                webView.webViewClient = WebViewClient()
            }
        }

        thread = object : Thread() {
            override fun run() {
                super.run()
                handler.sendMessage(getMessage(operation))
            }
        }
        thread.start()
    }

    val handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            val oper = msg?.data?.get(THREAD_KEY_OPERATION_WRAPPER) as OperationWrapper
            oper.execute()
        }
    }

    private fun getMessage(wrapper: OperationWrapper): Message {
        val bundle = Bundle()
        bundle.putSerializable(THREAD_KEY_OPERATION_WRAPPER, wrapper)

        val msg = Message()
        msg.data = bundle

        return msg
    }

    internal inner class JavaScriptInterface {
        @JavascriptInterface
        fun processHTML(html: String) {
            if (StringUtils.containsIgnoreCase(html, getString(R.string.ad_worth_text))) {
                processAdList(html)
                return
            } else if (StringUtils.containsIgnoreCase(html, getString(R.string.no_ads_available))) {
                processNoAd()
                return
            }

        }
    }
}
