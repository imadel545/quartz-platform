package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.UserLocation
import com.quartz.platform.domain.repository.LocationRepository
import javax.inject.Inject

class GetLastKnownUserLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(): UserLocation? = locationRepository.getLastKnownLocation()
}
