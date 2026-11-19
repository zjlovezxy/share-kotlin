package io.github.zjlovezxy.sharekt.core.network

import io.github.zjlovezxy.sharekt.core.common.AppLog
import io.github.zjlovezxy.sharekt.core.model.Comment
import io.github.zjlovezxy.sharekt.core.model.Page
import io.github.zjlovezxy.sharekt.core.model.Status
import io.github.zjlovezxy.sharekt.core.model.WeiboUser
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * 业务语义层：h5 接口的 JSON -> Page&lt;Status&gt; / Page&lt;Comment&gt;，并统一错误。
 *
 * 分页一律用游标：微博流是 `cardlistInfo.since_id`，评论是 `max_id`；
 * 搜索是唯一的页码分页（page_type=searchall）。
 */
class WeiboApi(private val client: MapiClient) {

    suspend fun friendsTimeline(
        sinceId: String = "",
        maxId: String = "",
        count: Int = PAGE_SIZE,
        page: Int = 1,
    ): Page<Status> = timeline(ApiConfig.H5_CONTAINER_RECOMMEND, sinceId.ifEmpty { maxId })

    suspend fun hotTimeline(
        sinceId: String = "",
        maxId: String = "",
        count: Int = PAGE_SIZE,
        page: Int = 1,
    ): Page<Status> = timeline(ApiConfig.H5_CONTAINER_RECOMMEND, sinceId.ifEmpty { maxId })

    suspend fun userTimeline(
        uid: String,
        sinceId: String = "",
        maxId: String = "",
        count: Int = PAGE_SIZE,
        page: Int = 1,
    ): Page<Status> = timeline("${ApiConfig.H5_CONTAINER_PROFILE}$uid", sinceId.ifEmpty { maxId })

    private suspend fun timeline(containerId: String, cursor: String): Page<Status> {
        val obj = client.getUrl(
            ApiConfig.H5_INDEX_URL,
            ApiConfig.H5_HEADERS,
            buildMap {
                put("containerid", containerId)
                if (cursor.isNotEmpty()) put("since_id", cursor)
            },
        )
        obj.ensureOk()
        val data = obj.obj("data") ?: return Page(items = emptyList(), maxId = "")
        val (items, next) = H5Parsers.timeline(data)
        return Page(items = items, sinceId = next, maxId = next)
    }

    suspend fun statusById(id: String): Status? {
        val obj = client.getUrl(ApiConfig.H5_SHOW_URL, ApiConfig.H5_HEADERS, mapOf("id" to id))
        obj.ensureOk()
        val mblog = obj.obj("data") ?: return null
        val status = H5Parsers.parseMblog(mblog)
        if (!status.isLongText) return status
        val full = longText(status.id)
        return if (full.isEmpty()) status else status.copy(text = full)
    }

    /** 长文：正文被截断时用 statuses/extend 换全文。 */
    private suspend fun longText(id: String): String = runCatching {
        val obj = client.getUrl(ApiConfig.H5_EXTEND_URL, ApiConfig.H5_HEADERS, mapOf("id" to id))
        obj.str("longTextContent").ifEmpty { obj.obj("data")?.str("longTextContent").orEmpty() }
    }.getOrDefault("")

    suspend fun comments(
        statusId: String,
        maxId: Long = 0L,
        count: Int = PAGE_SIZE,
    ): Page<Comment> {
        val obj = client.getUrl(
            ApiConfig.H5_COMMENTS_URL,
            ApiConfig.H5_HEADERS,
            buildMap {
                put("id", statusId)
                put("mid", statusId)
                put("max_id_type", "0")
                if (maxId > 0L) put("max_id", maxId.toString())
            },
        )
        obj.ensureOk()
        return H5Parsers.parseComments(obj)
    }

    /** 单个用户资料：h5 的 container/getIndex?type=uid&value=… 会带 data.userInfo。 */
    suspend fun userShow(uids: String): List<WeiboUser> {
        val uid = uids.substringBefore(',').trim()
        if (uid.isEmpty()) return emptyList()
        val obj = client.getUrl(
            ApiConfig.H5_INDEX_URL,
            ApiConfig.H5_HEADERS,
            mapOf("type" to "uid", "value" to uid),
        )
        obj.ensureOk()
        val info = obj.obj("data")?.obj("userInfo") ?: return emptyList()
        return listOf(H5Parsers.parseUser(info))
    }

    suspend fun searchStatuses(
        query: String,
        page: Int = 1,
        count: Int = PAGE_SIZE,
    ): Page<Status> {
        if (query.isBlank()) return Page(items = emptyList(), maxId = "")
        val obj = client.getUrl(
            ApiConfig.H5_INDEX_URL,
            ApiConfig.H5_HEADERS,
            mapOf(
                "containerid" to "100103type=1&q=$query",
                "page_type" to "searchall",
                "page" to page.toString(),
            ),
        )
        obj.ensureOk()
        val data = obj.obj("data") ?: return Page(items = emptyList(), maxId = "")
        val items = H5Parsers.statusesFromCards(data)
        return Page(items = items, maxId = if (items.isEmpty()) "" else (page + 1).toString())
    }

    /**
     * h5 有两种错误形态：
     * 1) {ok:0, msg:…}  常规接口错误
     * 2) {errno:-100, errmsg:…}  未登录 / 风控
     * 两种都转成 MapiException，否则未登录会被静默渲染成空列表。
     */
    private fun JsonObject.ensureOk() {
        val okPrim = this["ok"] as? JsonPrimitive
        if (okPrim != null) {
            val okValue = okPrim.booleanOrNull?.let { if (it) 1 else 0 } ?: okPrim.intOrNull ?: 1
            if (okValue == 0) {
                val code = int("code").let { if (it != 0) it else -1 }
                val msg = str("msg").ifEmpty { str("message") }
                AppLog.w(TAG, "ok=0 code=$code msg=$msg")
                throw MapiException(code, msg.ifEmpty { "请求被拒绝（ok=0）" })
            }
            return
        }
        val errno = int("errno")
        if (errno != 0) {
            val msg = str("errmsg").ifEmpty { str("msg") }
            AppLog.w(TAG, "errno=$errno msg=$msg")
            throw MapiException(errno, msg.ifEmpty { "请求失败（$errno）" })
        }
    }

    companion object {
        const val PAGE_SIZE = 20
        private const val TAG = "WeiboApi"
    }
}
