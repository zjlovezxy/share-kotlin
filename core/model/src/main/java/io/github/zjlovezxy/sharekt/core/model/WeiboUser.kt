package io.github.zjlovezxy.sharekt.core.model

/** mapi 用户对象的最小投影，字段缺失一律给默认值。 */
data class WeiboUser(
    val id: String = "",
    val screenName: String = "",
    val avatarUrl: String = "",
    val verified: Boolean = false,
    val followersCount: Long = 0,
)
