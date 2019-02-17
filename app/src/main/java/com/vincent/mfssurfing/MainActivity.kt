package com.vincent.mfssurfing

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.BottomNavigationView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.*
import butterknife.BindView
import butterknife.ButterKnife
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.util.*

class MainActivity : AppCompatActivity() {
    // https://www.jianshu.com/p/aa499cc64f72
    // https://www.jianshu.com/p/21068fde1c82
    // https://www.jianshu.com/p/05df9c17a1d8
    // https://www.jianshu.com/p/5eaa129432bf
    // https://www.jianshu.com/p/476d3ed50db1
    // http://blog.maxkit.com.tw/2014/01/android-serviceintentservice.html

    @BindView(R.id.txtStatus) lateinit var txtStatus: TextView
    @BindView(R.id.webView) lateinit var webView: WebView
    @BindView(R.id.lstSettings) lateinit var expandableListView: ExpandableListView
    @BindView(R.id.navBar) lateinit var navBar: BottomNavigationView

    private val adPageStack: Stack<AdPage> = Stack()
    private var captchaNumberUrlStack: Stack<String> = Stack()

    private lateinit var selectedAdPage: AdPage
    private lateinit var selectedAdHomeUrl: String

    private var isSurfRunning: Boolean = false
    private var hasAdSinceLastCheck: Boolean = false
    private var isCredited: Boolean = false

    private var skipTimerThread: Thread? = null
    private var creditedTimerThread: Thread? = null

    private var myService: MyService? = null
    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var threadHelper: ThreadHelper

    private var skipSecond: Int = 60
    private var isHandlingCaptchaAllowed: Boolean = true
    private lateinit var ignoredAdNumbers: List<String>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        preferencesHelper = PreferencesHelper(this)
        threadHelper = ThreadHelper()

        setupNavigationBar()
        setupSettingOptions()

        with(webView) {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            addJavascriptInterface(JavaScriptInterface(), Constants.JAVASCRIPT_NAME)
            loadUrl(Constants.URL_MFS_HOME)
        }

        bindService(Intent(this, MyService::class.java), serviceCon, Context.BIND_AUTO_CREATE)
    }

    private fun setupNavigationBar() {
        navBar.setOnNavigationItemSelectedListener { item: MenuItem ->
            when(item.itemId) {
                R.id.navHome -> {
                    if (expandableListView.visibility != View.VISIBLE) {
                        goMFSHome()
                    } else {
                        displaySettingSection(false)
                    }
                }

                R.id.navStartStop -> {
                    if (expandableListView.visibility != View.VISIBLE) {
                        switchSurfingOperation(item)
                    } else {
                        displaySettingSection(false)
                    }
                }

                R.id.navSettings -> {
                    if (expandableListView.visibility == View.VISIBLE) {
                        displaySettingSection(false)
                    } else {
                        displaySettingSection(true)
                    }
                }
            }
            true
        }

        navBar.setOnNavigationItemReselectedListener { item: MenuItem ->
            when(item.itemId) {
                R.id.navHome -> {
                    if (expandableListView.visibility == View.VISIBLE) {
                        displaySettingSection(false)
                    } else {
                        goMFSHome()
                    }
                }

                R.id.navStartStop -> {
                    if (expandableListView.visibility == View.VISIBLE) {
                        displaySettingSection(false)
                    } else {
                        switchSurfingOperation(item)
                    }
                }
            }
        }
    }

    private fun setupSettingOptions() {
        val ignoredAdNumbers = preferencesHelper.getIgnoredAdList()
        val inflater: LayoutInflater = LayoutInflater.from(this)

        val view1 = SettingOptionView(
            getString(R.string.title_handle_captcha),
            getString(R.string.desc_handle_captcha),
            getRadioGroupOfHandleCaptcha(inflater)
        )

        val view2 = SettingOptionView(
            getString(R.string.title_skip_time),
            getString(R.string.desc_skip_time),
            getSeekBarOfBrowsingSkipTime(inflater)
        )

        val view3 = SettingOptionView(
            "${getString(R.string.title_ignore_ad)} (${ignoredAdNumbers.size})",
            getString(R.string.desc_ignore_ad),
            View(this)
        )

        ignoredAdNumbers.add(0, getString(R.string.label_add_ad))
        val groupContents = listOf(view1, view2, view3)
        val childContent  = listOf(mutableListOf(), mutableListOf(), ignoredAdNumbers)

        expandableListView.setAdapter(SettingOptionAdapter(this, groupContents, childContent))
    }

    private fun getRadioGroupOfHandleCaptcha(inflater: LayoutInflater): RadioGroup {
        val radioGroup = inflater.inflate(R.layout.radio_handle_captcha, null) as RadioGroup

        if (preferencesHelper.isHandlingCaptchaAllowed()) {
            radioGroup.check(R.id.rdoAllowHandlingCaptcha)
        } else {
            radioGroup.check(R.id.rdoDisallowHandlingCaptcha)
        }

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when(checkedId) {
                R.id.rdoAllowHandlingCaptcha -> {
                    preferencesHelper.setHandlingCaptchaBehavior(true)
                }

                R.id.rdoDisallowHandlingCaptcha -> {
                    preferencesHelper.setHandlingCaptchaBehavior(false)
                }
            }
        }

        return radioGroup
    }

    private fun getSeekBarOfBrowsingSkipTime(inflater: LayoutInflater): RelativeLayout {
        val layout = inflater.inflate(R.layout.seekbar_browsing_skip_time, null) as RelativeLayout
        val seekBar = layout.findViewById<SeekBar>(R.id.seekSkipTime)
        val txtSkipTime = layout.findViewById<TextView>(R.id.txtSkipTime)

        skipSecond = preferencesHelper.getAdBrowsingSkipSecond()
        txtSkipTime.text = getString(R.string.label_skip_time, skipSecond)
        seekBar.progress = (skipSecond - Constants.MINIMUM_BROWSING_SKIP_TIME) / 5

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                txtSkipTime.text = getString(R.string.label_skip_time,
                    progress * 5 + Constants.MINIMUM_BROWSING_SKIP_TIME)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                preferencesHelper
                    .setAdBrowsingSkipSecond(seekBar!!.progress * 5 + Constants.MINIMUM_BROWSING_SKIP_TIME)
            }

        })

        return layout
    }

    private fun goMFSHome() {
        if (isSurfRunning) {
            Toast.makeText(applicationContext, getString(R.string.do_not_disturb_surfing), Toast.LENGTH_SHORT).show()
        } else if (StringUtils.equals(webView.originalUrl, Constants.URL_MFS_HOME)) {
            Toast.makeText(applicationContext, getString(R.string.landing_home_already), Toast.LENGTH_SHORT).show()
        } else {
            myService?.updateText(txtStatus, getString(R.string.stand_by))
            webView.webViewClient = WebViewClient()
            webView.loadUrl(Constants.URL_MFS_HOME)
        }
    }

    private fun switchSurfingOperation(item: MenuItem) {
        if (isSurfRunning) {
            threadHelper.stopThread(skipTimerThread)
            threadHelper.stopThread(creditedTimerThread)

            myService?.updateText(txtStatus, getString(R.string.surfing_stopped))
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

            myService?.updateText(txtStatus, getString(R.string.loading_ad_list))
            with(item) {
                title = getString(R.string.stop_surfing)
                setIcon(R.drawable.icon_stop)
            }

            skipSecond = preferencesHelper.getAdBrowsingSkipSecond()
            isHandlingCaptchaAllowed = preferencesHelper.isHandlingCaptchaAllowed()

            ignoredAdNumbers = preferencesHelper.getIgnoredAdList()

            selectedAdHomeUrl = webView.originalUrl
            webView.webViewClient = getSpecialWebViewClient()
            webView.reload()
        }

        isSurfRunning = !isSurfRunning
    }

    private fun displaySettingSection(show: Boolean) {
        if (show) {
            txtStatus.visibility = View.GONE
            webView.visibility = View.GONE
            expandableListView.visibility = View.VISIBLE
            myService?.updateText(txtStatus, getString(R.string.stand_by))
        } else {
            txtStatus.visibility = View.VISIBLE
            webView.visibility = View.VISIBLE
            expandableListView.visibility = View.GONE
        }
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
        val operator = object : Operator {
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

        threadHelper.startThread(operator, 0)
    }

    private fun processCaptchaPage(html: String) {
        threadHelper.stopThread(creditedTimerThread)

        if (!isHandlingCaptchaAllowed) {
            threadHelper.stopThread(skipTimerThread)
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

        myService?.updateText(txtStatus, "${getString(R.string.loading_ad_page)}${selectedAdPage.name}${getString(R.string.try_captcha_number)}$captchaNumber")
        myService?.browseUrl(webView, url)
    }

    private fun processAdPage() {
        threadHelper.stopThread(skipTimerThread)
        captchaNumberUrlStack.clear()

        if (isCredited) {
            browseNextAd()
            return
        }

        threadHelper.stopThread(creditedTimerThread)

        with(selectedAdPage) {
            myService?.updateText(txtStatus, "${getString(R.string.browsing_ad_page)}$name${getString(R.string.time)}$duration${getString(R.string.second)}")

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
                myService?.updateText(txtStatus, getString(R.string.checking_left_ads))
                hasAdSinceLastCheck = false
                myService?.browseUrl(webView, selectedAdHomeUrl)
            } else {
                processNoAdAvailablePage()
            }
        } else {
            selectedAdPage = adPageStack.pop()
            myService?.browseUrl(webView, selectedAdPage.url)
        }
    }

    private fun isVisitableAd(adUrl: String): Boolean {
        return !ignoredAdNumbers.contains(
            StringUtils.substringAfter(adUrl, Constants.PARAM_AD_NUMBER))
    }

    private fun launchSkipTimer() {
        val operator = object : Operator {
            override fun execute() {
                Toast.makeText(applicationContext, getString(R.string.going_to_skip_ad), Toast.LENGTH_SHORT).show()
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

    private fun getSpecialWebViewClient(): WebViewClient {
        return object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (StringUtils.startsWith(url, Constants.URL_PREFIX_AD_PAGE)) {
                    isCredited = false
                    launchSkipTimer()
                    launchCreditTimer(selectedAdPage.duration)

                    myService?.updateText(txtStatus, "${getString(R.string.loading_ad_page)}${selectedAdPage.name}")
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.loadUrl(Constants.JAVASCRIPT_SYNTAX)
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

    override fun onDestroy() {
        super.onDestroy()
        threadHelper.stopThread(skipTimerThread)
        threadHelper.stopThread(creditedTimerThread)
        threadHelper.handler.removeCallbacksAndMessages(null)
        unbindService(serviceCon)
    }

    private val serviceCon = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            myService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            myService = (service as MyService.MyBinder).getService()
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
