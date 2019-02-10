package com.vincent.mfssurfing

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.webkit.WebView
import android.widget.TextView

class ThreadHelper {

    val handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)

            val operator = msg?.data?.get(Constants.MESSAGE_KEY_OPERATOR) as Operator
            operator.execute()
        }
    }

    fun browseUrl(webView: WebView, url: String) {
        val operator = object : Operator {
            override fun execute() {
                webView.loadUrl(url)
            }
        }
        startThread(operator, 0)
    }

    fun updateStatus(txtStatus: TextView, message: String) {
        val operator = object : Operator {
            override fun execute() {
                txtStatus.text = message
            }
        }
        startThread(operator, 0)
    }

    fun startThread(operator: Operator, delayTimeMill: Long) {
        val thread = object : Thread() {
            override fun run() {
                super.run()
                handler.sendMessageDelayed(generateMessage(operator), delayTimeMill)
            }
        }
        thread.start()
    }

    fun launchTimer(operator: Operator, sleepTimeMill: Long): Thread {
        val thread = object : Thread() {
            override fun run() {
                super.run()

                try {
                    Thread.sleep(sleepTimeMill)
                    handler.sendMessage(generateMessage(operator))
                } catch (e: InterruptedException) {
                }
            }
        }
        thread.start()

        return thread
    }

    fun stopTimer(timerThread: Thread) {
        if (!timerThread.isInterrupted) {
            timerThread.interrupt()
        }
    }

    private fun generateMessage(operator: Operator): Message {
        val bundle = Bundle()
        bundle.putSerializable(Constants.MESSAGE_KEY_OPERATOR, operator)

        val message = Message()
        message.data = bundle

        return message
    }

}