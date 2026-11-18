package io.github.zjlovezxy.sharekt.core.network

import kotlinx.serialization.Serializable

/** 登录态快照。多账号靠 per-uid 存储隔离，运行时只保留当前账号。 */
@Serializable
data class Session(
    val sub: String = "",
    val subp: String = "",
    val gsid: String = "",
    val uid: String = "",
    val screenName: String = "",
    val avatarUrl: String = "",
) {
    val isLoggedIn: Boolean get() = sub.isNotEmpty()
}
