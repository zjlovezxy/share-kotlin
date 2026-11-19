package io.github.zjlovezxy.sharekt.core.network

/**
 * 协议常量集中在这里。
 *
 * 为什么走 h5（m.weibo.cn）而不是 mapi（api.weibo.cn/2）：
 * mapi 每个请求都要带客户端签名参数 `s`，那是原版 App 的私有算法
 * （缺 s 一律 errno=-200「校验参数不存在」，乱猜则是 -105「客户端身份校验失败」）。
 * h5 的 container/getIndex、statuses/show、comments/hotflow 只要登录 cookie 就够。
 */
object ApiConfig {

    /** mapi 基址，保留给以后能复刻签名时用。 */
    const val BASE_URL = "https://api.weibo.cn/2/"

    const val H5_BASE = "https://m.weibo.cn"
    const val H5_INDEX_URL = "$H5_BASE/api/container/getIndex"
    const val H5_SHOW_URL = "$H5_BASE/statuses/show"
    const val H5_COMMENTS_URL = "$H5_BASE/comments/hotflow"
    const val H5_EXTEND_URL = "$H5_BASE/statuses/extend"

    /** 登录态校验 / 取 uid。 */
    const val CONFIG_URL = "$H5_BASE/api/config"
    const val LOGIN_URL = "$H5_BASE/login"

    /** 首页容器：102803 = 推荐/广场流；107603 + uid = 某人的微博。 */
    const val H5_CONTAINER_RECOMMEND = "102803"
    const val H5_CONTAINER_PROFILE = "107603"

    /** h5 接口要求的请求头，缺了会返回 ok=0 或干脆登录失效。 */
    val H5_HEADERS: Map<String, String> = mapOf(
        "Referer" to "$H5_BASE/",
        "X-Requested-With" to "XMLHttpRequest",
        "MWeibo-Pwa" to "1",
    )

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

    // ---- 端点（mapi 2/ 之后的部分，暂未启用）----
    const val EP_FRIENDS_TIMELINE = "statuses/unread_friends_timeline"
    const val EP_HOT_TIMELINE = "statuses/unread_hot_timeline"
    const val EP_VIDEO_TIMELINE = "statuses/video_mixtimeline"
    const val EP_USER_TIMELINE = "statuses/user_timeline"
    const val EP_STATUS_SHOW_BATCH = "statuses/show_batch"
    const val EP_COMMENTS_SHOW = "comments/show"
    const val EP_USERS_SHOW_BATCH = "users/show_batch"
    const val EP_SEARCH_STATUSES = "search/statuses"
    const val EP_UNREAD_COUNT = "remind/unread_count"
}
