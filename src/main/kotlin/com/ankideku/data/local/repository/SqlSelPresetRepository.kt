package com.ankideku.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ankideku.data.local.database.AnkiDekuDb
import com.ankideku.data.local.database.Sel_preset
import com.ankideku.domain.model.SelPreset
import com.ankideku.domain.model.SelPresetId
import com.ankideku.domain.repository.SelPresetRepository
import com.ankideku.domain.sel.model.EntityType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SqlSelPresetRepository(
    private val database: AnkiDekuDb,
) : SelPresetRepository {

    override fun getAll(): Flow<List<SelPreset>> =
        database.selPresetQueries.getAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    override fun getByTarget(target: EntityType): Flow<List<SelPreset>> =
        database.selPresetQueries.getByTarget(target.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    override fun getById(id: SelPresetId): SelPreset? =
        database.selPresetQueries.getById(id)
            .executeAsOneOrNull()
            ?.toDomain()

    override fun existsByName(name: String): Boolean =
        database.selPresetQueries.getByName(name)
            .executeAsOneOrNull() != null

    override fun save(preset: SelPreset): SelPresetId {
        val now = System.currentTimeMillis()
        database.selPresetQueries.insert(
            name = preset.name,
            description = preset.description,
            target = preset.target.name,
            query_json = preset.queryJson,
            scopes_json = preset.scopesJson,
            created_at = now,
            updated_at = now,
        )
        return database.selPresetQueries.lastInsertedId().executeAsOne()
    }

    override fun update(preset: SelPreset) {
        database.selPresetQueries.update(
            name = preset.name,
            description = preset.description,
            query_json = preset.queryJson,
            scopes_json = preset.scopesJson,
            updated_at = System.currentTimeMillis(),
            id = preset.id,
        )
    }

    override fun delete(id: SelPresetId) {
        database.selPresetQueries.delete(id)
    }

    private fun Sel_preset.toDomain() = SelPreset(
        id = id,
        name = name,
        description = description,
        target = EntityType.valueOf(target),
        queryJson = query_json,
        scopesJson = scopes_json,
        createdAt = created_at,
        updatedAt = updated_at,
    )
}
