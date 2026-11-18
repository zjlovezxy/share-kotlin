package io.github.zjlovezxy.sharekt.core.model

import kotlinx.serialization.Serializable

/**
 * 一条微博。所有字段都有默认值——mapi 的返回是"随时可能少字段"的，
 * 缺字段必须降级成空值，不能抛异常（一条坏数据不能拖垮整页）。
 */
@Serializable
data class Status(
    val id: String = "",
    val mid: String = "",
    val text: String = "",
    /** epoch 毫秒；解析失败为 0，UI 侧显示"刚刚"而不是崩溃。 */
    val createdAt: Long = 0L,
    val source: String = "",
    val user: WeiboUser = WeiboUser(),
    val retweeted: Status? = null,
    val imageUrls: List<String> = emptyList(),
    val videoUrl: String = "",
    val commentsCount: Int = 0,
    val repostsCount: Int = 0,
    val likesCount: Int = 0,
    val isLongText: Boolean = false,
    val cardType: Int = 0,
) {
    /** 转发时正文为空很常见，用占位符保持卡片高度稳定。 */
    val displayText: String
        get() = text.ifBlank { "转发微博" }
}
