package com.vincent.mfssurfing

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
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

    @BindView(R.id.txtStatus) lateinit var txtStatus: TextView
    @BindView(R.id.webView) lateinit var webView: WebView
    @BindView(R.id.lstSettings) lateinit var expandableListView: ExpandableListView
    @BindView(R.id.navBar) lateinit var navBar: BottomNavigationView

    private val adPageStack: Stack<AdPage> = Stack()
    private var turingNumberUrlStack: Stack<String> = Stack()

    private lateinit var selectedAdPage: AdPage
    private lateinit var selectedAdHomeUrl: String

    private var isSurfRunning: Boolean = false
    private var hasAdSinceLastCheck: Boolean = false

    private lateinit var thread: Thread
    private var timerThread: Thread = Thread()

    private lateinit var sp: SharedPreferences
    private var jumpTimeSec: Int = 60
    private var canHandleTuringTest: Boolean = true
    private lateinit var ignoredAdNumbers: List<String>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        sp = getSharedPreferences("MFS", Context.MODE_PRIVATE)

        with(webView) {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            addJavascriptInterface(JavaScriptInterface(), Constants.JAVASCRIPT_NAME)
            loadUrl(Constants.URL_MFS_HOME)
        }

        setupNavigationBar()
        setupSettingOptions()
    }

    private fun setupNavigationBar() {
        navBar.setOnNavigationItemSelectedListener { item: MenuItem ->
            when(item.itemId) {
                R.id.navHome -> {
                    if (expandableListView.visibility != View.VISIBLE) {
                        goMFSHome()
                    } else {
                        hideSettingSection()
                    }
                }

                R.id.navStartStop -> {
                    if (expandableListView.visibility != View.VISIBLE) {
                        switchSurfingOperation(item)
                    } else {
                        hideSettingSection()
                    }
                }

                R.id.navSettings -> {
                    if (expandableListView.visibility == View.VISIBLE) {
                        hideSettingSection()
                    } else {
                        showSettingSection()
                    }
                }
            }
            true
        }

        navBar.setOnNavigationItemReselectedListener { item: MenuItem ->
            when(item.itemId) {
                R.id.navHome -> {
                    if (expandableListView.visibility == View.VISIBLE) {
                        hideSettingSection()
                    } else {
                        goMFSHome()
                    }
                }

                R.id.navStartStop -> {
                    if (expandableListView.visibility == View.VISIBLE) {
                        hideSettingSection()
                    } else {
                        switchSurfingOperation(item)
                    }
                }
            }
        }
    }

    private fun setupSettingOptions() {
        val ignoredAdNumbers = sp.getString(Constants.KEY_IGNORED_AD_NUMBERS, Constants.IGNORABLE_AD_DEFAULT_NUMBER)
            .split(Constants.IGNORABLE_AD_NUMBER_DELIMETER)
            .toMutableList()

        val inflater: LayoutInflater = LayoutInflater.from(this)

        val view1 = SettingOptionView(
            getString(R.string.title_handle_turing_test),
            getString(R.string.desc_handle_turing_test),
            getRadioGroupOfHandleTuringTest(inflater)
        )

        val view2 = SettingOptionView(
            getString(R.string.title_mandatory_jump),
            getString(R.string.desc_mandatory_jump),
            getSeekBarOfMandatoryJumpTime(inflater)
        )

        val view3 = SettingOptionView(
            "${getString(R.string.title_ignore_ad)} (${ignoredAdNumbers.size})",
            getString(R.string.desc_ignore_ad),
            View(this)
        )

        ignoredAdNumbers.add(0, getString(R.string.label_add_ad))

        val groupContents = listOf(view1, view2, view3)
        val childContent  = listOf<List<String>>(listOf(), listOf(), ignoredAdNumbers)

        expandableListView.setAdapter(SettingOptionAdapter(this, groupContents, childContent))
    }

    private fun getRadioGroupOfHandleTuringTest(inflater: LayoutInflater): RadioGroup {
        val radioGroup = inflater.inflate(R.layout.radio_handle_turing_test, null) as RadioGroup

        canHandleTuringTest = sp.getBoolean(Constants.KEY_CAN_HANDLE_TURING_TEST, true)
        if (canHandleTuringTest) {
            radioGroup.check(R.id.rdoProceedTuring)
        } else {
            radioGroup.check(R.id.rdoEscapeTuring)
        }

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when(checkedId) {
                R.id.rdoProceedTuring -> {
                    sp.edit().putBoolean(Constants.KEY_CAN_HANDLE_TURING_TEST, true).apply()
                }

                R.id.rdoEscapeTuring -> {
                    sp.edit().putBoolean(Constants.KEY_CAN_HANDLE_TURING_TEST, false).apply()
                }
            }
        }

        return radioGroup
    }

    private fun getSeekBarOfMandatoryJumpTime(inflater: LayoutInflater): RelativeLayout {
        val layout = inflater.inflate(R.layout.seekbar_mandatory_jump_time, null) as RelativeLayout
        val seekBar = layout.findViewById<SeekBar>(R.id.seekJumpTime)
        val txtJumpTime = layout.findViewById<TextView>(R.id.txtJumpTime)

        jumpTimeSec = sp.getInt(Constants.KEY_MANDATORY_JUMP_TIME, 60)
        txtJumpTime.text = getString(R.string.label_jump_time, jumpTimeSec)
        seekBar.progress = (jumpTimeSec - Constants.MINIMUM_MANDATORY_JUMP_TIME) / 5

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                txtJumpTime.text = getString(R.string.label_jump_time,
                    progress * 5 + Constants.MINIMUM_MANDATORY_JUMP_TIME)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                sp.edit().putInt(Constants.KEY_MANDATORY_JUMP_TIME,
                    seekBar!!.progress * 5 + Constants.MINIMUM_MANDATORY_JUMP_TIME).apply()
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

            jumpTimeSec = sp.getInt(Constants.KEY_MANDATORY_JUMP_TIME, 60)
            canHandleTuringTest = sp.getBoolean(Constants.KEY_CAN_HANDLE_TURING_TEST, true)

            ignoredAdNumbers = sp.getString(Constants.KEY_IGNORED_AD_NUMBERS, Constants.IGNORABLE_AD_DEFAULT_NUMBER)
                    .split(Constants.IGNORABLE_AD_NUMBER_DELIMETER).toList()

            selectedAdHomeUrl = webView.originalUrl
            webView.webViewClient = getSpecialWebViewClient()
            webView.reload()
        }

        isSurfRunning = !isSurfRunning
    }

    private fun showSettingSection() {
        txtStatus.visibility = View.GONE
        webView.visibility = View.GONE
        expandableListView.visibility = View.VISIBLE
        updateStatus(getString(R.string.stand_by))
    }

    private fun hideSettingSection() {
        txtStatus.visibility = View.VISIBLE
        webView.visibility = View.VISIBLE
        expandableListView.visibility = View.GONE
    }

    private fun processAdList(html: String) {
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
            processNoAdAvailable()
        }
    }

    private fun processNoAdAvailable() {
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

        startThread(operator, 0)
    }

    private fun processTuringTest(html: String) {
        if (!canHandleTuringTest) {
            stopTimer()
            return
        }

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

            val operator = object : Operator {
                override fun execute() {
                    if (isSurfRunning) {
                        browseNextAd()
                    }
                }
            }

            startThread(operator, duration * 1000L)
        }
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
        val operator = object : Operator {
            override fun execute() {
                webView.loadUrl(url)
            }
        }

        startThread(operator, 0)
    }

    private fun updateStatus(message: String) {
        val operator = object : Operator {
            override fun execute() {
                txtStatus.text = message
            }
        }

        startThread(operator, 0)
    }

    private fun launchTimer() {
        val operator = object : Operator {
            override fun execute() {
                browseNextAd()
            }
        }

        timerThread = object : Thread() {
            override fun run() {
                super.run()

                try {
                    Thread.sleep(jumpTimeSec * 1000L)
                } catch (e: InterruptedException) {
                    return
                }

                handler.sendMessage(generateMessage(operator))
            }
        }
        timerThread.start()
    }

    private fun stopTimer() {
        if (!timerThread.isInterrupted) {
            timerThread.interrupt()
        }
    }

    private fun isVisitableAd(adUrl: String): Boolean {
        for (number in ignoredAdNumbers) {
            if (StringUtils.equals(StringUtils.substringAfter(adUrl, Constants.PARAM_AD_NUMBER), number)) {
                return false
            }
        }

        return true
    }

    private fun startThread(operator: Operator, delayTimeMill: Long) {
        thread = object : Thread() {
            override fun run() {
                super.run()
                handler.sendMessageDelayed(generateMessage(operator), delayTimeMill)
            }
        }
        thread.start()
    }

    private fun generateMessage(operator: Operator): Message {
        val bundle = Bundle()
        bundle.putSerializable(Constants.MESSAGE_KEY_OPERATOR, operator)

        val message = Message()
        message.data = bundle

        return message
    }

    val handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)

            val operator = msg?.data?.get(Constants.MESSAGE_KEY_OPERATOR) as Operator
            operator.execute()
        }
    }

    private fun getSpecialWebViewClient(): WebViewClient {
        return object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (StringUtils.startsWith(url, Constants.URL_PREFIX_AD_PAGE)) {
                    launchTimer()
                    val adNumber = StringUtils.substringAfter(selectedAdPage.url, Constants.PARAM_AD_NUMBER)
                    updateStatus("${getString(R.string.loading_ad_page)}$adNumber")
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        stopTimer()
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
            } else {
                processAdPage()
            }
        }
    }

}
