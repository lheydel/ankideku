package com.ankideku.domain.repository

import com.ankideku.domain.model.SelPreset
import com.ankideku.domain.model.SelPresetId
import com.ankideku.domain.sel.model.EntityType
import kotlinx.coroutines.flow.Flow

/**
 * Repository for SEL query presets.
 */
interface SelPresetRepository {
    /** Get all presets, ordered by most recently updated. */
    fun getAll(): Flow<List<SelPreset>>

    /** Get presets for a specific entity type, ordered by name. */
    fun getByTarget(target: EntityType): Flow<List<SelPreset>>

    /** Get a single preset by ID. */
    fun getById(id: SelPresetId): SelPreset?

    /** Check if a preset with the given name already exists. */
    fun existsByName(name: String): Boolean

    /** Save a new preset, returns the generated ID. */
    fun save(preset: SelPreset): SelPresetId

    /** Update an existing preset. */
    fun update(preset: SelPreset)

    /** Delete a preset by ID. */
    fun delete(id: SelPresetId)
}
