package com.ankideku.ui.components.sel.operand

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankideku.domain.sel.model.EntityType
import com.ankideku.domain.sel.operator.SelType
import com.ankideku.domain.sel.schema.SelEntityRegistry
import com.ankideku.ui.components.AppDropdown
import com.ankideku.ui.components.sel.state.OperandState
import com.ankideku.ui.theme.Spacing

/**
 * Property selector with optional scope selector for parent entity references.
 */
@Composable
internal fun PropertySelector(
    operand: OperandState,
    entityType: EntityType,
    expectedType: SelType,
    parentScopes: List<Pair<String, EntityType>>,
) {
    // Build scope options: current entity + parent scopes
    val scopeOptions = buildList {
        add(null to entityType) // Current entity (null scope)
        addAll(parentScopes)
    }

    // Get selected scope's entity type
    val selectedScopeEntity = scopeOptions.find { it.first == operand.propertyScope }?.second ?: entityType

    // Get properties for selected scope
    val schema = SelEntityRegistry[selectedScopeEntity]
    val properties = schema.visiblePropertiesOfType(expectedType)

    if (parentScopes.isEmpty()) {
        // No parent scopes - just show property dropdown
        AppDropdown(
            items = properties,
            selectedItem = properties.find { it.selKey == operand.propertyName },
            onItemSelected = { prop -> operand.propertyName = prop.selKey },
            itemLabel = { prop -> prop.displayName ?: prop.selKey },
            placeholder = "Select property...",
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        // Show scope selector + property dropdown
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            // Scope selector
            AppDropdown(
                items = scopeOptions,
                selectedItem = scopeOptions.find { it.first == operand.propertyScope },
                onItemSelected = { (scope, _) ->
                    operand.propertyScope = scope
                    operand.propertyName = "" // Reset property when scope changes
                },
                itemLabel = { (scope, entity) ->
                    if (scope == null) entity.name else "$scope (${entity.name})"
                },
                modifier = Modifier.width(140.dp),
            )

            // Property dropdown for selected scope
            AppDropdown(
                items = properties,
                selectedItem = properties.find { it.selKey == operand.propertyName },
                onItemSelected = { prop -> operand.propertyName = prop.selKey },
                itemLabel = { prop -> prop.displayName ?: prop.selKey },
                placeholder = "property...",
                modifier = Modifier.weight(1f),
            )
        }
    }
}
