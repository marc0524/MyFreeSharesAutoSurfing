package com.vincent.mfssurfing

import android.content.Context
import android.content.SharedPreferences
import org.apache.commons.lang3.StringUtils

class PreferencesHelper(
    context: Context) {

    private val sp: SharedPreferences = context.getSharedPreferences("MFS", Context.MODE_PRIVATE)

    private fun getAdNumbersString(ignoredAdNumbers: Iterable<String>): String {
        return StringUtils.join(ignoredAdNumbers, Constants.IGNORABLE_AD_NUMBER_DELIMETER)
    }

    fun getIgnoredAdList(): MutableList<String> {
        return sp.getString(Constants.KEY_IGNORED_AD_NUMBERS, Constants.IGNORABLE_AD_DEFAULT_NUMBER)
                .split(Constants.IGNORABLE_AD_NUMBER_DELIMETER)
                .toMutableList()
    }

    fun addIgnoredAdNumber(adNumber: String) {
        val ignoredAdNumbers = sp.getString(Constants.KEY_IGNORED_AD_NUMBERS, Constants.IGNORABLE_AD_DEFAULT_NUMBER)
            .split(Constants.IGNORABLE_AD_NUMBER_DELIMETER)
            .toSortedSet()

        ignoredAdNumbers.add(adNumber)

        sp.edit()
            .putString(Constants.KEY_IGNORED_AD_NUMBERS, getAdNumbersString(ignoredAdNumbers))
            .apply()
    }

    fun deleteIgnoredAdNumber(adNumber: String) {
        val ignoredAdNumbers = sp.getString(Constants.KEY_IGNORED_AD_NUMBERS, Constants.IGNORABLE_AD_DEFAULT_NUMBER)
            .split(Constants.IGNORABLE_AD_NUMBER_DELIMETER)
            .toSortedSet()

        ignoredAdNumbers.remove(adNumber)

        sp.edit()
            .putString(Constants.KEY_IGNORED_AD_NUMBERS, getAdNumbersString(ignoredAdNumbers))
            .apply()
    }

    fun isHandlingCaptchaAllowed(): Boolean {
        return sp.getBoolean(Constants.KEY_IS_HANDLING_CAPTCHA_ALLOWED, true)
    }

    fun setHandlingCaptchaBehavior(allowed: Boolean) {
        sp.edit()
            .putBoolean(Constants.KEY_IS_HANDLING_CAPTCHA_ALLOWED, allowed)
            .apply()
    }

}