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

    fun startThread(operator: Operator, delayTimeMill: Long): Thread {
        val thread = object : Thread() {
            override fun run() {
                super.run()
                try {
                    Thread.sleep(delayTimeMill)
                    handler.sendMessage(generateMessage(operator))
                } catch (e: InterruptedException) {
                }
            }
        }
        thread.start()

        return thread
    }

    fun stopThread(thread: Thread?) {
        if (thread != null && thread.isAlive) {
            thread.interrupt()
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