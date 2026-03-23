package com.quartz.platform.device.network

import com.quartz.platform.domain.model.NetworkStatus
import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
    fun observe(): Flow<NetworkStatus>
}
