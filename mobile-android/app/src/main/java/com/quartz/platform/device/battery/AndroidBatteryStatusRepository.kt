package com.quartz.platform.device.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.quartz.platform.domain.model.BatterySnapshot
import com.quartz.platform.domain.repository.BatteryStatusRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidBatteryStatusRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : BatteryStatusRepository {

    override suspend fun getCurrentSnapshot(): BatterySnapshot? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val percent = ((level.toFloat() / scale.toFloat()) * 100f)
            .toInt()
            .coerceIn(0, 100)

        return BatterySnapshot(
            levelPercent = percent,
            isCharging = isCharging
        )
    }
}
