package com.quartz.platform.presentation.ret.session

import com.quartz.platform.domain.model.RetGuidedSession
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus

data class RetGuidedSessionUiState(
    val isLoading: Boolean = true,
    val siteId: String = "",
    val siteLabel: String = "",
    val sectorId: String = "",
    val sectorCode: String = "",
    val session: RetGuidedSession? = null,
    val sessionHistory: List<RetGuidedSession> = emptyList(),
    val latestSessionId: String? = null,
    val selectedStatus: RetSessionStatus = RetSessionStatus.CREATED,
    val selectedOutcome: RetResultOutcome = RetResultOutcome.NOT_RUN,
    val notesInput: String = "",
    val resultSummaryInput: String = "",
    val completionGuardMessage: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val isCreatingSession: Boolean = false,
    val isCreatingDraft: Boolean = false,
    val isSavingSummary: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)
