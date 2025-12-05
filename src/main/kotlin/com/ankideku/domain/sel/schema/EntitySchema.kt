package com.ankideku.domain.sel.schema

import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.operator.SelType

/**
 * Property definition for an entity.
 *
 * @param selKey The key used in SEL queries (e.g., "noteId")
 * @param sqlColumn The actual SQL column name (e.g., "note_id")
 * @param type The SEL type for validation and UI filtering
 * @param displayName Human-readable name for UI display, or null to hide from UI builder
 */
data class EntityProperty(
    val selKey: String,
    val sqlColumn: String,
    val type: SelType,
    val displayName: String? = null,
) {
    /** Whether this property should be shown in the UI builder */
    val isVisibleInBuilder: Boolean get() = displayName != null
}

/**
 * Field context for entities that have field_value storage.
 *
 * @param selKey The key used in SEL queries (e.g., "changes")
 * @param sqlValue The value stored in field_value.context column
 */
data class SelFieldContext(
    val selKey: String,
    val sqlValue: String,
)

/**
 * Type of scope for determining which data source to use.
 */
enum class ScopeType {
    /** Deck scope - loads from DeckFinder */
    Deck,
    /** Session scope - loads from SessionFinder */
    Session,
}

/**
 * Scope definition for filtering entities.
 *
 * Scopes allow filtering by related entities (e.g., filter notes by deck, suggestions by session).
 * They appear as dropdowns at the top of the query builder.
 *
 * @param key Unique identifier for the scope (e.g., "deck", "session")
 * @param displayName Human-readable name for UI display
 * @param propertyKey The entity property that holds the scope value (e.g., "deckId")
 * @param type The type of scope, determines which data source to use
 */
data class EntityScope(
    val key: String,
    val displayName: String,
    val propertyKey: String,
    val type: ScopeType,
)

/**
 * Relation between two entities.
 *
 * @param targetEntity The related entity type
 * @param sourceProperty SEL property on source entity (e.g., "noteId")
 * @param targetProperty SEL property on target entity (e.g., "id")
 */
data class EntityRelation(
    val targetEntity: EntityType,
    val sourceProperty: String,
    val targetProperty: String,
)

/**
 * Complete entity definition.
 *
 * Defines the structure of an entity for SQL generation:
 * - Which SQL table it maps to
 * - What properties (columns) it has
 * - What field contexts are valid (for field_value access)
 * - What relations to other entities exist
 *
 * @param fieldValueFkColumn The foreign key column in field_value table for this entity (null if no field access)
 */
data class EntitySchema(
    val type: EntityType,
    val sqlTable: String,
    val sqlAlias: String,
    val properties: List<EntityProperty>,
    val fieldContexts: List<SelFieldContext>,
    val relations: List<EntityRelation>,
    val fieldValueFkColumn: String? = null,
    val scopes: List<EntityScope> = emptyList(),
) {
    private val propertyMap = properties.associateBy { it.selKey }
    private val contextMap = fieldContexts.associateBy { it.selKey }

    /** Properties visible in the UI builder (those with displayName set) */
    val visibleProperties: List<EntityProperty> by lazy {
        properties.filter { it.isVisibleInBuilder }
    }

    /** Get visible properties that match the expected type */
    fun visiblePropertiesOfType(expectedType: SelType): List<EntityProperty> =
        if (expectedType == SelType.Any) {
            visibleProperties
        } else {
            visibleProperties.filter { it.type == expectedType || it.type == SelType.Any }
        }

    fun getProperty(selKey: String): EntityProperty? = propertyMap[selKey]
    fun getFieldContext(selKey: String): SelFieldContext? = contextMap[selKey]
}
