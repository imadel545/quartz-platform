package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSyncQueueUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(): Flow<Int> = syncRepository.observePendingJobCount()
}
