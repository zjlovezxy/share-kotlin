package io.github.zjlovezxy.sharekt.core.network

import io.github.zjlovezxy.sharekt.core.model.Comment
import io.github.zjlovezxy.sharekt.core.model.Page
import io.github.zjlovezxy.sharekt.core.model.Status
import io.github.zjlovezxy.sharekt.core.model.WeiboUser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * m.weibo.cn（h5）解析层。
 *
 * 与 mapi 的差异：
 * - 图片在 `pics[].large.url`（没有 pics 时用 `pic_ids` 拼 URL）
 * - 视频在 `page_info.media_info.stream_url(_hd)`
 * - `created_at` 是 "Wed Nov 18 20:49:17 +0800 2026" 或 "3分钟前"
 * - 卡片流是 `data.cards[].mblog`，广告卡只有 `card_group`
 */
object H5Parsers {

    private const val MAX_DEPTH = 1
    private const val PIC_BASE = "https://wx1.sinaimg.cn/large/"

    /** 卡片流拍平成一串微博；没有 mblog 的卡片（广告、分组头）直接跳过，绝不崩。 */
    fun statusesFromCards(data: JsonObject): List<Status> {
        val out = ArrayList<Status>()
        data.arrayOf("cards").forEach { card ->
            card.obj("mblog")?.let { out += parseMblog(it) }
            card.arrayOf("card_group").forEach { sub ->
                sub.obj("mblog")?.let { out += parseMblog(it) }
            }
        }
        return out.filter { it.id.isNotEmpty() }.distinctBy { it.id }
    }

    /** 返回 (微博列表, 下一页 since_id)。 */
    fun timeline(data: JsonObject): Pair<List<Status>, String> =
        statusesFromCards(data) to data.obj("cardlistInfo")?.str("since_id").orEmpty()

    fun parseMblog(m: JsonObject, depth: Int = 0): Status {
        val retweeted = if (depth < MAX_DEPTH) {
            m.obj("retweeted_status")?.let { parseMblog(it, depth + 1) }
        } else {
            null
        }
        return Status(
            id = m.str("id"),
            mid = m.str("mid").ifEmpty { m.str("id") },
            text = m.str("text"),
            createdAt = parseTime(m.str("created_at")),
            source = stripTags(m.str("source")),
            user = parseUser(m.objOrEmpty("user")),
            retweeted = retweeted,
            imageUrls = parseImages(m),
            videoUrl = parseVideo(m),
            commentsCount = m.int("comments_count"),
            repostsCount = m.int("reposts_count"),
            likesCount = m.int("attitudes_count"),
            isLongText = m.bool("isLongText"),
            cardType = m.int("card_type"),
        )
    }

    /** `data.data[]` + `data.max_id`；max_id == "0" 表示没有更多。 */
    fun parseComments(obj: JsonObject): Page<Comment> {
        val data = obj.obj("data") ?: return Page(items = emptyList(), maxId = "")
        val items = data.arrayOf("data").map { parseComment(it) }
        val next = data.str("max_id")
        return Page(
            items = items,
            maxId = if (next == "0") "" else next,
            total = data.int("total_number"),
        )
    }

    fun parseComment(c: JsonObject, depth: Int = 0): Comment = Comment(
        id = c.str("id"),
        text = c.str("text"),
        createdAt = parseTime(c.str("created_at")),
        user = parseUser(c.objOrEmpty("user")),
        replyTo = if (depth < MAX_DEPTH) c.obj("reply_comment")?.let { parseComment(it, depth + 1) } else null,
        likesCount = c.int("like_count"),
        source = stripTags(c.str("source")),
    )

    fun parseUser(u: JsonObject): WeiboUser = WeiboUser(
        id = u.str("id"),
        screenName = u.str("screen_name"),
        avatarUrl = u.str("profile_image_url").ifEmpty { u.str("avatar_large") },
        verified = u.bool("verified") || u.int("verified_type") > 0,
        verifiedReason = u.str("verified_reason"),
        followersCount = u.long("followers_count"),
        friendsCount = u.long("follow_count"),
        statusesCount = u.long("statuses_count"),
        description = u.str("description"),
        coverUrl = u.str("cover_image_phone"),
        following = u.bool("following"),
    )

    /** "Wed Nov 18 20:49:17 +0800 2026" / "3分钟前" / 秒或毫秒时间戳；解析不了就给 0（UI 显示"刚刚"）。 */
    fun parseTime(raw: String): Long {
        val s = raw.trim()
        if (s.isEmpty()) return 0L
        s.toLongOrNull()?.let { return if (it < 1_000_000_000_000L) it * 1000 else it }
        runCatching { SimpleDateFormat(WEB_DATE, Locale.US).parse(s)?.time }.getOrNull()?.let { return it }
        val now = System.currentTimeMillis()
        if (s == "刚刚") return now
        Regex("""^(\d+)\s*秒前""").find(s)?.let { return now - it.groupValues[1].toLong() * 1000 }
        Regex("""^(\d+)\s*分钟前""").find(s)?.let { return now - it.groupValues[1].toLong() * 60_000 }
        Regex("""^(\d+)\s*小时前""").find(s)?.let { return now - it.groupValues[1].toLong() * 3_600_000 }
        Regex("""^(\d{1,2})-(\d{1,2})$""").find(s)?.let { m ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MONTH, m.groupValues[1].toInt() - 1)
            cal.set(Calendar.DAY_OF_MONTH, m.groupValues[2].toInt())
            return cal.timeInMillis
        }
        return 0L
    }

    private fun parseImages(m: JsonObject): List<String> {
        val pics = m.arrayOf("pics").map { p ->
            p.obj("large")?.str("url").orEmpty().ifEmpty { p.str("url") }
        }.filter { it.isNotEmpty() }
        if (pics.isNotEmpty()) return pics.take(9)
        val ids = m.arr("pic_ids") ?: return emptyList()
        return ids.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .filter { it.isNotEmpty() }
            .take(9)
            .map { "$PIC_BASE$it.jpg" }
    }

    private fun parseVideo(m: JsonObject): String {
        val info = m.obj("page_info")?.obj("media_info") ?: return ""
        return info.str("stream_url_hd").ifEmpty { info.str("stream_url") }
    }

    private fun stripTags(s: String): String = s.replace(Regex("<[^>]*>"), "").trim()

    private const val WEB_DATE = "EEE MMM dd HH:mm:ss Z yyyy"
}
