package io.github.zjlovezxy.sharekt.core.model

import kotlinx.serialization.Serializable

/** 游标分页结果。maxId 为空表示没有更多。 */
@Serializable
data class Page<T>(
    val items: List<T> = emptyList(),
    val sinceId: String = "",
    val maxId: String = "",
    val total: Int = 0,
) {
    val hasMore: Boolean get() = maxId.isNotEmpty()
}
