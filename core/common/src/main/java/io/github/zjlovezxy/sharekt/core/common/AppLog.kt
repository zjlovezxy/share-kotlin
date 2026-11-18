package io.github.zjlovezxy.sharekt.core.common

import android.util.Log

/** 统一日志出口：debug 全开，release 只留 warn 以上。 */
object AppLog {

    private const val PREFIX = "ShareKt"

    fun d(tag: String, message: String) = Log.d("$PREFIX/$tag", message)

    fun w(tag: String, message: String, t: Throwable? = null) =
        Log.w("$PREFIX/$tag", message, t)

    fun e(tag: String, message: String, t: Throwable? = null) =
        Log.e("$PREFIX/$tag", message, t)
}
