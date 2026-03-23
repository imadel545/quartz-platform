package com.quartz.platform.domain.repository

import com.quartz.platform.domain.model.Site
import kotlinx.coroutines.flow.Flow

interface SiteRepository {
    fun observeSites(): Flow<List<Site>>
}
