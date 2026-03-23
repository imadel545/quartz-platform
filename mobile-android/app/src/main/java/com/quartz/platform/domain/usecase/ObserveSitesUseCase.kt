package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.Site
import com.quartz.platform.domain.repository.SiteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSitesUseCase @Inject constructor(
    private val siteRepository: SiteRepository
) {
    operator fun invoke(): Flow<List<Site>> = siteRepository.observeSites()
}
