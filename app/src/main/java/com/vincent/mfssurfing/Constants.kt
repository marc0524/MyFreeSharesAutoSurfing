package com.vincent.mfssurfing

class Constants {

    companion object {

        val MESSAGE_KEY_OPERATION_WRAPPER = "operationWrapper"

        val URL_MFS_HOME = "http://myfreeshares.com/pages/enternew.php"
        val URL_PREFIX_AD_PAGE = "http://myfreeshares.com/scripts/runner.php?PA="
        val URL_PREFIX_TURING_NUMBER = "http://myfreeshares.com"

        val PARAM_AD_NUMBER = "PA="
        val PARAM_TURING_NUMBER = "PI="

        val PAGE_TEXT_AD_WORTH = "The ad above is worth"
        val PAGE_TEXT_NO_ADS_AVAILABLE = "Sorry, no ads are available for you to click on this page at this time"
        val PAGE_TEXT_TURING_TEST = "Please click the matching number to continue to the advertisement"
        val PAGE_TEXT_ACCOUNT_CREDITED = "Your account has been credited"

        val TAG_REGEX_AD = "a[href^=http://myfreeshares.com/scripts/runner.php?PA=]"
        val TAG_REGEX_TURING_NUMBER = "a[href^=/scripts/runner.php?KE=]"

        val ATTR_HREF = "href"
        val ATTR_ON_CLICK = "onClick"

        val DANGEROUS_AD_TITLE_KEYWORDS = listOf("Donate")

        val JAVASCRIPT_NAME = "HTMLOUT"
        val JAVASCRIPT_SYNTAX = "javascript:HTMLOUT.processHTML(document.documentElement.outerHTML);"


    }

}