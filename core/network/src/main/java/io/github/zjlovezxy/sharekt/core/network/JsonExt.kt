package io.github.zjlovezxy.sharekt.core.network

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * mapi 的数值字段有时是数字、有时是字符串（"12" / 12），
 * 这里的扩展全部按"尽力而为"取值，绝不抛异常。
 */
fun JsonObject.str(key: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull ?: ""

fun JsonObject.str(key: String, def: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull ?: def

fun JsonObject.long(key: String): Long {
    val p = this[key] as? JsonPrimitive ?: return 0L
    return p.contentOrNull?.trim()?.toLongOrNull() ?: p.longOrNull ?: 0L
}

fun JsonObject.int(key: String): Int {
    val p = this[key] as? JsonPrimitive ?: return 0
    return p.contentOrNull?.trim()?.toIntOrNull() ?: p.intOrNull ?: 0
}

fun JsonObject.bool(key: String): Boolean {
    val p = this[key] as? JsonPrimitive ?: return false
    return when (p.contentOrNull?.trim()?.lowercase()) {
        "true", "1", "yes" -> true
        else -> false
    }
}

fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray

fun JsonObject.objOrEmpty(key: String): JsonObject =
    obj(key) ?: JsonObject(emptyMap())

fun JsonObject.arrayOf(key: String): List<JsonObject> =
    arr(key)?.mapNotNull { it as? JsonObject } ?: emptyList()
