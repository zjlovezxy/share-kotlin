package io.github.zjlovezxy.sharekt.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** h5（m.weibo.cn）返回体的解析用例，全部用内联 JSON，不依赖网络。 */
class H5ParsersTest {

    private val json = Json { isLenient = true }

    private fun obj(raw: String) = json.parseToJsonElement(raw) as JsonObject

    @Test
    fun cardsFlattenSkipsCardsWithoutMblog() {
        val data = obj(
            """
            {"cards": [
              {"card_type": 9, "itemid": "ad-1", "card_group": []},
              {"card_type": 9, "mblog": {"id": "1", "text": "第一条", "user": {"id": "9", "screen_name": "张三"}}},
              {"card_type": 11, "card_group": [
                 {"mblog": {"id": "2", "text": "分组里的", "user": {"id": "8", "screen_name": "李四"}}}
              ]},
              {"card_type": 9, "mblog": {"id": "1", "text": "重复"}}
            ]}
            """,
        )
        val statuses = H5Parsers.statusesFromCards(data)
        assertEquals(2, statuses.size)
        assertEquals(listOf("1", "2"), statuses.map { it.id })
        assertEquals("第一条", statuses[0].text)
        assertEquals("李四", statuses[1].user.screenName)
    }

    @Test
    fun timelineReturnsNextCursor() {
        val data = obj(
            """
            {"cards": [{"mblog": {"id": "1", "text": "x"}}], "cardlistInfo": {"since_id": 42}}
            """,
        )
        val (items, next) = H5Parsers.timeline(data)
        assertEquals(1, items.size)
        assertEquals("42", next)
    }

    @Test
    fun mblogFields() {
        val status = H5Parsers.parseMblog(
            obj(
                """
                {"id": 5366707153537849, "mid": "5366707153537849", "bid": "RrZmbeQm5",
                 "text": "正文 <a href='/n/某人'>@某人</a>",
                 "created_at": "Wed Nov 18 20:49:17 +0800 2026",
                 "source": "<a href='x'>微博视频号</a>",
                 "comments_count": 146, "reposts_count": "93", "attitudes_count": 2365,
                 "isLongText": false,
                 "user": {"id": 6992976169, "screen_name": "某人", "profile_image_url": "https://a/b.jpg",
                          "verified": true, "followers_count": 1200, "follow_count": 30},
                 "pics": [
                   {"pid": "p1", "url": "https://w/orj360/p1.jpg",
                    "large": {"url": "https://w/mw2000/p1.jpg"}},
                   {"pid": "p2", "url": "https://w/orj360/p2.jpg"}
                 ],
                 "page_info": {"type": "video", "media_info": {"stream_url_hd": "https://v/hd.mp4", "stream_url": "https://v/sd.mp4"}}
                }
                """,
            ),
        )
        assertEquals("5366707153537849", status.id)
        assertEquals(2365, status.likesCount)
        assertEquals(93, status.repostsCount)
        assertEquals("微博视频号", status.source)
        assertEquals("某人", status.user.screenName)
        assertTrue(status.user.verified)
        assertEquals(1200L, status.user.followersCount)
        assertEquals(listOf("https://w/mw2000/p1.jpg", "https://w/orj360/p2.jpg"), status.imageUrls)
        assertEquals("https://v/hd.mp4", status.videoUrl)
        assertTrue(status.createdAt > 1_700_000_000_000L)
        assertNull(status.retweeted)
    }

    @Test
    fun picIdsFallbackBuildsUrls() {
        val status = H5Parsers.parseMblog(
            obj("""{"id": "7", "pic_ids": ["008cJClfgy1ij79jh1d9uj30xb15ln8f"], "pic_num": 1}"""),
        )
        assertEquals(
            listOf("https://wx1.sinaimg.cn/large/008cJClfgy1ij79jh1d9uj30xb15ln8f.jpg"),
            status.imageUrls,
        )
    }

    @Test
    fun retweetParsedOneLevelOnly() {
        val status = H5Parsers.parseMblog(
            obj(
                """
                {"id": "1", "text": "转发微博",
                 "retweeted_status": {"id": "2", "text": "原文",
                   "retweeted_status": {"id": "3", "text": "更深一层"}}}
                """,
            ),
        )
        assertEquals("2", status.retweeted?.id)
        assertEquals("原文", status.retweeted?.text)
        assertNull(status.retweeted?.retweeted)
    }

    @Test
    fun commentsPageAndCursor() {
        val page = H5Parsers.parseComments(
            obj(
                """
                {"ok": 1, "data": {"max_id": 220256973229, "max_id_type": 0,
                  "data": [
                    {"id": "c1", "text": "评论一", "like_count": 3,
                     "created_at": "Wed Nov 18 22:01:50 +0800 2026",
                     "user": {"id": "5", "screen_name": "沈胖子520"}},
                    {"id": "c2", "text": "评论二",
                     "reply_comment": {"id": "c0", "text": "被回复的", "user": {"id": "6", "screen_name": "甲"}}}
                  ]}}
                """,
            ),
        )
        assertEquals(2, page.items.size)
        assertEquals("220256973229", page.maxId)
        assertEquals("沈胖子520", page.items[0].user.screenName)
        assertEquals("被回复的", page.items[1].replyTo?.text)
        assertEquals("甲", page.items[1].replyTo?.user?.screenName)
    }

    @Test
    fun commentsWithoutDataIsEmpty() {
        val page = H5Parsers.parseComments(obj("""{"ok":0,"msg":"这里还没有内容"}"""))
        assertTrue(page.items.isEmpty())
        assertEquals("", page.maxId)
    }

    @Test
    fun relativeTimes() {
        val now = System.currentTimeMillis()
        assertTrue(H5Parsers.parseTime("刚刚") in (now - 5_000)..(now + 5_000))
        assertTrue(H5Parsers.parseTime("3分钟前") in (now - 3 * 60_000 - 5_000)..(now - 3 * 60_000 + 5_000))
        assertTrue(H5Parsers.parseTime("2小时前") in (now - 2 * 3_600_000 - 5_000)..(now - 2 * 3_600_000 + 5_000))
    }

    @Test
    fun secondsAndMillisTimestamps() {
        assertEquals(1_750_000_000_000L, H5Parsers.parseTime("1750000000"))
        assertEquals(1_750_000_000_000L, H5Parsers.parseTime("1750000000000"))
        assertEquals(0L, H5Parsers.parseTime("这不是时间"))
    }
}
