package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qos_scripts")
data class QosScriptEntity(
    @PrimaryKey val id: String,
    val name: String,
    val repeatCount: Int,
    val targetTechnologiesCsv: String,
    val testFamiliesCsv: String,
    val isArchived: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

