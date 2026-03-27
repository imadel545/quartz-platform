package com.quartz.platform.data.remote.simulation

enum class SyncSimulationMode {
    NORMAL_SUCCESS,
    FAIL_NEXT_RETRYABLE,
    FAIL_ONCE_THEN_SUCCESS,
    FAIL_NEXT_TERMINAL
}
