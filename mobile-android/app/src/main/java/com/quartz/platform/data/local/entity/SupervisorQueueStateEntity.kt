package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quartz.platform.domain.model.SupervisorQueueActionType
import com.quartz.platform.domain.model.SupervisorQueueStatus

@Entity(tableName = "supervisor_queue_states")
data class SupervisorQueueStateEntity(
    @PrimaryKey val draftId: String,
    val status: SupervisorQueueStatus,
    val lastActionType: SupervisorQueueActionType?,
    val lastActionAtEpochMillis: Long?,
    val lastActionNote: String?,
    val updatedAtEpochMillis: Long
)
