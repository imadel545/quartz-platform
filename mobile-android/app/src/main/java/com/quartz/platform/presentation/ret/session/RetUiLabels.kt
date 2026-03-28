package com.quartz.platform.presentation.ret.session

import androidx.annotation.StringRes
import com.quartz.platform.R
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.model.RetStepCode
import com.quartz.platform.domain.model.RetStepStatus

@StringRes
fun retSessionStatusLabelRes(status: RetSessionStatus): Int {
    return when (status) {
        RetSessionStatus.CREATED -> R.string.ret_status_created
        RetSessionStatus.IN_PROGRESS -> R.string.ret_status_in_progress
        RetSessionStatus.COMPLETED -> R.string.ret_status_completed
        RetSessionStatus.CANCELLED -> R.string.ret_status_cancelled
    }
}

@StringRes
fun retResultOutcomeLabelRes(outcome: RetResultOutcome): Int {
    return when (outcome) {
        RetResultOutcome.NOT_RUN -> R.string.ret_result_not_run
        RetResultOutcome.PASS -> R.string.ret_result_pass
        RetResultOutcome.FAIL -> R.string.ret_result_fail
        RetResultOutcome.INCONCLUSIVE -> R.string.ret_result_inconclusive
    }
}

@StringRes
fun retStepCodeLabelRes(code: RetStepCode): Int {
    return when (code) {
        RetStepCode.CALIBRATION_PRECHECK -> R.string.ret_step_calibration_precheck
        RetStepCode.VALIDATION_CAPTURE -> R.string.ret_step_validation_capture
        RetStepCode.RESTORE_TILT_AND_RESULT -> R.string.ret_step_restore_tilt_and_result
    }
}

@StringRes
fun retStepInstructionLabelRes(code: RetStepCode): Int {
    return when (code) {
        RetStepCode.CALIBRATION_PRECHECK -> R.string.ret_step_instruction_calibration
        RetStepCode.VALIDATION_CAPTURE -> R.string.ret_step_instruction_validation
        RetStepCode.RESTORE_TILT_AND_RESULT -> R.string.ret_step_instruction_restore
    }
}

@StringRes
fun retStepStatusLabelRes(status: RetStepStatus): Int {
    return when (status) {
        RetStepStatus.TODO -> R.string.ret_step_status_todo
        RetStepStatus.IN_PROGRESS -> R.string.ret_step_status_in_progress
        RetStepStatus.DONE -> R.string.ret_step_status_done
        RetStepStatus.BLOCKED -> R.string.ret_step_status_blocked
    }
}
