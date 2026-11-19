package io.github.zjlovezxy.sharekt.core.network

import io.github.zjlovezxy.sharekt.core.common.AppLog
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * mapi 的最小客户端：拼 URL、挂 UA/Cookie、把非 JSON/HTTP 错误统一成 MapiException。
 * 不做重试——重试策略由调用方按场景决定（时间线可以退避，发微博绝不能自动重试）。
 */
class MapiClient(
    val jar: WeiboCookieJar? = null,
    private val sessionProvider: () -> Session = { Session() },
    baseUrl: String = ApiConfig.BASE_URL,
    private val userAgent: String = ApiConfig.USER_AGENT,
    client: OkHttpClient = defaultClient(),
) {

    private val base = baseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val http: OkHttpClient = if (jar != null) {
        client.newBuilder().cookieJar(jar).build()
    } else {
        client
    }

    suspend fun getJson(path: String, params: Map<String, String> = emptyMap()): JsonObject =
        execute(Request.Builder().url(buildUrl(path, params)).header("User-Agent", userAgent).build())

    /** 给 h5 端（m.weibo.cn）用；cookie 由调用方显式给，不经过 mapi 的 jar。 */
    suspend fun getAbsoluteJson(url: String, cookieHeader: String = ""): JsonObject {
        val req = Request.Builder().url(url.toHttpUrl()).header("User-Agent", userAgent)
            .apply { if (cookieHeader.isNotEmpty()) header("Cookie", cookieHeader) }
            .build()
        return execute(req)
    }

    private suspend fun execute(req: Request): JsonObject = withContext(Dispatchers.IO) {
        AppLog.d(TAG, "GET ${req.url}")
        val text = try {
            http.newCall(req).execute().use { r ->
                val body = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw MapiException(r.code, "HTTP ${r.code} ${r.message}")
                body
            }
        } catch (e: MapiException) {
            throw e
        } catch (e: IOException) {
            AppLog.w(TAG, "网络失败 ${req.url}: ${e.javaClass.simpleName}: ${e.message}")
            throw MapiException(MapiException.CODE_NETWORK, "网络错误：${e.message}", e)
        }
        val element = runCatching { json.parseToJsonElement(text) }.getOrElse {
            throw MapiException(MapiException.CODE_PARSE, "响应不是合法 JSON")
        }
        element as? JsonObject
            ?: throw MapiException(MapiException.CODE_PARSE, "响应不是 JSON 对象")
    }

    private fun buildUrl(path: String, params: Map<String, String>): HttpUrl {
        val url = "$base/${path.trimStart('/')}".toHttpUrl().newBuilder()
        val session = sessionProvider()
        url.addQueryParameter(ApiConfig.PARAM_FROM, ApiConfig.FROM)
        url.addQueryParameter(ApiConfig.PARAM_LANG, "zh_CN")
        if (session.gsid.isNotEmpty()) url.addQueryParameter(ApiConfig.PARAM_GSID, session.gsid)
        params.forEach { (k, v) -> if (v.isNotEmpty()) url.addQueryParameter(k, v) }
        return url.build()
    }

    private companion object {
        const val TAG = "MapiClient"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
