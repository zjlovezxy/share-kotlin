package io.github.zjlovezxy.sharekt.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String = "",
    val text: String = "",
    val createdAt: Long = 0L,
    val user: WeiboUser = WeiboUser(),
    val replyTo: Comment? = null,
    val likesCount: Int = 0,
    val source: String = "",
) {
    val displayText: String get() = text.ifBlank { "[图片或表情]" }
}
