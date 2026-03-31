package com.quartz.platform.domain.repository

import com.quartz.platform.domain.model.NetworkStatus
import kotlinx.coroutines.flow.Flow

interface NetworkStatusRepository {
    fun observe(): Flow<NetworkStatus>
}
