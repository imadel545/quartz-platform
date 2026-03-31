package com.quartz.platform.domain.repository

import com.quartz.platform.domain.model.QosScriptDefinition
import kotlinx.coroutines.flow.Flow

interface QosScriptRepository {
    fun observeActiveScripts(): Flow<List<QosScriptDefinition>>
    suspend fun getById(scriptId: String): QosScriptDefinition?
    suspend fun upsert(script: QosScriptDefinition): QosScriptDefinition
    suspend fun archive(scriptId: String)
}

