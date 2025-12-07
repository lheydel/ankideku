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

    /**
     * Get metadata for an operator by key.
     */
    fun getMetadata(key: SelOperatorKey): SelOperatorMetadata? = operators[key]?.metadata

    /**
     * Get all operators in a specific category.
     */
    fun byCategory(category: SelOperatorCategory): List<SelOperator> =
        operators.values.filter { it.metadata.category == category }

    /**
     * Get all user-facing operators (excludes Internal category).
     */
    val userOperators: List<SelOperator> by lazy {
        operators.values.filter { it.metadata.category != SelOperatorCategory.Internal }
    }

    /**
     * Get all user-facing operators grouped by category.
     */
    val userOperatorsByCategory: Map<SelOperatorCategory, List<SelOperator>> by lazy {
        userOperators.groupBy { it.metadata.category }
    }

    /**
     * Get operators that return a specific type.
     * If type is Any, returns all user operators (no restriction).
     */
    fun returningType(type: SelType): List<SelOperator> =
        if (type == SelType.Any) {
            userOperators
        } else {
            userOperators.filter {
                it.metadata.signature.returnType == type || it.metadata.signature.returnType == SelType.Any
            }
        }

    /**
     * Get operators that accept a specific argument type.
     */
    fun acceptingType(type: SelType): List<SelOperator> =
        userOperators.filter { op ->
            op.metadata.signature.argTypes.any { argType ->
                argType == type || argType == SelType.Any
            }
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
