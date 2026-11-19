package io.github.zjlovezxy.sharekt.core.auth

import io.github.zjlovezxy.sharekt.core.common.AppLog
import io.github.zjlovezxy.sharekt.core.network.ApiConfig
import io.github.zjlovezxy.sharekt.core.network.MapiClient
import io.github.zjlovezxy.sharekt.core.network.MapiException
import io.github.zjlovezxy.sharekt.core.network.Session
import io.github.zjlovezxy.sharekt.core.network.WeiboCookieJar
import io.github.zjlovezxy.sharekt.core.network.obj
import io.github.zjlovezxy.sharekt.core.network.bool
import io.github.zjlovezxy.sharekt.core.network.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 登录只做一件事：把 WebView 里拿到的 cookie 换成 Session，并用 /api/config 验一下 login=true。
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
        if (pairs[ApiConfig.COOKIE_SUB].isNullOrEmpty()) {
            throw MapiException(MapiException.CODE_NO_LOGIN, "没有拿到 SUB cookie")
        }
        jar.seed(pairs)

        val config = client.getUrl(ApiConfig.CONFIG_URL, ApiConfig.H5_HEADERS)
        val data = config.obj("data")
            ?: throw MapiException(MapiException.CODE_PARSE, "登录态校验失败")
        if (!data.bool("login")) {
            throw MapiException(MapiException.CODE_NO_LOGIN, "登录未完成")
        }
        val uid = data.str("uid")

        var screenName = ""
        var avatar = ""
        runCatching {
            val info = client.getUrl(
                ApiConfig.H5_INDEX_URL,
                ApiConfig.H5_HEADERS,
                mapOf("type" to "uid", "value" to uid),
            )
            val user = info.obj("data")?.obj("userInfo")
            screenName = user?.str("screen_name").orEmpty()
            avatar = user?.str("avatar_large").orEmpty()
        }.onFailure { AppLog.w(TAG, "拉取用户资料失败（不影响登录态）", it) }

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
