package com.ankideku.domain.sel.operator

/**
 * All arithmetic operators.
 */
object ArithmeticOperators : List<ArithmeticOperator> by listOf(
    ArithmeticOperator("+", sqlOperator = "+", minArguments = 2, defaultFirstArgument = 0),
    ArithmeticOperator("-", sqlOperator = "-", minArguments = 2, defaultFirstArgument = 0),
    ArithmeticOperator("*", sqlOperator = "*", minArguments = 2, defaultFirstArgument = 1),
    ArithmeticOperator("/", sqlOperator = "/", minArguments = 2, maxArguments = 2),
    ArithmeticOperator("%", sqlOperator = "%", minArguments = 2, maxArguments = 2),
)

/**
 * Parameterized arithmetic operator.
 *
 * @param key The operator key (e.g., "+", "-", "*", "/", "%")
 * @param sqlOperator The SQL operator to use
 * @param minArguments Minimum number of arguments required
 * @param maxArguments Maximum number of arguments allowed
 * @param defaultFirstArgument Value prepended when one argument short (e.g., 0 for unary minus)
 */
class ArithmeticOperator(
    override val key: String,
    override val sqlOperator: String,
    override val minArguments: Int = 2,
    override val maxArguments: Int = Int.MAX_VALUE,
    override val defaultFirstArgument: Number? = null,
) : MathOperator()
