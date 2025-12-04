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
data class SelString(override val value: String) : SelPrimitive<String>

/**
 * Number primitive value (supports both integers and floating point).
 */
data class SelNumber(override val value: Number) : SelPrimitive<Number> {
    fun toDouble(): Double = value.toDouble()
    fun toLong(): Long = value.toLong()
    fun toInt(): Int = value.toInt()
}

/**
 * Boolean primitive value.
 */
data class SelBoolean(override val value: Boolean) : SelPrimitive<Boolean> {
    companion object {
        val TRUE = SelBoolean(true)
        val FALSE = SelBoolean(false)
    }
}
