package io.github.zjlovezxy.sharekt.core.data

/** 离线缓存策略：时间线 3 天，详情 7 天。 */
object CachePolicy {
    const val TIMELINE_TTL_MS = 3L * 24 * 60 * 60 * 1000
    const val DETAIL_TTL_MS = 7L * 24 * 60 * 60 * 1000
}
