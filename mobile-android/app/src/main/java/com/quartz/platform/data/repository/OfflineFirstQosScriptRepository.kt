package com.quartz.platform.data.repository

import com.quartz.platform.data.local.dao.QosScriptDao
import com.quartz.platform.data.local.mapper.toDomain
import com.quartz.platform.data.local.mapper.toEntity
import com.quartz.platform.domain.model.QosScriptDefinition
import com.quartz.platform.domain.repository.QosScriptRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstQosScriptRepository @Inject constructor(
    private val dao: QosScriptDao
) : QosScriptRepository {
    override fun observeActiveScripts(): Flow<List<QosScriptDefinition>> {
        return dao.observeActiveScripts().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }

    override suspend fun getById(scriptId: String): QosScriptDefinition? {
        return dao.getById(scriptId)?.toDomain()
    }

    override suspend fun upsert(script: QosScriptDefinition): QosScriptDefinition {
        dao.upsert(script.toEntity())
        return script
    }

    override suspend fun archive(scriptId: String) {
        dao.archive(scriptId = scriptId, updatedAtEpochMillis = System.currentTimeMillis())
    }
}

