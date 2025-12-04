package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.SelOperatorException
import com.ankideku.domain.sel.ast.SelArray

// ==================== Argument Validation ====================

/**
 * Require exactly [expected] arguments for an operator.
 * @throws SelOperatorException if the argument count doesn't match
 */
fun requireArgs(args: SelArray, expected: Int, operator: String, jsonPath: String) {
    if (args.size != expected) {
        throw SelOperatorException("'$operator' requires $expected arguments, got ${args.size}", jsonPath)
    }
}

/**
 * Require at least [min] arguments for an operator.
 * @throws SelOperatorException if there are fewer arguments
 */
fun requireMinArgs(args: SelArray, min: Int, operator: String, jsonPath: String) {
    if (args.size < min) {
        throw SelOperatorException("'$operator' requires at least $min arguments, got ${args.size}", jsonPath)
    }
}

/**
 * Require at least [max] arguments for an operator.
 * @throws SelOperatorException if there are fewer arguments
 */
fun requireMaxArgs(args: SelArray, max: Int, operator: String, jsonPath: String) {
    if (args.size > max) {
        throw SelOperatorException("'$operator' requires at least $max arguments, got ${args.size}", jsonPath)
    }
}
