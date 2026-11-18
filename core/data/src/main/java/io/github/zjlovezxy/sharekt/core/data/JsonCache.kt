package io.github.zjlovezxy.sharekt.core.data

import java.io.File

/**
 * 极简磁盘缓存：一个 key 一个文件，用文件修改时间当 TTL。
 * 不用 Room —— 这里只需要"整页 JSON 存/取"，上数据库纯属自找麻烦。
 */
class JsonCache(private val dir: File) {

    private val lock = Any()

    fun put(key: String, value: String) {
        synchronized(lock) {
            runCatching {
                if (!dir.exists()) dir.mkdirs()
                val tmp = File(dir, "$key.tmp")
                tmp.writeText(value)
                val target = File(dir, key)
                if (target.exists()) target.delete()
                tmp.renameTo(target)
            }
        }
    }

    fun get(key: String, ttlMs: Long): String? {
        synchronized(lock) {
            val file = File(dir, key)
            if (!file.isFile) return null
            if (ttlMs in 1..(System.currentTimeMillis() - file.lastModified())) return null
            return runCatching { file.readText() }.getOrNull()
        }
    }

    fun remove(key: String) {
        synchronized(lock) { File(dir, key).delete() }
    }

    fun clear() {
        synchronized(lock) { dir.listFiles()?.forEach { it.delete() } }
    }
}
