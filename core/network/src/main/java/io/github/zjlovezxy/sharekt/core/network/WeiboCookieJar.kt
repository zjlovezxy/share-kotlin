package io.github.zjlovezxy.sharekt.core.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * 内存 + 持久化双层 CookieJar。同名同域按 name 去重，后到的覆盖先到的，
 * 否则服务端轮换 SUB 后会同时存在两个旧值，请求带上哪个看运气。
 */
class WeiboCookieJar(private val storage: CookieStorage) : CookieJar {

    private val lock = Any()
    private val cache = LinkedHashMap<String, Cookie>()

    init {
        synchronized(lock) {
            storage.read().forEach { cache[keyOf(it)] = it }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(lock) {
        val now = System.currentTimeMillis()
        cache.values.filter { it.matches(url) && it.expiresAt > now }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        synchronized(lock) {
            cookies.forEach { cache[keyOf(it)] = it }
            storage.write(cache.values.toList())
        }
    }

    fun clear() = synchronized(lock) {
        cache.clear()
        storage.clear()
    }

    private fun keyOf(c: Cookie): String = "${c.domain}|${c.path}|${c.name}"

    /** WebView 登录后把 cookie 塞进来；域名统一挂到 .weibo.cn，mapi 与 h5 共用。 */
    fun seed(pairs: Map<String, String>) {
        if (pairs.isEmpty()) return
        val cookies = pairs.mapNotNull { (name, value) ->
            runCatching {
                Cookie.Builder().name(name).value(value).domain(".weibo.cn").path("/").build()
            }.getOrNull()
        }
        saveFromResponse(SEED_URL, cookies)
    }

    private companion object {
        val SEED_URL = "https://api.weibo.cn/2/".toHttpUrl()
    }
}
