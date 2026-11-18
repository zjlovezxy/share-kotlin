package io.github.zjlovezxy.sharekt.core.data

import io.github.zjlovezxy.sharekt.core.common.AppLog
import io.github.zjlovezxy.sharekt.core.model.Comment
import io.github.zjlovezxy.sharekt.core.model.Page
import io.github.zjlovezxy.sharekt.core.model.Status
import io.github.zjlovezxy.sharekt.core.model.WeiboUser
import io.github.zjlovezxy.sharekt.core.network.MapiException
import io.github.zjlovezxy.sharekt.core.network.WeiboApi
import kotlinx.serialization.json.Json

/**
 * 数据入口。策略：
 * - 第一页优先读缓存（3 天内），下拉刷新才强制走网络；
 * - 网络失败时回落到任意旧缓存，保证"断网也能看昨天的时间线"；
 * - 详情缓存 7 天。
 */
class WeiboRepository(
    private val api: WeiboApi,
    private val cache: JsonCache,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun friendsTimeline(fresh: Boolean = false, maxId: String = "", page: Int = 1): Page<Status> =
        loadStatusPage(KEY_FRIENDS, CachePolicy.TIMELINE_TTL_MS, fresh, firstPage(maxId, page)) {
            api.friendsTimeline(maxId = maxId, page = page)
        }

    suspend fun hotTimeline(fresh: Boolean = false, maxId: String = "", page: Int = 1): Page<Status> =
        loadStatusPage(KEY_HOT, CachePolicy.TIMELINE_TTL_MS, fresh, firstPage(maxId, page)) {
            api.hotTimeline(maxId = maxId, page = page)
        }

    suspend fun userTimeline(uid: String, fresh: Boolean = false, maxId: String = "", page: Int = 1): Page<Status> =
        loadStatusPage("${KEY_USER}_$uid", CachePolicy.TIMELINE_TTL_MS, fresh, firstPage(maxId, page)) {
            api.userTimeline(uid = uid, maxId = maxId, page = page)
        }

    suspend fun status(id: String, fresh: Boolean = false): Status? {
        val key = "${KEY_STATUS}_$id"
        if (!fresh) {
            cache.get(key, CachePolicy.DETAIL_TTL_MS)?.let { raw ->
                runCatching { json.decodeFromString(Status.serializer(), raw) }.getOrNull()?.let { return it }
            }
        }
        val status = api.statusById(id) ?: return null
        runCatching { cache.put(key, json.encodeToString(Status.serializer(), status)) }
        return status
    }

    suspend fun comments(statusId: String, page: Int = 1): Page<Comment> =
        api.comments(statusId, page = page)

    suspend fun user(uid: String, fresh: Boolean = false): WeiboUser? {
        val key = "${KEY_USER_INFO}_$uid"
        if (!fresh) {
            cache.get(key, CachePolicy.DETAIL_TTL_MS)?.let { raw ->
                runCatching { json.decodeFromString(WeiboUser.serializer(), raw) }.getOrNull()?.let { return it }
            }
        }
        val user = api.userShow(uid).firstOrNull() ?: return null
        runCatching { cache.put(key, json.encodeToString(WeiboUser.serializer(), user)) }
        return user
    }

    suspend fun search(query: String, page: Int = 1): Page<Status> = api.searchStatuses(query, page)

    fun clearCache() = cache.clear()

    private fun firstPage(maxId: String, page: Int) = maxId.isEmpty() && page == 1

    private suspend fun loadStatusPage(
        key: String,
        ttl: Long,
        fresh: Boolean,
        cacheFirst: Boolean,
        fetch: suspend () -> Page<Status>,
    ): Page<Status> {
        if (!fresh && cacheFirst) {
            decodePage(key, ttl)?.let { return it }
        }
        return try {
            val page = fetch()
            if (cacheFirst) encodePage(key, page)
            page
        } catch (e: MapiException) {
            val stale = if (cacheFirst) decodePage(key, Long.MAX_VALUE) else null
            if (stale != null) {
                AppLog.w(TAG, "网络失败，回落缓存 key=$key（${e.message}）")
                stale
            } else {
                throw e
            }
        }
    }

    private fun encodePage(key: String, page: Page<Status>) {
        runCatching {
            cache.put(key, json.encodeToString(Page.serializer(Status.serializer()), page))
        }
    }

    private fun decodePage(key: String, ttl: Long): Page<Status>? =
        cache.get(key, ttl)?.let { raw ->
            runCatching { json.decodeFromString(Page.serializer(Status.serializer()), raw) }.getOrNull()
        }

    private companion object {
        const val TAG = "WeiboRepository"
        const val KEY_FRIENDS = "timeline_friends"
        const val KEY_HOT = "timeline_hot"
        const val KEY_USER = "timeline_user"
        const val KEY_STATUS = "status"
        const val KEY_USER_INFO = "user"
    }
}
