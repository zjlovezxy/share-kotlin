package io.github.zjlovezxy.sharekt.core.network

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Cookie

/** Cookie 持久化接口；测试里可以塞内存实现。 */
interface CookieStorage {
    fun read(): List<Cookie>
    fun write(cookies: List<Cookie>)
    fun clear()
}

@Serializable
private data class StoredCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val expiresAt: Long,
    val secure: Boolean,
    val httpOnly: Boolean,
) {
    fun toCookie(): Cookie? = runCatching {
        Cookie.Builder()
            .name(name)
            .value(value)
            .domain(domain)
            .path(path)
            .apply { if (expiresAt > 0) expiresAt(expiresAt) }
            .apply { if (secure) secure() }
            .apply { if (httpOnly) httpOnly() }
            .build()
    }.getOrNull()

    companion object {
        fun from(c: Cookie): StoredCookie = StoredCookie(
            name = c.name,
            value = c.value,
            domain = c.domain,
            path = c.path,
            expiresAt = c.expiresAt,
            secure = c.secure,
            httpOnly = c.httpOnly,
        )
    }
}

class PreferencesCookieStorage(context: Context) : CookieStorage {

    private val prefs = context.applicationContext
        .getSharedPreferences("sharekt_cookies", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override fun read(): List<Cookie> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val stored = runCatching { json.decodeFromString<List<StoredCookie>>(raw) }
            .getOrDefault(emptyList())
        return stored.mapNotNull { it.toCookie() }
    }

    override fun write(cookies: List<Cookie>) {
        val out = json.encodeToString(cookies.map { StoredCookie.from(it) })
        prefs.edit().putString(KEY, out).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "cookies"
    }
}
