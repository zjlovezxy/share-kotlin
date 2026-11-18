package io.github.zjlovezxy.sharekt.core.model

import kotlinx.serialization.Serializable

/** mapi 用户对象的最小投影，字段缺失一律给默认值。 */
@Serializable
data class WeiboUser(
    val id: String = "",
    val screenName: String = "",
    val avatarUrl: String = "",
    val verified: Boolean = false,
    val verifiedReason: String = "",
    val followersCount: Long = 0,
    val friendsCount: Long = 0,
    val statusesCount: Long = 0,
    val description: String = "",
    val coverUrl: String = "",
    val following: Boolean = false,
)
