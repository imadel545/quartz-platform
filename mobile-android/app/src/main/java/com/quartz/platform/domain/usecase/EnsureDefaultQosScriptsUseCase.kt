package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.QosDefaultScripts
import com.quartz.platform.domain.repository.QosScriptRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class EnsureDefaultQosScriptsUseCase @Inject constructor(
    private val repository: QosScriptRepository
) {
    suspend operator fun invoke() {
        val existing = repository.observeActiveScripts().first()
        if (existing.isNotEmpty()) return

        val now = System.currentTimeMillis()
        QosDefaultScripts.build(now).forEach { script ->
            repository.upsert(script)
        }
    }
}

