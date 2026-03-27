package com.quartz.platform.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.model.SiteSummary
import com.quartz.platform.domain.repository.SiteRepository
import com.quartz.platform.domain.repository.SiteSnapshotBootstrapSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BootstrapDemoSiteSnapshotUseCaseTest {

    @Test
    fun `bootstrap use case replaces local snapshot with demo data`() = runTest {
        val expected = listOf(
            SiteDetail(
                id = "demo-site",
                externalCode = "QRTZ-001",
                name = "Demo",
                latitude = 0.0,
                longitude = 0.0,
                status = "IN_SERVICE",
                sectorsInService = 3,
                sectorsForecast = 0,
                indoorOnly = false,
                updatedAtEpochMillis = 1L
            )
        )
        val repository = CapturingSiteRepository()
        val source = object : SiteSnapshotBootstrapSource {
            override suspend fun loadDemoSnapshot(): List<SiteDetail> = expected
        }

        BootstrapDemoSiteSnapshotUseCase(repository, source).invoke()

        assertThat(repository.replacedSnapshot).isEqualTo(expected)
    }

    private class CapturingSiteRepository : SiteRepository {
        var replacedSnapshot: List<SiteDetail> = emptyList()

        override fun observeSiteList(): Flow<List<SiteSummary>> = flowOf(emptyList())

        override fun observeSiteDetail(siteId: String): Flow<SiteDetail?> = flowOf(null)

        override suspend fun replaceSitesSnapshot(sites: List<SiteDetail>) {
            replacedSnapshot = sites
        }
    }
}
