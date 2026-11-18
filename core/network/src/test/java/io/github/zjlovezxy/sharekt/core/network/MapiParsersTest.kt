package io.github.zjlovezxy.sharekt.core.network

import io.github.zjlovezxy.sharekt.core.model.Status
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapiParsersTest {

    private val json = Json { isLenient = true }

    private fun obj(raw: String) = json.parseToJsonElement(raw) as JsonObject

    @Test
    fun parseNormalStatus() {
        val status = MapiParsers.parseStatus(
            obj(
                """
                {"id": 501, "mid": "501", "created_at": "Wed Nov 18 20:00:00 +0800 2026",
                 "text": "你好 <a href='x'>@某人</a>", "source": "<a>iPhone</a>",
                 "comments_count": 3, "reposts_count": "7", "attitudes_count": 12,
                 "user": {"id": 9, "screen_name": "张三", "avatar_large": "http://a/b.jpg",
                          "verified": true, "followers_count": 1000}}
                """.trimIndent()
            )
        )
        assertEquals("501", status.id)
        assertEquals(3, status.commentsCount)
        assertEquals(7, status.repostsCount)
        assertEquals(12, status.likesCount)
        assertEquals("张三", status.user.screenName)
        assertTrue(status.user.verified)
        assertTrue(status.createdAt > 0)
        assertEquals("iPhone", status.source)
    }

    @Test
    fun parseMissingFields() {
        val status = MapiParsers.parseStatus(obj("""{"id": 1}"""))
        assertEquals("1", status.id)
        assertEquals(0L, status.createdAt)
        assertEquals("", status.user.screenName)
        assertEquals("转发微博", Status().displayText)
    }

    @Test
    fun parseNineGridImages() {
        val status = MapiParsers.parseStatus(
            obj(
                """
                {"id": 2, "text": "图",
                 "pic_infos": {"a": {"largest": {"url": "http://img/1.jpg"}},
                               "b": {"bmiddle": {"url": "http://img/2.jpg"}}}}
                """.trimIndent()
            )
        )
        assertEquals(listOf("http://img/1.jpg", "http://img/2.jpg"), status.imageUrls)
    }

    @Test
    fun parseRetweetOnlyOneLevel() {
        val status = MapiParsers.parseStatus(
            obj(
                """
                {"id": 3, "text": "转",
                 "retweeted_status": {"id": 31, "text": "原",
                   "retweeted_status": {"id": 32, "text": "更原"}}}
                """.trimIndent()
            )
        )
        assertNotNull(status.retweeted)
        assertEquals("原", status.retweeted?.text)
        assertEquals(null, status.retweeted?.retweeted)
    }

    @Test
    fun parseLongText() {
        val status = MapiParsers.parseStatus(
            obj("""{"id": 4, "text": "省略", "longText": {"longTextContent": "完整正文"}}""")
        )
        assertEquals("完整正文", status.text)
    }

    @Test
    fun parseTimeSecondsAndMillis() {
        val sec = MapiParsers.parseStatus(obj("""{"id":"5","created_at":"1750000000"}"""))
        val ms = MapiParsers.parseStatus(obj("""{"id":"6","created_at":"1750000000000"}"""))
        assertEquals(1750000000000L, sec.createdAt)
        assertEquals(1750000000000L, ms.createdAt)
    }

    @Test
    fun parseCommentsWithReply() {
        val comments = MapiParsers.parseComments(
            obj(
                """
                {"comments": [
                  {"id": 1, "text": "a", "user": {"screen_name": "u1"},
                   "reply_comment": {"id": 0, "text": "b", "user": {"screen_name": "u2"}}}
                ]}
                """.trimIndent()
            )
        )
        assertEquals(1, comments.size)
        assertEquals("u2", comments[0].replyTo?.user?.screenName)
    }
}
