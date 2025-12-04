package com.ankideku.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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

fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is JsonElement -> this
    is Iterable<*> -> JsonArray(map { it.toJsonElement() })
    is Array<*> -> JsonArray(map { it.toJsonElement() })
    is Map<*, *> -> JsonObject(entries.associate { (key, value) -> key.toString() to value.toJsonElement() })
    else -> throw IllegalArgumentException("Unsupported type for JSON conversion: ${this::class.simpleName}")
}
