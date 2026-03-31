package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "performance_qos_timeline_events",
    foreignKeys = [
        ForeignKey(
            entity = PerformanceSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "occurredAtEpochMillis"]),
        Index(value = ["sessionId", "checkpointSequence"])
    ]
)
data class PerformanceQosTimelineEventEntity(
    @PrimaryKey(autoGenerate = true)
    val eventId: Long = 0L,
    val sessionId: String,
    val family: String,
    val repetitionIndex: Int,
    val eventType: String,
    val reasonCode: String?,
    val reason: String?,
    val occurredAtEpochMillis: Long,
    val checkpointSequence: Int
)
