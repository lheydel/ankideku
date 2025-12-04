package com.ankideku.domain.sel.ast

/**
 * Null primitive value (singleton).
 */
data object SelNull : SelPrimitive<Any?> {
    override val value: Any? = null
}
