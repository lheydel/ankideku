package com.ankideku.ui.components.sel.condition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ankideku.domain.sel.operator.SelOperatorRegistry
import com.ankideku.domain.sel.operator.SelType
import com.ankideku.ui.components.AppDropdown

/**
 * Shared UI components for the SEL query builder.
 */

/**
 * Dropdown selector for SEL operators.
 *
 * @param selectedOperator The currently selected operator key
 * @param onOperatorSelected Called when an operator is selected
 * @param returnType If specified, only show operators that return this type
 * @param modifier Modifier for the dropdown
 */
@Composable
fun OperatorSelector(
    selectedOperator: String,
    onOperatorSelected: (String) -> Unit,
    returnType: SelType? = null,
    modifier: Modifier = Modifier,
) {
    val selectedOp = SelOperatorRegistry[selectedOperator]

    val filteredOperators = remember(returnType) {
        if (returnType != null) {
            SelOperatorRegistry.returningType(returnType)
        } else {
            SelOperatorRegistry.userOperators
        }
    }

    AppDropdown(
        items = filteredOperators,
        selectedItem = selectedOp,
        onItemSelected = { onOperatorSelected(it.key) },
        itemLabel = { "${it.metadata.displayName} (${it.key})" },
        placeholder = "Select operator...",
        modifier = modifier,
    )
}
