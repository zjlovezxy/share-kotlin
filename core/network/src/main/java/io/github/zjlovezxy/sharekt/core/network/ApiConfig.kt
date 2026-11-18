package io.github.zjlovezxy.sharekt.core.network

/** 所有协议常量集中在这里，mapi 变更只改这个文件。 */
object ApiConfig {

    const val BASE_URL = "https://api.weibo.cn/2/"

    /** 客户端标识，mapi 的 from 参数。 */
    const val FROM = "10B6395010"

    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Mobile Safari/537.36"

    const val PARAM_CONTAINER = "c"
    const val PARAM_CURSOR = "s"
    const val PARAM_PAGE_SIZE = "i"
    const val PARAM_FROM = "from"
    const val PARAM_GSID = "gsid"
    const val PARAM_LANG = "lang"

    const val HEADER_XSRF = "X-XSRF-TOKEN"
    const val COOKIE_SUB = "SUB"
    const val COOKIE_SUBP = "SUBP"
}
