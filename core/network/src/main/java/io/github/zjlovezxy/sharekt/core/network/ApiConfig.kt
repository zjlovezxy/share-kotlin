package io.github.zjlovezxy.sharekt.core.network

/** 所有协议常量集中在这里，mapi 变更只改这个文件。 */
object ApiConfig {

    const val BASE_URL = "https://api.weibo.cn/2/"

    /** 登录态校验 / 取 uid 走 h5 端，比 mapi 稳。 */
    const val CONFIG_URL = "https://m.weibo.cn/api/config"
    const val LOGIN_URL = "https://m.weibo.cn/login"

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

    // ---- 端点（mapi 2/ 之后的部分）----
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
