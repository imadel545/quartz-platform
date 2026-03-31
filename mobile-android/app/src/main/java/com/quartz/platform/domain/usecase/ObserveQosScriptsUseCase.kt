package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.QosScriptDefinition
import com.quartz.platform.domain.repository.QosScriptRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveQosScriptsUseCase @Inject constructor(
    private val repository: QosScriptRepository
) {
    operator fun invoke(): Flow<List<QosScriptDefinition>> = repository.observeActiveScripts()
}

