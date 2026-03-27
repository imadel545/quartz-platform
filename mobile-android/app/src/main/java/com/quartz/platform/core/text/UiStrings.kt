package com.quartz.platform.core.text

import androidx.annotation.StringRes

interface UiStrings {
    fun get(@StringRes resId: Int, vararg formatArgs: Any): String
}
