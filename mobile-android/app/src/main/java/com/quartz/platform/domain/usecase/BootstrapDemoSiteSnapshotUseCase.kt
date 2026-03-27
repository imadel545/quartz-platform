package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.repository.SiteRepository
import com.quartz.platform.domain.repository.SiteSnapshotBootstrapSource
import javax.inject.Inject

class BootstrapDemoSiteSnapshotUseCase @Inject constructor(
    private val siteRepository: SiteRepository,
    private val bootstrapSource: SiteSnapshotBootstrapSource
) {
    suspend operator fun invoke() {
        val snapshot = bootstrapSource.loadDemoSnapshot()
        siteRepository.replaceSitesSnapshot(snapshot)
    }
}
