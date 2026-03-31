package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "performance_qos_timeline_events",
    primaryKeys = ["sessionId", "family", "repetitionIndex", "eventType"],
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
        Index(value = ["sessionId", "occurredAtEpochMillis"])
    ]
)
data class PerformanceQosTimelineEventEntity(
    val sessionId: String,
    val family: String,
    val repetitionIndex: Int,
    val eventType: String,
    val reason: String?,
    val occurredAtEpochMillis: Long
)
