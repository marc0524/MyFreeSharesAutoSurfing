package com.vincent.mfssurfing

import org.apache.commons.lang3.StringUtils

class Constants {

    companion object {

        val MESSAGE_KEY_OPERATOR = "operationWrapper"

        val URL_MFS_HOME = "http://myfreeshares.com/pages/enternew.php"
        val URL_PREFIX_AD_PAGE = "http://myfreeshares.com/scripts/runner.php?PA="
        val URL_PREFIX_CAPTCHA_NUMBER = "http://myfreeshares.com"

        val PARAM_AD_NUMBER = "PA="
        val PARAM_CAPTCHA_NUMBER = "PI="

        val PAGE_TEXT_AD_WORTH = "The ad above is worth"
        val PAGE_TEXT_NO_ADS_AVAILABLE = "Sorry, no ads are available for you to click on this page at this time"
        val PAGE_TEXT_TURING_TEST = "Please click the matching number to continue to the advertisement"

        val TAG_REGEX_AD = "a[href^=http://myfreeshares.com/scripts/runner.php?PA=]"
        val TAG_REGEX_CAPTCHA_NUMBER = "a[href^=/scripts/runner.php?KE=]"

        val ATTR_HREF = "href"
        val ATTR_ON_CLICK = "onClick"

        val IGNORABLE_AD_NUMBER_DELIMETER = ','
        val IGNORABLE_AD_DEFAULT_NUMBER =
            StringUtils.join(arrayOf("4163", "8710", "9126"), IGNORABLE_AD_NUMBER_DELIMETER)

        val JAVASCRIPT_NAME = "HTMLOUT"
        val JAVASCRIPT_SYNTAX = "javascript:HTMLOUT.processHTML(document.documentElement.outerHTML);"

        val MINIMUM_BROWSING_SKIP_TIME = 45
        val KEY_IS_HANDLING_CAPTCHA_ALLOWED = "isHandlingCaptchaAllowed"
        val KEY_AD_BROWSING_SKIP_SECOND = "adBrowsingSkipTime"
        val KEY_IGNORED_AD_NUMBERS = "ignoredAdNumbers"

    }

}