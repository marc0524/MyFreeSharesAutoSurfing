package com.vincent.mfssurfing

import org.apache.commons.lang3.StringUtils

enum class SurfingSpace(
    var url: String  = "") {

    PAID_TO_CLICK("http://www.myfreeshares.com/pages/member5"),
    SHARE_TO_CLICK("http://www.myfreeshares.com/pages/member5p.php"),
    CONTEST_PTC("http://www.myfreeshares.com/pages/member5c.php");

    companion object {
        fun fromUrlStartWith(url: String): SurfingSpace? {
            return values().firstOrNull { space ->
                StringUtils.startsWith(url, space.url)
            }
        }
    }

}