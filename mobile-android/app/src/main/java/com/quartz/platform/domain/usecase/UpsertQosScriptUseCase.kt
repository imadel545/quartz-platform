package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.QosScriptDefinition
import com.quartz.platform.domain.model.QosTestFamily
import com.quartz.platform.domain.repository.QosScriptRepository
import java.util.UUID
import javax.inject.Inject

class UpsertQosScriptUseCase @Inject constructor(
    private val repository: QosScriptRepository
) {
    suspend operator fun invoke(
        id: String?,
        name: String,
        repeatCount: Int,
        targetTechnologies: Set<String>,
        testFamilies: Set<QosTestFamily>
    ): QosScriptDefinition {
        val sanitizedName = name.trim()
        require(sanitizedName.isNotBlank()) { "Le nom du script QoS est requis." }
        require(repeatCount >= 1) { "Le nombre de répétitions doit être supérieur ou égal à 1." }
        require(testFamilies.isNotEmpty()) { "Sélectionnez au moins une famille de tests QoS." }

        val sanitizedTechnologies = targetTechnologies
            .map { value -> value.trim() }
            .filter { value -> value.isNotBlank() }
            .toSet()
        require(sanitizedTechnologies.isNotEmpty()) { "Sélectionnez au moins une technologie cible pour le script QoS." }

        val now = System.currentTimeMillis()
        val existing = id?.let { scriptId -> repository.getById(scriptId) }
        val next = QosScriptDefinition(
            id = existing?.id ?: UUID.randomUUID().toString(),
            name = sanitizedName,
            repeatCount = repeatCount,
            targetTechnologies = sanitizedTechnologies,
            testFamilies = testFamilies,
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
            updatedAtEpochMillis = now,
            isArchived = false
        )
        return repository.upsert(next)
    }
}
