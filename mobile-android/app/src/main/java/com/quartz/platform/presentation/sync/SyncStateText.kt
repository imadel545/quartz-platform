package com.quartz.platform.presentation.sync

import androidx.annotation.StringRes
import com.quartz.platform.R
import com.quartz.platform.domain.model.ReportSyncState

@StringRes
fun syncStateLabelRes(state: ReportSyncState): Int {
    return when (state) {
        ReportSyncState.LOCAL_ONLY -> R.string.sync_state_local_only
        ReportSyncState.PENDING -> R.string.sync_state_pending
        ReportSyncState.SYNCED -> R.string.sync_state_synced
        ReportSyncState.FAILED -> R.string.sync_state_failed
    }
}

@StringRes
fun syncStateDescriptionRes(state: ReportSyncState): Int {
    return when (state) {
        ReportSyncState.LOCAL_ONLY -> R.string.sync_state_description_local_only
        ReportSyncState.PENDING -> R.string.sync_state_description_pending
        ReportSyncState.SYNCED -> R.string.sync_state_description_synced
        ReportSyncState.FAILED -> R.string.sync_state_description_failed
    }
}
