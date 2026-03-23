package com.quartz.platform.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quartz.platform.core.logging.AppLogger
import com.quartz.platform.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class QuartzSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val appLogger: AppLogger
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val processedCount = syncRepository.processPendingJobs(limit = 20)
            appLogger.info(TAG, "Sync worker completed, processed=$processedCount")
            Result.success()
        }.getOrElse { throwable ->
            appLogger.error(TAG, "Sync worker failed", throwable)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "QuartzSyncWorker"
    }
}
