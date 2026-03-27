package com.quartz.platform.domain.repository

import com.quartz.platform.domain.model.SiteDetail
import com.quartz.platform.domain.model.SiteSummary
import kotlinx.coroutines.flow.Flow

interface SiteRepository {
    fun observeSiteList(): Flow<List<SiteSummary>>
    fun observeSiteDetail(siteId: String): Flow<SiteDetail?>
    suspend fun replaceSitesSnapshot(sites: List<SiteDetail>)
}
