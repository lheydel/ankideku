package com.ankideku.domain.model

import com.ankideku.domain.sel.model.EntityType

/**
 * A saved SEL query preset.
 *
 * Presets allow users to save and reuse common queries.
 */
data class SelPreset(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val target: EntityType,
    val queryJson: String,
    val scopesJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

typealias SelPresetId = Long
