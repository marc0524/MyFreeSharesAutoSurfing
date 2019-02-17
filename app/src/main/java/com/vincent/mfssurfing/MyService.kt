package com.vincent.mfssurfing

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.util.*

class MyService : Service() {
    lateinit var txtStatus: TextView
    lateinit var webView: WebView

    private lateinit var context: Context
    private val adPageStack: Stack<AdPage> = Stack()
    private var captchaNumberUrlStack: Stack<String> = Stack()

    private lateinit var selectedAdPage: AdPage
    private lateinit var selectedAdHomeUrl: String

    private var hasAdSinceLastCheck: Boolean = false
    private var isCredited: Boolean = false
    var isSurfRunning: Boolean = false

    private var skipSecond: Int = 60
    private var isHandlingCaptchaAllowed: Boolean = true
    private lateinit var ignoredAdNumbers: List<String>

    private lateinit var onSurfFinishedListener: OnSurfFinishedListener
    private var skipTimerThread: Thread? = null
    private var creditedTimerThread: Thread? = null

    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var threadHelper: ThreadHelper

    override fun onBind(intent: Intent?): IBinder {
        return MyBinder()
    }

    fun startSurfing(listener: OnSurfFinishedListener) {
        this.onSurfFinishedListener = listener

        preferencesHelper = PreferencesHelper(context)
        threadHelper = ThreadHelper()
        isSurfRunning = true

        skipSecond = preferencesHelper.getAdBrowsingSkipSecond()
        isHandlingCaptchaAllowed = preferencesHelper.isHandlingCaptchaAllowed()
        ignoredAdNumbers = preferencesHelper.getIgnoredAdList()

        selectedAdHomeUrl = webView.originalUrl

        webView.webViewClient = getSpecialWebViewClient()
        webView.addJavascriptInterface(JavaScriptInterface(), Constants.JAVASCRIPT_NAME)
        webView.reload()
    }

    private fun browseUrl(webView: WebView, url: String) {
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

    private fun processAdListPage(html: String) {
        val adLinkTags: Elements = Jsoup.parse(html)
            .select(Constants.TAG_REGEX_AD)

        val adPageList = mutableListOf<AdPage>()

        for (tag in adLinkTags) {
            if (isVisitableAd(tag.attr(Constants.ATTR_HREF))) {
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
            processNoAdAvailablePage()
        }
    }

    private fun processNoAdAvailablePage() {
        onSurfFinishedListener.finished()
//        val operator = object : Operator {
//            override fun execute() {
//                txtStatus.text = context.getString(R.string.ad_clear)
//                Toast.makeText(context, context.getString(R.string.no_ad_can_browse), Toast.LENGTH_SHORT).show()
//
//                with(navBar.menu.findItem(R.id.navStartStop)) {
//                    title = context.getString(R.string.start_surfing)
//                    setIcon(R.drawable.icon_start)
//                }
//
//                isSurfRunning = false
//                webView.webViewClient = WebViewClient()
//            }
//        }
//
//        threadHelper.startThread(operator, 0)
    }

    private fun processCaptchaPage(html: String) {
        stopSkipTimer()

        if (!isHandlingCaptchaAllowed) {
            stopSkipTimer()
            return
        }

        if (captchaNumberUrlStack.empty()) {
            val numberLinkTags: Elements = Jsoup.parse(html)
                .select(Constants.TAG_REGEX_CAPTCHA_NUMBER)
            numberLinkTags.shuffle()

            for (tag in numberLinkTags) {
                captchaNumberUrlStack.push("${Constants.URL_PREFIX_CAPTCHA_NUMBER}${tag.attr(Constants.ATTR_HREF)}")
            }
        }

        val url = captchaNumberUrlStack.pop()
        val captchaNumber = StringUtils.substring(
            StringUtils.substringAfter(url, Constants.PARAM_CAPTCHA_NUMBER), 0, 4)

        updateText(txtStatus, "${context.getString(R.string.loading_ad_page)}${selectedAdPage.name}${context.getString(R.string.try_captcha_number)}$captchaNumber")
        browseUrl(webView, url)
    }

    private fun processAdPage() {
        stopSkipTimer()
        captchaNumberUrlStack.clear()

        if (isCredited) {
            browseNextAd()
            return
        }

        stopCreditTimer()

        with(selectedAdPage) {
            updateText(txtStatus, "${context.getString(R.string.browsing_ad_page)}$name${context.getString(R.string.time)}$duration${context.getString(R.string.second)}")

            val operator = object : Operator {
                override fun execute() {
                    if (isSurfRunning) {
                        browseNextAd()
                    }
                }
            }

            threadHelper.startThread(operator, duration * 1000L)
        }
    }

    private fun browseNextAd() {
        if (adPageStack.empty()) {
            if (hasAdSinceLastCheck) {
                updateText(txtStatus, context.getString(R.string.checking_left_ads))
                hasAdSinceLastCheck = false
                browseUrl(webView, selectedAdHomeUrl)
            } else {
                processNoAdAvailablePage()
            }
        } else {
            selectedAdPage = adPageStack.pop()
            browseUrl(webView, selectedAdPage.url)
        }
    }

    private fun isVisitableAd(adUrl: String): Boolean {
        return !ignoredAdNumbers.contains(
            StringUtils.substringAfter(adUrl, Constants.PARAM_AD_NUMBER))
    }

    private fun launchSkipTimer() {
        val operator = object : Operator {
            override fun execute() {
                Toast.makeText(context, context.getString(R.string.going_to_skip_ad), Toast.LENGTH_SHORT).show()
                browseNextAd()
            }
        }
        skipTimerThread = threadHelper.startThread(operator, skipSecond * 1000L)
    }

    private fun launchCreditTimer(creditTimeSecond: Int) {
        val operator = object : Operator {
            override fun execute() {
                isCredited = true
            }
        }

        val extraTimeMill = (Math.random() * 500 + 1000).toLong()
        creditedTimerThread = threadHelper.startThread(operator, creditTimeSecond * 1000 + extraTimeMill)
    }

    @Throws(UninitializedPropertyAccessException::class)
    fun stopSkipTimer() {
        threadHelper.stopThread(skipTimerThread)
    }

    @Throws(UninitializedPropertyAccessException::class)
    fun stopCreditTimer() {
        threadHelper.stopThread(creditedTimerThread)
    }

    private fun getSpecialWebViewClient(): WebViewClient {
        return object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (StringUtils.startsWith(url, Constants.URL_PREFIX_AD_PAGE)) {
                    isCredited = false
                    launchSkipTimer()
                    launchCreditTimer(selectedAdPage.duration)

                    updateText(txtStatus, "${context.getString(R.string.loading_ad_page)}${selectedAdPage.name}")
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.loadUrl(Constants.JAVASCRIPT_SYNTAX)
            }

        }
    }

    internal inner class MyBinder : Binder() {
        fun getService(context: Context) : MyService {
            val service = MyService()
            service.context = context

            return service
        }
    }

    internal inner class JavaScriptInterface {
        @JavascriptInterface
        fun processHTML(html: String) {
            if (StringUtils.containsIgnoreCase(html, Constants.PAGE_TEXT_AD_WORTH)) {
                processAdListPage(html)
            } else if (StringUtils.containsIgnoreCase(html, Constants.PAGE_TEXT_NO_ADS_AVAILABLE)) {
                processNoAdAvailablePage()
            } else if (StringUtils.containsIgnoreCase(html,Constants.PAGE_TEXT_TURING_TEST)) {
                processCaptchaPage(html)
            } else {
                processAdPage()
            }
        }
    }

}