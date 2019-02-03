package com.vincent.mfssurfing

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.design.widget.BottomNavigationView
import android.view.MenuItem
import android.webkit.*
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.util.*

class MainActivity : AppCompatActivity() {
    // https://www.jianshu.com/p/aa499cc64f72

    @BindView(R.id.txtStatus) lateinit var txtStatus: TextView
    @BindView(R.id.webView) lateinit var webView: WebView
    @BindView(R.id.navBar) lateinit var navBar: BottomNavigationView

    private val adPageStack: Stack<AdPage> = Stack()
    private var turingNumberUrlStack: Stack<String> = Stack()

    private lateinit var selectedAdPage: AdPage
    private lateinit var selectedAdHomeUrl: String

    private var isSurfRunning: Boolean = false
    private var hasAdSinceLastCheck: Boolean = false

    private lateinit var thread: Thread
    private lateinit var timerThread: Thread

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        with(webView) {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            addJavascriptInterface(JavaScriptInterface(), Constants.JAVASCRIPT_NAME)
            loadUrl(Constants.URL_MFS_HOME)
        }

        setupNavigationBar()
    }

    private fun setupNavigationBar() {
        navBar.setOnNavigationItemSelectedListener { item: MenuItem ->
            when(item.itemId) {
                R.id.navHome -> goMFSHome()
                R.id.navStartStop -> switchSurfingOperation(item)
                R.id.navSettings -> {
                    updateStatus(getString(R.string.stand_by))
                    Toast.makeText(applicationContext, item.title, Toast.LENGTH_SHORT).show()
                }
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
        if (StringUtils.equals(webView.originalUrl, Constants.URL_MFS_HOME)) {
            Toast.makeText(applicationContext, getString(R.string.landing_home_already), Toast.LENGTH_SHORT).show()
        } else {
            updateStatus(getString(R.string.stand_by))
            webView.webViewClient = WebViewClient()
            webView.loadUrl(Constants.URL_MFS_HOME)
        }
    }

    private fun switchSurfingOperation(item: MenuItem) {
        if (isSurfRunning) {
            stopTimer()

            updateStatus(getString(R.string.surfing_stopped))
            with(item) {
                title = getString(R.string.start_surfing)
                setIcon(R.drawable.icon_start)
            }

            webView.webViewClient = WebViewClient()
        } else {
            if (SurfingSpace.fromUrlStartWith(webView.originalUrl) == null) {
                Toast.makeText(applicationContext, getString(R.string.go_to_surfing_space), Toast.LENGTH_SHORT).show()
                return
            }

            updateStatus(getString(R.string.loading_ad_list))
            with(item) {
                title = getString(R.string.stop_surfing)
                setIcon(R.drawable.icon_stop)
            }

            selectedAdHomeUrl = webView.originalUrl
            webView.webViewClient = getSpecialWebViewClient()
            webView.reload()
        }

        isSurfRunning = !isSurfRunning
    }

    private fun processAdList(html: String) {
        val adLinkTags: Elements = Jsoup.parse(html)
            .select(Constants.TAG_REGEX_AD)

        val adPageList = mutableListOf<AdPage>()

        for (tag in adLinkTags) {
            if (isNotDangerousAd(tag.text())) {
                val adPage = AdPage()
                with(adPage) {
                    name = StringUtils.substringAfter(tag.attr(Constants.ATTR_HREF), Constants.PARAM_AD_NUMBER)
                    url = tag.attr(Constants.ATTR_HREF)
                    duration = StringUtils.substringBetween(tag.attr(Constants.ATTR_ON_CLICK), "(", ")").toInt()
                }

                adPageList.add(adPage)
            }
        }

        if (adPageList.isNotEmpty()) {
            hasAdSinceLastCheck = true
            adPageList.shuffle()
            adPageStack.clear()
            adPageStack.addAll(adPageList)
            browseNextAd()
        } else {
            processNoAdAvailable()
        }
    }

    private fun processNoAdAvailable() {
        val operation = object : OperationWrapper {
            override fun execute() {
                txtStatus.text = getString(R.string.ad_clear)
                Toast.makeText(applicationContext, getString(R.string.no_ad_can_browse), Toast.LENGTH_SHORT).show()

                with(navBar.menu.findItem(R.id.navStartStop)) {
                    title = getString(R.string.start_surfing)
                    setIcon(R.drawable.icon_start)
                }

                isSurfRunning = false
                webView.webViewClient = WebViewClient()
            }
        }

        startThread(operation, 0)
    }

    private fun processTuringTest(html: String) {
        if (turingNumberUrlStack.empty()) {
            val numberLinkTags: Elements = Jsoup.parse(html)
                .select(Constants.TAG_REGEX_TURING_NUMBER)
            numberLinkTags.shuffle()

            for (tag in numberLinkTags) {
                turingNumberUrlStack.push("${Constants.URL_PREFIX_TURING_NUMBER}${tag.attr(Constants.ATTR_HREF)}")
            }
        }

        val url = turingNumberUrlStack.pop()
        val turingNumber = StringUtils.substring(
            StringUtils.substringAfter(url, Constants.PARAM_TURING_NUMBER), 0, 4)

        updateStatus("${getString(R.string.try_turing_number)}$turingNumber")
        browseUrl(url)
    }

    private fun processAdPage() {
        stopTimer()
        turingNumberUrlStack.clear()

        with(selectedAdPage) {
            updateStatus("${getString(R.string.browsing_ad_page)}$name${getString(R.string.time)}$duration${getString(R.string.second)}")

            val operation = object : OperationWrapper {
                override fun execute() {
                    if (isSurfRunning) {
                        browseNextAd()
                    }
                }
            }

            val extraDelayTime = (Math.random() * 2000 + 3000).toLong()
            startThread(operation, duration * 1000 + extraDelayTime)
        }
    }

    private fun processAccountCredited() {
        stopTimer()
        browseNextAd()
    }

    private fun browseNextAd() {
        if (adPageStack.empty()) {
            if (hasAdSinceLastCheck) {
                updateStatus(getString(R.string.checking_left_ads))
                hasAdSinceLastCheck = false
                browseUrl(selectedAdHomeUrl)
            } else {
                processNoAdAvailable()
            }
        } else {
            selectedAdPage = adPageStack.pop()
            browseUrl(selectedAdPage.url)
        }
    }

    private fun browseUrl(url: String) {
        val operation = object : OperationWrapper {
            override fun execute() {
                webView.loadUrl(url)
            }
        }

        startThread(operation, 0)
    }

    private fun updateStatus(message: String) {
        val operation = object : OperationWrapper {
            override fun execute() {
                txtStatus.text = message
            }
        }

        startThread(operation, 0)
    }

    private fun launchTimer() {
        val operation = object : OperationWrapper {
            override fun execute() {
                browseNextAd()
            }
        }

        timerThread = object : Thread() {
            override fun run() {
                super.run()
                Thread.sleep(60000)
                handler.sendMessage(getOperationMessage(operation))
            }
        }
    }

    private fun stopTimer() {
        if (!timerThread.isInterrupted) {
            timerThread.interrupt()
        }
    }

    private fun isNotDangerousAd(adTitle: String): Boolean {
        for (keyword in Constants.DANGEROUS_AD_TITLE_KEYWORDS) {
            if (StringUtils.containsIgnoreCase(adTitle, keyword)) {
                return false
            }
        }

        return true
    }

    private fun startThread(operation: OperationWrapper, delayTimeMill: Long) {
        thread = object : Thread() {
            override fun run() {
                super.run()
                handler.sendMessageDelayed(getOperationMessage(operation), delayTimeMill)
            }
        }
        thread.start()
    }

    private fun getOperationMessage(operation: OperationWrapper): Message {
        val bundle = Bundle()
        bundle.putSerializable(Constants.MESSAGE_KEY_OPERATION_WRAPPER, operation)

        val message = Message()
        message.data = bundle

        return message
    }

    val handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)

            val operation = msg?.data?.get(Constants.MESSAGE_KEY_OPERATION_WRAPPER) as OperationWrapper
            operation.execute()
        }
    }

    private fun getSpecialWebViewClient(): WebViewClient {
        return object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (StringUtils.startsWith(url, Constants.URL_PREFIX_AD_PAGE)) {
                    val adNumber = StringUtils.substringAfter(selectedAdPage.url, Constants.PARAM_AD_NUMBER)
                    updateStatus("${getString(R.string.loading_ad_page)}$adNumber")
                    launchTimer()
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.loadUrl(Constants.JAVASCRIPT_SYNTAX)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                //updateStatus(getString(R.string.connection_failed))
            }

        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }

        super.onBackPressed()
    }

    internal inner class JavaScriptInterface {
        @JavascriptInterface
        fun processHTML(html: String) {
            if (StringUtils.containsIgnoreCase(html, Constants.PAGE_TEXT_AD_WORTH)) {
                processAdList(html)
            } else if (StringUtils.containsIgnoreCase(html, Constants.PAGE_TEXT_NO_ADS_AVAILABLE)) {
                processNoAdAvailable()
            } else if (StringUtils.containsIgnoreCase(html,Constants.PAGE_TEXT_TURING_TEST)) {
                processTuringTest(html)
            } else if (StringUtils.containsIgnoreCase(html, Constants.PAGE_TEXT_ACCOUNT_CREDITED)){
                processAccountCredited()
            } else {
                processAdPage()
            }
        }
    }

}
