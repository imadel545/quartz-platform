package com.quartz.platform.presentation.site.list

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.MainDispatcherRule
import com.quartz.platform.TestUiStrings
import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.model.SiteSummary
import com.quartz.platform.domain.repository.SiteRepository
import com.quartz.platform.domain.repository.SiteSnapshotBootstrapSource
import com.quartz.platform.domain.usecase.BootstrapDemoSiteSnapshotUseCase
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
class SiteListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `ui state exposes cached site list`() = runTest {
        val repository = FakeSiteRepository(
            initialSites = listOf(sampleSiteDetail("site-1", "Rabat Centre"))
        )

        val viewModel = SiteListViewModel(
            observeSiteListUseCase = ObserveSiteListUseCase(repository),
            bootstrapDemoSiteSnapshotUseCase = BootstrapDemoSiteSnapshotUseCase(
                siteRepository = repository,
                bootstrapSource = FakeSnapshotSource(listOf(sampleSiteDetail("demo-1", "Demo")))
            ),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
        assertThat(state.sites).hasSize(1)
        assertThat(state.sites.single().name).isEqualTo("Rabat Centre")
    }

    @Test
    fun `demo snapshot bootstrap populates site list and info message`() = runTest {
        val repository = FakeSiteRepository(initialSites = emptyList())
        val snapshotSource = FakeSnapshotSource(
            listOf(sampleSiteDetail("demo-1", "Demo Snapshot Site"))
        )

        val viewModel = SiteListViewModel(
            observeSiteListUseCase = ObserveSiteListUseCase(repository),
            bootstrapDemoSiteSnapshotUseCase = BootstrapDemoSiteSnapshotUseCase(
                siteRepository = repository,
                bootstrapSource = snapshotSource
            ),
            uiStrings = TestUiStrings()
        )

        advanceUntilIdle()
        viewModel.onLoadDemoSnapshotClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.sites).hasSize(1)
        assertThat(state.sites.single().name).isEqualTo("Demo Snapshot Site")
        assertThat(state.infoMessage).isNotNull()
    }

    private class FakeSiteRepository(
        initialSites: List<SiteDetail>
    ) : SiteRepository {
        private val detailsFlow = MutableStateFlow(initialSites)

        override fun observeSiteList(): Flow<List<SiteSummary>> = detailsFlow
            .map { details ->
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

        override fun observeSiteDetail(siteId: String): Flow<SiteDetail?> = detailsFlow
            .map { details -> details.firstOrNull { it.id == siteId } }

        override suspend fun replaceSitesSnapshot(sites: List<SiteDetail>) {
            detailsFlow.value = sites
        }
    }

    private class FakeSnapshotSource(
        private val snapshot: List<SiteDetail>
    ) : SiteSnapshotBootstrapSource {
        override suspend fun loadDemoSnapshot(): List<SiteDetail> = snapshot
    }

    private fun sampleSiteDetail(id: String, name: String): SiteDetail {
        return SiteDetail(
            id = id,
            externalCode = "QRTZ-$id",
            name = name,
            latitude = 0.0,
            longitude = 0.0,
            status = "IN_SERVICE",
            sectorsInService = 3,
            sectorsForecast = 0,
            indoorOnly = false,
            updatedAtEpochMillis = 1L
        )
    }

}
