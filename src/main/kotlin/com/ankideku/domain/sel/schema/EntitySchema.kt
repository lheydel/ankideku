package com.ankideku.domain.sel.schema

import com.ankideku.domain.sel.model.EntityType

/**
 * Property type for validation and SQL generation.
 */
enum class PropertyType {
    String,
    Int,
    Long,
    Boolean,
    Timestamp
}

/**
 * Property definition for an entity.
 *
 * @param selKey The key used in SEL queries (e.g., "noteId")
 * @param sqlColumn The actual SQL column name (e.g., "note_id")
 * @param type The property type for validation
 */
data class EntityProperty(
    val selKey: String,
    val sqlColumn: String,
    val type: PropertyType,
)

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
) {
    private val propertyMap = properties.associateBy { it.selKey }
    private val contextMap = fieldContexts.associateBy { it.selKey }

    fun getProperty(selKey: String): EntityProperty? = propertyMap[selKey]
    fun getFieldContext(selKey: String): SelFieldContext? = contextMap[selKey]
}
