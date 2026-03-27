package com.quartz.platform.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "xfeeder_steps",
    primaryKeys = ["sessionId", "code"],
    foreignKeys = [
        ForeignKey(
            entity = XfeederSessionEntity::class,
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
data class XfeederStepEntity(
    val sessionId: String,
    val code: String,
    val required: Boolean,
    val status: String,
    val displayOrder: Int
)
