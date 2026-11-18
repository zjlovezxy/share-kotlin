package io.github.zjlovezxy.sharekt.core.network

import io.github.zjlovezxy.sharekt.core.common.AppLog
import io.github.zjlovezxy.sharekt.core.model.Comment
import io.github.zjlovezxy.sharekt.core.model.Page
import io.github.zjlovezxy.sharekt.core.model.Status
import io.github.zjlovezxy.sharekt.core.model.WeiboUser
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** mapi 的业务语义层：把 JSON 变成 Page&lt;Status&gt; / Page&lt;Comment&gt;，并统一错误。 */
class WeiboApi(private val client: MapiClient) {

    suspend fun friendsTimeline(
        sinceId: String = "",
        maxId: String = "",
        count: Int = PAGE_SIZE,
        page: Int = 1,
    ): Page<Status> = pageOf(
        client.getJson(
            ApiConfig.EP_FRIENDS_TIMELINE,
            params(sinceId, maxId, count, page),
        ),
        count,
    )

    suspend fun hotTimeline(
        sinceId: String = "",
        maxId: String = "",
        count: Int = PAGE_SIZE,
        page: Int = 1,
    ): Page<Status> = pageOf(
        client.getJson(ApiConfig.EP_HOT_TIMELINE, params(sinceId, maxId, count, page)),
        count,
    )

    suspend fun userTimeline(
        uid: String,
        sinceId: String = "",
        maxId: String = "",
        count: Int = PAGE_SIZE,
        page: Int = 1,
    ): Page<Status> = pageOf(
        client.getJson(
            ApiConfig.EP_USER_TIMELINE,
            params(sinceId, maxId, count, page) + ("uid" to uid) + ("feature" to "0"),
        ),
        count,
    )

    suspend fun statusById(id: String): Status? {
        val obj = client.getJson(ApiConfig.EP_STATUS_SHOW_BATCH, mapOf("ids" to id))
        obj.ensureOk()
        return MapiParsers.parseStatuses(obj).firstOrNull()
    }

    suspend fun comments(
        statusId: String,
        page: Int = 1,
        count: Int = PAGE_SIZE,
    ): Page<Comment> {
        val obj = client.getJson(
            ApiConfig.EP_COMMENTS_SHOW,
            mapOf(
                "id" to statusId,
                "page" to page.toString(),
                "count" to count.toString(),
                "is_repost" to "0",
            ),
        )
        obj.ensureOk()
        val items = MapiParsers.parseComments(obj)
        return Page(
            items = items,
            maxId = if (items.size < count) "" else (page + 1).toString(),
            total = obj.int("total_number"),
        )
    }

    suspend fun userShow(uids: String): List<WeiboUser> {
        val obj = client.getJson(ApiConfig.EP_USERS_SHOW_BATCH, mapOf("uids" to uids))
        obj.ensureOk()
        return obj.arrayOf("users").map { MapiParsers.parseUser(it) }
    }

    suspend fun searchStatuses(
        query: String,
        page: Int = 1,
        count: Int = PAGE_SIZE,
    ): Page<Status> {
        val obj = client.getJson(
            ApiConfig.EP_SEARCH_STATUSES,
            mapOf("q" to query, "page" to page.toString(), "count" to count.toString()),
        )
        obj.ensureOk()
        return pageOf(obj, count)
    }

    suspend fun unreadCount(): Int {
        val obj = client.getJson(ApiConfig.EP_UNREAD_COUNT, mapOf("unread_message" to "1"))
        obj.ensureOk()
        return obj.int("message")
    }

    private fun params(sinceId: String, maxId: String, count: Int, page: Int) = buildMap {
        if (sinceId.isNotEmpty()) put(ApiConfig.PARAM_CURSOR, sinceId)
        put("max_id", maxId)
        put(ApiConfig.PARAM_PAGE_SIZE, count.toString())
        put("page", page.toString())
        put("is_showbulk" to "1")
    }

    private fun pageOf(obj: JsonObject, count: Int): Page<Status> {
        obj.ensureOk()
        val items = MapiParsers.parseStatuses(obj)
        val cursor = obj.str("max_id").ifEmpty { items.lastOrNull()?.id.orEmpty() }
        return Page(
            items = items,
            sinceId = obj.str("since_id"),
            maxId = if (items.size < count) "" else cursor,
            total = obj.int("total_number"),
        )
    }

    /** ok=0 才当错误；某些成功响应根本不带 ok，别误伤。 */
    private fun JsonObject.ensureOk() {
        val raw = this["ok"] as? JsonPrimitive ?: return
        val okValue = raw.booleanOrNull?.let { if (it) 1 else 0 } ?: raw.intOrNull ?: 1
        if (okValue == 0) {
            val code = int("code").let { if (it != 0) it else -1 }
            val msg = str("msg").ifEmpty { str("message") }
            AppLog.w("WeiboApi", "ok=0 code=$code msg=$msg")
            throw MapiException(code, msg.ifEmpty { "请求被拒绝（ok=0）" })
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
