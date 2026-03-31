package com.quartz.platform.data.local.mapper

import com.quartz.platform.data.local.entity.QosScriptEntity
import com.quartz.platform.domain.model.QosScriptDefinition
import com.quartz.platform.domain.model.QosTestFamily

private const val CSV_SEPARATOR = ","

fun QosScriptEntity.toDomain(): QosScriptDefinition {
    return QosScriptDefinition(
        id = id,
        name = name,
        repeatCount = repeatCount.coerceAtLeast(1),
        targetTechnologies = targetTechnologiesCsv.toCsvSet(),
        testFamilies = testFamiliesCsv
            .toCsvSet()
            .mapNotNull { raw -> runCatching { QosTestFamily.valueOf(raw) }.getOrNull() }
            .toSet(),
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        isArchived = isArchived
    )
}

fun QosScriptDefinition.toEntity(): QosScriptEntity {
    return QosScriptEntity(
        id = id,
        name = name.trim(),
        repeatCount = repeatCount.coerceAtLeast(1),
        targetTechnologiesCsv = targetTechnologies
            .map { value -> value.trim() }
            .filter { value -> value.isNotBlank() }
            .toSortedSet()
            .joinToString(CSV_SEPARATOR),
        testFamiliesCsv = testFamilies
            .map { family -> family.name }
            .toSortedSet()
            .joinToString(CSV_SEPARATOR),
        isArchived = isArchived,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}

private fun String.toCsvSet(): Set<String> {
    return split(CSV_SEPARATOR)
        .map { part -> part.trim() }
        .filter { part -> part.isNotBlank() }
        .toSet()
}

