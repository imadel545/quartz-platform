package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.NetworkStatus
import com.quartz.platform.domain.repository.NetworkStatusRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveNetworkStatusUseCase @Inject constructor(
    private val repository: NetworkStatusRepository
) {
    operator fun invoke(): Flow<NetworkStatus> = repository.observe()
}
