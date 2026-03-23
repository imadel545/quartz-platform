package com.quartz.platform.presentation.home

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.MainDispatcherRule
import com.quartz.platform.device.network.NetworkMonitor
import com.quartz.platform.domain.model.NetworkStatus
import com.quartz.platform.domain.model.Site
import com.quartz.platform.domain.repository.SiteRepository
import com.quartz.platform.domain.repository.SyncRepository
import com.quartz.platform.domain.usecase.ObserveSitesUseCase
import com.quartz.platform.domain.usecase.ObserveSyncQueueUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `ui state exposes local sites and sync queue`() = runTest {
        val siteRepository = FakeSiteRepository()
        val syncRepository = FakeSyncRepository()
        val networkMonitor = FakeNetworkMonitor(NetworkStatus.AVAILABLE)

        val viewModel = HomeViewModel(
            observeSitesUseCase = ObserveSitesUseCase(siteRepository),
            observeSyncQueueUseCase = ObserveSyncQueueUseCase(syncRepository),
            networkMonitor = networkMonitor
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.siteCount).isEqualTo(1)
        assertThat(state.pendingSyncJobs).isEqualTo(0)
        assertThat(state.networkStatus).isEqualTo(NetworkStatus.AVAILABLE)
    }

    @Test
    fun `ui state exposes unavailable network when monitor emits unavailable`() = runTest {
        val siteRepository = FakeSiteRepository()
        val syncRepository = FakeSyncRepository()
        val networkMonitor = FakeNetworkMonitor(NetworkStatus.UNAVAILABLE)

        val viewModel = HomeViewModel(
            observeSitesUseCase = ObserveSitesUseCase(siteRepository),
            observeSyncQueueUseCase = ObserveSyncQueueUseCase(syncRepository),
            networkMonitor = networkMonitor
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.networkStatus).isEqualTo(NetworkStatus.UNAVAILABLE)
    }

    private class FakeSiteRepository : SiteRepository {
        private val sitesFlow = MutableStateFlow(
            listOf(
                Site(
                    id = "id-1",
                    externalCode = "QRTZ-1",
                    name = "Site 1",
                    latitude = 0.0,
                    longitude = 0.0,
                    status = "IN_SERVICE",
                    sectorsInService = 3,
                    sectorsForecast = 0,
                    indoorOnly = false
                )
            )
        )

        override fun observeSites(): Flow<List<Site>> = sitesFlow
    }

    private class FakeSyncRepository : SyncRepository {
        private val pendingCount = MutableStateFlow(0)

        override fun observePendingJobCount(): Flow<Int> = pendingCount

        override suspend fun enqueueReportUpload(reportId: String) {
            pendingCount.value = pendingCount.value + 1
        }

        override suspend fun processPendingJobs(limit: Int): Int = 0
    }

    private class FakeNetworkMonitor(status: NetworkStatus) : NetworkMonitor {
        private val statusFlow = flowOf(status)
        override fun observe(): Flow<NetworkStatus> = statusFlow
    }
}
