package com.ankideku.domain.sel.operator

import com.ankideku.domain.sel.ast.SelOperatorKey

private val operators = mutableMapOf<SelOperatorKey, SelOperator>()

/**
 * Registry for SEL operators.
 *
 * Provides lookup of operators by their key and allows registration of custom operators.
 * Built-in operators are registered during initialization.
 */
object SelOperatorRegistry : Map<SelOperatorKey, SelOperator> by operators {

    /**
     * Register an operator. If an operator with the same key exists, it will be replaced.
     */
    fun register(operator: SelOperator) {
        operators[operator.key] = operator
    }

    /**
     * Register multiple operators at once.
     */
    fun register(operators: Iterable<SelOperator>) {
        operators.forEach { register(it) }
    }

    init {
        // Field access operators
        register(FieldOperator)
        register(PropOperator)

        // Scope reference operator
        register(RefOperator)

        // Subquery operators
        register(QueryOperator)
        register(ExistsOperator)

        // Comparison operators
        register(ComparisonOperators)

        // Logical operators
        register(LogicOperators)
        register(NotOperator)

        // String operators
        register(StringMatchOperators)
        register(LenOperator)

        // Predicate operators
        register(NullCheckOperators)
        register(EmptyCheckOperators)

        // Math operators
        register(AggregateOperators)
        register(ArithmeticOperators)
    }
}
