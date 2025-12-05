package com.ankideku.ui.components.sel.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ankideku.domain.sel.ast.*
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.operator.SelOperatorMetadata
import com.ankideku.domain.sel.operator.SelOperatorRegistry
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

private val subqueryCounter = AtomicInteger(0)

/** Generate a unique alias for a subquery */
internal fun generateSubqueryAlias(): String = "sq${subqueryCounter.incrementAndGet()}"

/**
 * Types of operands that can be used in conditions.
 */
enum class OperandType(val displayName: String) {
    Field("Field"),
    Property("Property"),
    Value("Value"),
    Expression("Expression"),
    Subquery("Subquery"),
}

/**
 * Type of subquery result.
 *
 * @param displayName Human-readable name
 * @param returnType The SEL type this result produces (null for ScalarProperty which depends on selected property)
 */
enum class SubqueryResultType(
    val displayName: String,
    val returnType: com.ankideku.domain.sel.operator.SelType?,
) {
    Exists("Exists", com.ankideku.domain.sel.operator.SelType.Boolean),
    NotExists("Not Exists", com.ankideku.domain.sel.operator.SelType.Boolean),
    ScalarProperty("Get Property", null), // Type depends on selected property
    Count("Count", com.ankideku.domain.sel.operator.SelType.Number),
}

/**
 * State for a subquery operand.
 *
 * @param initialAlias The alias for this subquery (used by child subqueries to reference this scope).
 *                     If null, auto-generates based on target entity type.
 */
@Stable
class SubqueryState(
    initialTarget: EntityType = EntityType.Suggestion,
    initialResultType: SubqueryResultType = SubqueryResultType.Exists,
    initialAlias: String? = null,
) {
    val id: String = UUID.randomUUID().toString()
    var target by mutableStateOf(initialTarget)
    var resultType by mutableStateOf(initialResultType)
    var resultProperty by mutableStateOf("")
    var rootGroup by mutableStateOf(ConditionGroupState())
    var alias by mutableStateOf(initialAlias ?: generateSubqueryAlias())
    var orderBy by mutableStateOf<List<SelOrderClause>>(emptyList())
    var limit by mutableStateOf<Int?>(null)

    fun toSelNode(parentAlias: String?): SelNode {
        val subquery = SelQuery(
            target = target,
            alias = alias,
            where = rootGroup.toSelNode(alias),
            orderBy = orderBy.takeIf { it.isNotEmpty() },
            limit = limit,
            result = when (resultType) {
                SubqueryResultType.Exists, SubqueryResultType.NotExists -> null
                SubqueryResultType.ScalarProperty -> SelOperation("prop", SelArray(listOf(SelString(resultProperty))))
                SubqueryResultType.Count -> SelOperation("count", SelArray(listOf(SelString("*"))))
            },
        )

        val queryOp = SelOperation("query", SelArray(listOf(subquery)))
        return when (resultType) {
            SubqueryResultType.Exists -> SelOperation("exists", SelArray(listOf(queryOp)))
            SubqueryResultType.NotExists -> SelOperation("not", SelArray(listOf(
                SelOperation("exists", SelArray(listOf(queryOp)))
            )))
            SubqueryResultType.ScalarProperty, SubqueryResultType.Count -> queryOp
        }
    }

    fun copy(): SubqueryState = SubqueryState(
        initialTarget = target,
        initialResultType = resultType,
        initialAlias = alias,
    ).also { copy ->
        copy.resultProperty = resultProperty
        copy.rootGroup = rootGroup.copy()
        copy.orderBy = orderBy
        copy.limit = limit
    }

    fun toPreviewText(): String {
        val prefix = when (resultType) {
            SubqueryResultType.Exists -> "exists"
            SubqueryResultType.NotExists -> "not exists"
            SubqueryResultType.ScalarProperty -> resultProperty
            SubqueryResultType.Count -> "count"
        }
        return "$prefix(${target.name})"
    }
}

/**
 * State for an expression operand - an operator applied to nested operands.
 * Enables nested expressions like len(field(...)) or +(a, b).
 */
@Stable
class ExpressionState(
    initialOperator: String = "len",
) {
    val id: String = UUID.randomUUID().toString()
    var operator by mutableStateOf(initialOperator)
    var operands by mutableStateOf<List<OperandState>>(listOf(OperandState()))

    val operatorMetadata: SelOperatorMetadata? get() = SelOperatorRegistry.getMetadata(operator)

    fun setOperandCount(count: Int) {
        val current = operands.toMutableList()
        while (current.size < count) current.add(OperandState())
        while (current.size > count) current.removeLast()
        operands = current
    }

    fun toSelNode(parentAlias: String? = null): SelNode {
        val args = operands.map { it.toSelNode(parentAlias) }
        return SelOperation(operator, SelArray(args))
    }

    fun copy(): ExpressionState = ExpressionState(initialOperator = operator).also { copy ->
        copy.operands = operands.map { it.copy() }
    }

    fun toPreviewText(): String {
        val op = operatorMetadata?.displayName ?: operator
        return when (operands.size) {
            1 -> "$op(${operands[0].toPreviewText()})"
            else -> "$op(${operands.joinToString(", ") { it.toPreviewText() }})"
        }
    }
}

/**
 * Represents an operand in the UI builder.
 */
@Stable
class OperandState(
    initialType: OperandType = OperandType.Value,
    initialValue: String = "",
    initialFieldName: String = "",
    initialFieldContext: String? = null,
    initialPropertyName: String = "",
    initialPropertyScope: String? = null,
) {
    val id: String = UUID.randomUUID().toString()
    var type by mutableStateOf(initialType)
    var value by mutableStateOf(initialValue)
    var fieldName by mutableStateOf(initialFieldName)
    var fieldContext by mutableStateOf(initialFieldContext)
    var propertyName by mutableStateOf(initialPropertyName)
    /** Scope for property reference: null = current entity, "parent" = parent, "parent2" = grandparent, etc. */
    var propertyScope by mutableStateOf(initialPropertyScope)
    var subquery by mutableStateOf<SubqueryState?>(null)
    var expression by mutableStateOf<ExpressionState?>(null)

    fun toSelNode(parentAlias: String? = null): SelNode = when (type) {
        OperandType.Field -> {
            val args = if (fieldContext != null) {
                SelArray(listOf(SelString(fieldName), SelString(fieldContext!!)))
            } else {
                SelArray(listOf(SelString(fieldName)))
            }
            SelOperation("field", args)
        }
        OperandType.Property -> {
            if (propertyScope != null) {
                // Scoped property uses ref operator: ref("scope", "propertyName")
                SelOperation("ref", SelArray(listOf(SelString(propertyScope!!), SelString(propertyName))))
            } else {
                SelOperation("prop", SelArray(listOf(SelString(propertyName))))
            }
        }
        OperandType.Value -> parseValue(value)
        OperandType.Expression -> expression?.toSelNode(parentAlias) ?: SelNull
        OperandType.Subquery -> subquery?.toSelNode(parentAlias) ?: SelNull
    }

    private fun parseValue(str: String): SelNode = when {
        str.isEmpty() -> SelString("")
        str == "null" -> SelNull
        str == "true" -> SelBoolean.TRUE
        str == "false" -> SelBoolean.FALSE
        str.toLongOrNull() != null -> SelNumber(str.toLong())
        str.toDoubleOrNull() != null -> SelNumber(str.toDouble())
        else -> SelString(str)
    }

    fun copy(): OperandState = OperandState(
        initialType = type,
        initialValue = value,
        initialFieldName = fieldName,
        initialFieldContext = fieldContext,
        initialPropertyName = propertyName,
        initialPropertyScope = propertyScope,
    ).also { copy ->
        copy.subquery = subquery?.copy()
        copy.expression = expression?.copy()
    }

    fun toPreviewText(): String = when (type) {
        OperandType.Field -> if (fieldContext != null) "$fieldName[$fieldContext]" else fieldName
        OperandType.Property -> if (propertyScope != null) "$propertyScope.$propertyName" else propertyName
        OperandType.Value -> if (value.isEmpty()) "\"\"" else value
        OperandType.Expression -> expression?.toPreviewText() ?: "expr"
        OperandType.Subquery -> subquery?.toPreviewText() ?: "subquery"
    }
}
