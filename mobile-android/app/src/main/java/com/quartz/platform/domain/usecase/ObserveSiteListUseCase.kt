package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.SiteSummary
import com.quartz.platform.domain.repository.SiteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSiteListUseCase @Inject constructor(
    private val siteRepository: SiteRepository
) {
    operator fun invoke(): Flow<List<SiteSummary>> = siteRepository.observeSiteList()
}
