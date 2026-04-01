package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quartz.platform.domain.model.SupervisorQueueActionType
import com.quartz.platform.domain.model.SupervisorQueueStatus

@Entity(
    tableName = "supervisor_queue_actions",
    indices = [
        Index(value = ["draftId"]),
        Index(value = ["actedAtEpochMillis"])
    ]
)
data class SupervisorQueueActionEntity(
    @PrimaryKey val id: String,
    val draftId: String,
    val actionType: SupervisorQueueActionType,
    val fromStatus: SupervisorQueueStatus?,
    val toStatus: SupervisorQueueStatus?,
    val note: String?,
    val triggeredFromFilter: String?,
    val triggeredFromPreset: String?,
    val actedAtEpochMillis: Long
)
