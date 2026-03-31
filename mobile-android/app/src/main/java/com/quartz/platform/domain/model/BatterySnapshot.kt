package com.quartz.platform.domain.model

data class BatterySnapshot(
    val levelPercent: Int,
    val isCharging: Boolean
)
