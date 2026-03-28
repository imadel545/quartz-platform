package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "ret_steps",
    primaryKeys = ["sessionId", "code"],
    foreignKeys = [
        ForeignKey(
            entity = RetSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "displayOrder"])
    ]
)
data class RetStepEntity(
    val sessionId: String,
    val code: String,
    val required: Boolean,
    val status: String,
    val displayOrder: Int
)
