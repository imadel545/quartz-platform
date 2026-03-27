package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.repository.SiteRepository
import javax.inject.Inject

class ReplaceSitesSnapshotUseCase @Inject constructor(
    private val siteRepository: SiteRepository
) {
    suspend operator fun invoke(sites: List<SiteDetail>) {
        siteRepository.replaceSitesSnapshot(sites)
    }
}
