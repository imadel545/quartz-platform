package com.quartz.platform.data.local

import androidx.room.TypeConverter
import com.quartz.platform.domain.model.SyncAggregateType
import com.quartz.platform.domain.model.SyncOperationType
import com.quartz.platform.domain.model.SyncJobStatus

class QuartzTypeConverters {
    @TypeConverter
    fun fromSyncJobStatus(value: SyncJobStatus): String = value.name

    @TypeConverter
    fun toSyncJobStatus(value: String): SyncJobStatus = SyncJobStatus.valueOf(value)

    @TypeConverter
    fun fromSyncAggregateType(value: SyncAggregateType): String = value.name

    @TypeConverter
    fun toSyncAggregateType(value: String): SyncAggregateType = SyncAggregateType.valueOf(value)

    @TypeConverter
    fun fromSyncOperationType(value: SyncOperationType): String = value.name

    @TypeConverter
    fun toSyncOperationType(value: String): SyncOperationType = SyncOperationType.valueOf(value)
}
