package com.quartz.platform.presentation.home.map

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.MainDispatcherRule
import com.quartz.platform.TestUiStrings
import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.model.SiteSummary
import com.quartz.platform.domain.model.UserLocation
import com.quartz.platform.domain.repository.LocationRepository
import com.quartz.platform.domain.repository.SiteRepository
import com.quartz.platform.domain.repository.SiteSnapshotBootstrapSource
import com.quartz.platform.domain.usecase.BootstrapDemoSiteSnapshotUseCase
import com.quartz.platform.domain.usecase.GetLastKnownUserLocationUseCase
import com.quartz.platform.domain.usecase.ObserveSiteListUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeMapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `home map exposes filtered sites and keeps first as selected`() = runTest {
        val repository = FakeSiteRepository(
            initialSites = listOf(
                sampleSiteDetail("s-1", "QRTZ-001", "Rabat Centre", 34.0, -6.8),
                sampleSiteDetail("s-2", "QRTZ-002", "Casablanca Port", 33.6, -7.6)
            )
        )

        val viewModel = HomeMapViewModel(
            observeSiteListUseCase = ObserveSiteListUseCase(repository),
            bootstrapDemoSiteSnapshotUseCase = BootstrapDemoSiteSnapshotUseCase(
                siteRepository = repository,
                bootstrapSource = FakeSnapshotSource(emptyList())
            ),
            getLastKnownUserLocationUseCase = GetLastKnownUserLocationUseCase(
                locationRepository = FakeLocationRepository(null)
            ),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.filteredSites).hasSize(2)
        assertThat(state.selectedSite?.id).isEqualTo("s-1")
        assertThat(state.cameraCenter?.latitude).isEqualTo(34.0)
    }

    @Test
    fun `search by code filters map sites`() = runTest {
        val repository = FakeSiteRepository(
            initialSites = listOf(
                sampleSiteDetail("s-1", "QRTZ-001", "Rabat Centre", 34.0, -6.8),
                sampleSiteDetail("s-2", "QRTZ-002", "Casablanca Port", 33.6, -7.6)
            )
        )

        val viewModel = HomeMapViewModel(
            observeSiteListUseCase = ObserveSiteListUseCase(repository),
            bootstrapDemoSiteSnapshotUseCase = BootstrapDemoSiteSnapshotUseCase(
                siteRepository = repository,
                bootstrapSource = FakeSnapshotSource(emptyList())
            ),
            getLastKnownUserLocationUseCase = GetLastKnownUserLocationUseCase(
                locationRepository = FakeLocationRepository(null)
            ),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()
        viewModel.onSearchQueryChanged("002")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.filteredSites).hasSize(1)
        assertThat(state.filteredSites.single().id).isEqualTo("s-2")
        assertThat(state.selectedSite?.id).isEqualTo("s-2")
    }

    @Test
    fun `recenter uses last known user location when available`() = runTest {
        val repository = FakeSiteRepository(
            initialSites = listOf(sampleSiteDetail("s-1", "QRTZ-001", "Rabat Centre", 34.0, -6.8))
        )

        val viewModel = HomeMapViewModel(
            observeSiteListUseCase = ObserveSiteListUseCase(repository),
            bootstrapDemoSiteSnapshotUseCase = BootstrapDemoSiteSnapshotUseCase(
                siteRepository = repository,
                bootstrapSource = FakeSnapshotSource(emptyList())
            ),
            getLastKnownUserLocationUseCase = GetLastKnownUserLocationUseCase(
                locationRepository = FakeLocationRepository(
                    UserLocation(latitude = 35.0, longitude = -5.0, capturedAtEpochMillis = 42L)
                )
            ),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()
        viewModel.onRecenterClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.userLocation).isNotNull()
        assertThat(state.cameraCenter?.latitude).isEqualTo(35.0)
        assertThat(state.cameraCenter?.longitude).isEqualTo(-5.0)
    }

    private class FakeSiteRepository(
        initialSites: List<SiteDetail>
    ) : SiteRepository {
        private val detailsFlow = MutableStateFlow(initialSites)

        override fun observeSiteList(): Flow<List<SiteSummary>> {
            return detailsFlow.map { details ->
                details.map { detail ->
                    SiteSummary(
                        id = detail.id,
                        externalCode = detail.externalCode,
                        name = detail.name,
                        latitude = detail.latitude,
                        longitude = detail.longitude,
                        status = detail.status,
                        sectorsInService = detail.sectorsInService,
                        sectorsForecast = detail.sectorsForecast,
                        indoorOnly = detail.indoorOnly,
                        updatedAtEpochMillis = detail.updatedAtEpochMillis
                    )
                }
            }
        }

        override fun observeSiteDetail(siteId: String): Flow<SiteDetail?> {
            return detailsFlow.map { details -> details.firstOrNull { it.id == siteId } }
        }

        override suspend fun replaceSitesSnapshot(sites: List<SiteDetail>) {
            detailsFlow.value = sites
        }
    }

    private class FakeSnapshotSource(
        private val snapshot: List<SiteDetail>
    ) : SiteSnapshotBootstrapSource {
        override suspend fun loadDemoSnapshot(): List<SiteDetail> = snapshot
    }

    private class FakeLocationRepository(
        private val location: UserLocation?
    ) : LocationRepository {
        override suspend fun getLastKnownLocation(): UserLocation? = location
    }

    private fun sampleSiteDetail(
        id: String,
        code: String,
        name: String,
        latitude: Double,
        longitude: Double
    ): SiteDetail {
        return SiteDetail(
            id = id,
            externalCode = code,
            name = name,
            latitude = latitude,
            longitude = longitude,
            status = "IN_SERVICE",
            sectorsInService = 3,
            sectorsForecast = 0,
            indoorOnly = false,
            updatedAtEpochMillis = 1L
        )
    }
}
