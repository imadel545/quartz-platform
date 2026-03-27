package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.repository.SiteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSiteDetailUseCase @Inject constructor(
    private val siteRepository: SiteRepository
) {
    operator fun invoke(siteId: String): Flow<SiteDetail?> = siteRepository.observeSiteDetail(siteId)
}
