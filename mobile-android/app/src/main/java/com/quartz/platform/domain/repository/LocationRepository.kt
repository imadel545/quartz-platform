package com.quartz.platform.domain.repository

import com.quartz.platform.domain.model.UserLocation

interface LocationRepository {
    suspend fun getLastKnownLocation(): UserLocation?
}
