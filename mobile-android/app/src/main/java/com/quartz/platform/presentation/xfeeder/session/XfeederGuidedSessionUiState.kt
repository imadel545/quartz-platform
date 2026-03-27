package com.quartz.platform.presentation.xfeeder.session

import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederUnreliableReason

data class XfeederGuidedSessionUiState(
    val isLoading: Boolean = true,
    val siteId: String = "",
    val siteLabel: String = "",
    val sectorId: String = "",
    val sectorCode: String = "",
    val session: XfeederGuidedSession? = null,
    val sessionHistory: List<XfeederGuidedSession> = emptyList(),
    val latestSessionId: String? = null,
    val selectedStatus: XfeederSessionStatus = XfeederSessionStatus.CREATED,
    val selectedOutcome: XfeederSectorOutcome = XfeederSectorOutcome.NOT_TESTED,
    val relatedSectorCodeInput: String = "",
    val selectedUnreliableReason: XfeederUnreliableReason? = null,
    val observedSectorCountInput: String = "",
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
