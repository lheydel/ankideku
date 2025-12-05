package com.ankideku.domain.sel.operator

/**
 * All arithmetic operators.
 */
object ArithmeticOperators : List<ArithmeticOperator> by listOf(
    ArithmeticOperator("+", "+", "Add", "Add numbers together", minArguments = 2, defaultFirstArgument = 0),
    ArithmeticOperator("-", "-", "Subtract", "Subtract numbers", minArguments = 2, defaultFirstArgument = 0),
    ArithmeticOperator("*", "*", "Multiply", "Multiply numbers together", minArguments = 2, defaultFirstArgument = 1),
    ArithmeticOperator("/", "/", "Divide", "Divide first number by second", minArguments = 2, maxArguments = 2),
    ArithmeticOperator("%", "%", "Modulo", "Get remainder of division", minArguments = 2, maxArguments = 2),
)

/**
 * Parameterized arithmetic operator.
 *
 * @param key The operator key (e.g., "+", "-", "*", "/", "%")
 * @param sqlOperator The SQL operator to use
 * @param displayName Human-readable name for UI
 * @param description Brief description of the operation
 * @param minArguments Minimum number of arguments required
 * @param maxArguments Maximum number of arguments allowed
 * @param defaultFirstArgument Value prepended when one argument short (e.g., 0 for unary minus)
 */
class ArithmeticOperator(
    override val key: String,
    override val sqlOperator: String,
    displayName: String,
    description: String,
    override val minArguments: Int = 2,
    override val maxArguments: Int = Int.MAX_VALUE,
    override val defaultFirstArgument: Number? = null,
) : MathOperator() {

    override val metadata = MathOperator.arithmeticMetadata(displayName, description, minArguments, maxArguments)
}
