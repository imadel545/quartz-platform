package com.quartz.platform.presentation.xfeeder.session

import androidx.annotation.StringRes
import com.quartz.platform.R
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.domain.model.XfeederUnreliableReason

@StringRes
fun xfeederSessionStatusLabelRes(status: XfeederSessionStatus): Int {
    return when (status) {
        XfeederSessionStatus.CREATED -> R.string.xfeeder_status_created
        XfeederSessionStatus.IN_PROGRESS -> R.string.xfeeder_status_in_progress
        XfeederSessionStatus.COMPLETED -> R.string.xfeeder_status_completed
        XfeederSessionStatus.CANCELLED -> R.string.xfeeder_status_cancelled
    }
}

@StringRes
fun xfeederSectorOutcomeLabelRes(outcome: XfeederSectorOutcome): Int {
    return when (outcome) {
        XfeederSectorOutcome.NOT_TESTED -> R.string.xfeeder_outcome_not_tested
        XfeederSectorOutcome.WAITING_NETWORK -> R.string.xfeeder_outcome_waiting_network
        XfeederSectorOutcome.OK -> R.string.xfeeder_outcome_ok
        XfeederSectorOutcome.CROSSED -> R.string.xfeeder_outcome_crossed
        XfeederSectorOutcome.MIXFEEDER -> R.string.xfeeder_outcome_mixfeeder
        XfeederSectorOutcome.UNRELIABLE -> R.string.xfeeder_outcome_unreliable
    }
}

@StringRes
fun xfeederStepCodeLabelRes(code: XfeederStepCode): Int {
    return when (code) {
        XfeederStepCode.PRECONDITION_NETWORK_READY -> R.string.xfeeder_step_network_ready
        XfeederStepCode.PRECONDITION_MEASUREMENT_ZONE_READY -> R.string.xfeeder_step_measurement_zone_ready
        XfeederStepCode.OBSERVE_CONNECTED_CELLS -> R.string.xfeeder_step_observe_connected_cells
        XfeederStepCode.CHECK_SECTOR_CROSSING -> R.string.xfeeder_step_check_crossing
        XfeederStepCode.CHECK_MIXFEEDER_ALTERNANCE -> R.string.xfeeder_step_check_mixfeeder
        XfeederStepCode.FINALIZE_SECTOR_SUMMARY -> R.string.xfeeder_step_finalize_summary
    }
}

@StringRes
fun xfeederStepStatusLabelRes(status: XfeederStepStatus): Int {
    return when (status) {
        XfeederStepStatus.TODO -> R.string.xfeeder_step_status_todo
        XfeederStepStatus.IN_PROGRESS -> R.string.xfeeder_step_status_in_progress
        XfeederStepStatus.DONE -> R.string.xfeeder_step_status_done
        XfeederStepStatus.BLOCKED -> R.string.xfeeder_step_status_blocked
    }
}

@StringRes
fun xfeederUnreliableReasonLabelRes(reason: XfeederUnreliableReason): Int {
    return when (reason) {
        XfeederUnreliableReason.NO_MAJORITY_SECTOR -> R.string.xfeeder_unreliable_reason_no_majority_sector
        XfeederUnreliableReason.UNSTABLE_SECTOR_SWITCHING -> R.string.xfeeder_unreliable_reason_unstable_sector_switching
    }
}
