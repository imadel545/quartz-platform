package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.BatterySnapshot
import com.quartz.platform.domain.repository.BatteryStatusRepository
import javax.inject.Inject

class GetCurrentBatterySnapshotUseCase @Inject constructor(
    private val repository: BatteryStatusRepository
) {
    suspend operator fun invoke(): BatterySnapshot? = repository.getCurrentSnapshot()
}
