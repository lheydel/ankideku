package com.ankideku.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

val json = Json {
    ignoreUnknownKeys = true          // equivalent to FAIL_ON_UNKNOWN_PROPERTIES = false
    isLenient = true                  // allows unquoted field names, trailing commas, etc.
    prettyPrint = false               // compact output
    encodeDefaults = true             // include default values in output
}

inline fun <reified T> String.parseJson(): T {
    return json.decodeFromString<T>(this)
}

inline fun <reified T> T.toJson(): String {
    return json.encodeToString(this)
}

fun Any?.serializeToJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is List<*> -> JsonArray(map { it.serializeToJsonElement() })
    is Map<*, *> -> buildJsonObject {
        forEach { (k, v) -> put(k.toString(), v.serializeToJsonElement()) }
    }
    else -> JsonPrimitive(toString())
}
