package com.ankideku.domain.sel.ast

/**
 * Array of SEL nodes.
 * Delegates List operations to the underlying values list.
 */
class SelArray(
    private val values: List<SelNode> = emptyList()
) : SelNode, List<SelNode> by values
