package com.quartz.platform.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.platform.device.network.NetworkMonitor
import com.quartz.platform.domain.model.NetworkStatus
import com.quartz.platform.domain.usecase.ObserveSitesUseCase
import com.quartz.platform.domain.usecase.ObserveSyncQueueUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeSitesUseCase: ObserveSitesUseCase,
    observeSyncQueueUseCase: ObserveSyncQueueUseCase,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        observeSitesUseCase(),
        observeSyncQueueUseCase(),
        networkMonitor.observe()
    ) { sites, pendingSyncCount, networkStatus ->
        HomeUiState(
            siteCount = sites.size,
            pendingSyncJobs = pendingSyncCount,
            networkStatus = networkStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeUiState(networkStatus = NetworkStatus.UNAVAILABLE)
    )
}
