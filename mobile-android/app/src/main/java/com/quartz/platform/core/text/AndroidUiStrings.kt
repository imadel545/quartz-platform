package com.quartz.platform.core.text

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidUiStrings @Inject constructor(
    @ApplicationContext private val context: Context
) : UiStrings {
    override fun get(@StringRes resId: Int, vararg formatArgs: Any): String {
        return if (formatArgs.isEmpty()) {
            context.getString(resId)
        } else {
            context.getString(resId, *formatArgs)
        }
    }
}
