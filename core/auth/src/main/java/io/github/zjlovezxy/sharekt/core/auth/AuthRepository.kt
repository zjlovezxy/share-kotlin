package io.github.zjlovezxy.sharekt.core.auth

import io.github.zjlovezxy.sharekt.core.common.AppLog
import io.github.zjlovezxy.sharekt.core.network.ApiConfig
import io.github.zjlovezxy.sharekt.core.network.MapiClient
import io.github.zjlovezxy.sharekt.core.network.Session
import io.github.zjlovezxy.sharekt.core.network.WeiboCookieJar
import io.github.zjlovezxy.sharekt.core.network.obj
import io.github.zjlovezxy.sharekt.core.network.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 登录只做一件事：把 WebView 里拿到的 cookie 换成 Session。
 * 不做 SSO（拿不到官方 app key），不做密码登录（会被风控打）。
 */
class AuthRepository(
    private val client: MapiClient,
    private val jar: WeiboCookieJar,
    private val store: AuthStore,
) {

    fun current(): Session = store.load()

    fun isLoggedIn(): Boolean = current().isLoggedIn

    suspend fun loginWithCookie(cookieHeader: String): Session = withContext(Dispatchers.IO) {
        val pairs = parseCookies(cookieHeader)
        jar.seed(pairs)

        var uid = ""
        var screenName = ""
        var avatar = ""
        runCatching {
            val config = client.getAbsoluteJson(ApiConfig.CONFIG_URL, cookieHeader)
            val data = config.obj("data") ?: return@runCatching
            val user = data.obj("user")
            uid = data.str("uid").ifEmpty { user?.str("id").orEmpty() }
            screenName = user?.str("screen_name").orEmpty()
            avatar = user?.str("avatar_large").orEmpty()
        }.onFailure { AppLog.w(TAG, "拉取用户信息失败（不影响已拿到的 cookie）", it) }

        val session = Session(
            sub = pairs[ApiConfig.COOKIE_SUB].orEmpty(),
            subp = pairs[ApiConfig.COOKIE_SUBP].orEmpty(),
            uid = uid,
            screenName = screenName,
            avatarUrl = avatar,
        )
        store.save(session)
        AppLog.d(TAG, "登录完成 uid=$uid name=$screenName")
        session
    }

    fun logout() {
        jar.clear()
        store.clear()
    }

    private fun parseCookies(header: String): Map<String, String> {
        if (header.isBlank()) return emptyMap()
        return header.split(';').mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) return@mapNotNull null
            part.substring(0, index).trim() to part.substring(index + 1).trim()
        }.filter { it.first.isNotEmpty() && it.second.isNotEmpty() }.toMap()
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
