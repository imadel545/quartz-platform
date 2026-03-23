package com.quartz.platform.data.repository

import com.quartz.platform.data.local.dao.SiteDao
import com.quartz.platform.data.local.mapper.toDomain
import com.quartz.platform.domain.model.Site
import com.quartz.platform.domain.repository.SiteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineFirstSiteRepository @Inject constructor(
    private val siteDao: SiteDao
) : SiteRepository {
    override fun observeSites(): Flow<List<Site>> = siteDao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }
}
