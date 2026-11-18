package io.github.zjlovezxy.sharekt.core.network

import io.github.zjlovezxy.sharekt.core.model.Comment
import io.github.zjlovezxy.sharekt.core.model.Status
import io.github.zjlovezxy.sharekt.core.model.WeiboUser
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.serialization.json.JsonObject

/**
 * mapi JSON → 领域对象。原则：
 * 1. 任何字段缺失/类型不对都降级为默认值；
 * 2. 单条解析失败返回 null，由上层过滤，不影响同页其他条目；
 * 3. 转发链只解析一层，防止恶意/异常数据递归爆炸。
 */
object MapiParsers {

    private const val MAX_DEPTH = 1

    private val timeFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.US)

    fun parseStatuses(o: JsonObject): List<Status> {
        val list = o.arrayOf("statuses").ifEmpty { o.arrayOf("status") }
            .ifEmpty { o.arrayOf("data") }
        return list.mapNotNull { runCatching { parseStatus(it) }.getOrNull() }
    }

    fun parseStatus(o: JsonObject, depth: Int = 0): Status {
        val retweeted = if (depth < MAX_DEPTH) o.obj("retweeted_status")?.let {
            runCatching { parseStatus(it, depth + 1) }.getOrNull()
        } else null
        return Status(
            id = o.str("id").ifEmpty { o.str("idstr") },
            mid = o.str("mid"),
            text = longTextOr(o),
            createdAt = parseTime(o.str("created_at")),
            source = stripHtml(o.str("source")),
            user = parseUser(o.obj("user")),
            retweeted = retweeted,
            imageUrls = parseImages(o),
            videoUrl = parseVideoUrl(o),
            commentsCount = o.int("comments_count"),
            repostsCount = o.int("reposts_count"),
            likesCount = o.int("attitudes_count"),
            isLongText = o.bool("isLongText") || o.bool("is_long_text"),
            cardType = o.int("card_type"),
        )
    }

    fun parseComments(o: JsonObject): List<Comment> {
        val list = o.arrayOf("comments").ifEmpty { o.arrayOf("data") }
        return list.mapNotNull { runCatching { parseComment(it) }.getOrNull() }
    }

    fun parseComment(o: JsonObject): Comment = Comment(
        id = o.str("id").ifEmpty { o.str("idstr") },
        text = longTextOr(o),
        createdAt = parseTime(o.str("created_at")),
        user = parseUser(o.obj("user")),
        replyTo = o.obj("reply_comment")?.let { runCatching { parseComment(it) }.getOrNull() },
        likesCount = o.int("like_counts").let { if (it != 0) it else o.int("likes_count") },
        source = stripHtml(o.str("source")),
    )

    fun parseUser(o: JsonObject?): WeiboUser {
        if (o == null) return WeiboUser()
        return WeiboUser(
            id = o.str("id").ifEmpty { o.str("idstr") },
            screenName = o.str("screen_name").ifEmpty { o.str("name") },
            avatarUrl = o.str("avatar_large").ifEmpty { o.str("profile_image_url") },
            verified = o.bool("verified") || o.int("verified_type") > 0,
            verifiedReason = o.str("verified_reason"),
            followersCount = o.long("followers_count"),
            friendsCount = o.long("friends_count"),
            statusesCount = o.long("statuses_count"),
            description = o.str("description"),
            coverUrl = o.str("cover_image_phone"),
            following = o.bool("following"),
        )
    }

    /** 长文的完整正文在 longText.longTextContent 里，text 只是省略版。 */
    private fun longTextOr(o: JsonObject): String {
        val full = o.obj("longText")?.str("longTextContent").orEmpty()
        if (full.isNotEmpty()) return full
        return o.str("text")
    }

    private fun parseImages(o: JsonObject): List<String> {
        val infos = o.obj("pic_infos")
        if (infos != null) {
            val urls = infos.keys.mapNotNull { key ->
                val info = infos.obj(key) ?: return@mapNotNull null
                val url = info.obj("largest")?.str("url").orEmpty()
                    .ifEmpty { info.obj("bmiddle")?.str("url").orEmpty() }
                    .ifEmpty { info.obj("thumbnail")?.str("url").orEmpty() }
                url.ifEmpty { null }
            }
            if (urls.isNotEmpty()) return urls
        }
        val single = o.str("bmiddle_pic").ifEmpty { o.str("thumbnail_pic") }
        return if (single.isEmpty()) emptyList() else listOf(single)
    }

    private fun parseVideoUrl(o: JsonObject): String {
        val page = o.obj("page_info") ?: return ""
        if (page.str("type").isEmpty() && page.str("object_type").isEmpty()) return ""
        val media = page.obj("media_info") ?: return page.str("page_url")
        return media.str("stream_url_hd").ifEmpty { media.str("stream_url") }
            .ifEmpty { page.str("page_url") }
    }

    private fun stripHtml(s: String): String =
        if (s.isEmpty()) "" else s.replace(HTML_TAG, "").trim()

    private fun parseTime(raw: String): Long {
        if (raw.isEmpty()) return 0L
        raw.toLongOrNull()?.let { num ->
            // 10 位数是秒，13 位是毫秒；mapi 两种都给过
            return if (num < 100_000_000_000L) num * 1000 else num
        }
        return runCatching { timeFormat.parse(raw)?.time ?: 0L }.getOrDefault(0L)
    }

    private val HTML_TAG = Regex("<[^>]*>")
}
