package com.quartz.platform.domain.repository

import com.quartz.platform.domain.model.SiteDetail

interface SiteSnapshotBootstrapSource {
    suspend fun loadDemoSnapshot(): List<SiteDetail>
}
