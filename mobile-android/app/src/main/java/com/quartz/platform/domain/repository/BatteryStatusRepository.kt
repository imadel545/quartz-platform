package com.quartz.platform.domain.repository

import com.quartz.platform.domain.model.BatterySnapshot

interface BatteryStatusRepository {
    suspend fun getCurrentSnapshot(): BatterySnapshot?
}
