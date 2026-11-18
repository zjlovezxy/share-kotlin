package io.github.zjlovezxy.sharekt.core.network

/** code > 0 为服务端返回的错误码；code < 0 是本地错误（网络、解析）。 */
class MapiException(
    val code: Int,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    val needLogin: Boolean
        get() = code == 21332 || code == 21327 || code == -100

    companion object {
        const val CODE_NETWORK = -1
        const val CODE_PARSE = -2
        const val CODE_NO_LOGIN = -100
    }
}

/** 给 UI 直接展示的一句话，别把 stacktrace 丢给用户。 */
fun Throwable.userMessage(): String = when (this) {
    is MapiException -> when {
        needLogin -> "登录已失效，请重新登录"
        code == MapiException.CODE_NETWORK -> "网络不可用，请检查连接"
        code == MapiException.CODE_PARSE -> "数据解析失败，稍后重试"
        else -> message.orEmpty().ifBlank { "请求失败（$code）" }
    }
    else -> message.orEmpty().ifBlank { "请求失败" }
}
