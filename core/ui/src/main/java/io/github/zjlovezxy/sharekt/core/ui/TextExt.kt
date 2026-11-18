package io.github.zjlovezxy.sharekt.core.ui

import android.widget.TextView
import androidx.core.text.HtmlCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 微博正文是 HTML 片段（话题、@、表情都是标签），统一走这里渲染。 */
fun TextView.setRichText(html: String) {
    text = runCatching {
        HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }.getOrDefault(html)
}

fun TextView.setPlainText(html: String) {
    text = html.replace(TAG_PATTERN, "")
}

fun String.stripHtml(): String = replace(TAG_PATTERN, "").trim()

private val TAG_PATTERN = Regex("<[^>]*>")

/** 时间统一显示"刚刚 / x 分钟前 / x 小时前 / x 天前 / MM-dd HH:mm"。 */
fun formatRelativeTime(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < 0L -> formatDate(epochMs)
        diff < MINUTE -> "刚刚"
        diff < HOUR -> "${diff / MINUTE} 分钟前"
        diff < DAY -> "${diff / HOUR} 小时前"
        diff < WEEK -> "${diff / DAY} 天前"
        else -> formatDate(epochMs)
    }
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR
private const val WEEK = 7 * DAY
