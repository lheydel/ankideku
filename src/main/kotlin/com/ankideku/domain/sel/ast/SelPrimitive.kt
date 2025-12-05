package com.ankideku.domain.sel.ast

/**
 * Sealed interface for primitive values (string, number, boolean, null).
 */
sealed interface SelPrimitive<T> : SelNode {
    val value: T
}

/**
 * String primitive value.
 */
data class SelString(override val value: String) : SelPrimitive<String> {
    override fun toJson(): String = "\"${value.escapeJson()}\""
}

/**
 * Number primitive value (supports both integers and floating point).
 */
data class SelNumber(override val value: Number) : SelPrimitive<Number> {
    fun toDouble(): Double = value.toDouble()
    fun toLong(): Long = value.toLong()
    fun toInt(): Int = value.toInt()

    override fun toJson(): String = when {
        value is Double || value is Float -> value.toDouble().let {
            if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
        }
        else -> value.toString()
    }
}

/**
 * Boolean primitive value.
 */
data class SelBoolean(override val value: Boolean) : SelPrimitive<Boolean> {
    companion object {
        val TRUE = SelBoolean(true)
        val FALSE = SelBoolean(false)
    }

    override fun toJson(): String = value.toString()
}

/**
 * Escapes special characters for JSON string values.
 */
private fun String.escapeJson(): String = buildString {
    for (c in this@escapeJson) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c.code < 32) append("\\u${c.code.toString(16).padStart(4, '0')}") else append(c)
        }
    }
}
