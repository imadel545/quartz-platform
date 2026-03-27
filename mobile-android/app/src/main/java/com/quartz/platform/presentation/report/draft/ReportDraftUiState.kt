package com.quartz.platform.presentation.report.draft

import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.domain.model.GuidedSessionClosureProjection
import com.quartz.platform.data.remote.simulation.SyncSimulationMode

data class ReportDraftUiState(
    val isLoading: Boolean = true,
    val draft: ReportDraft? = null,
    val titleInput: String = "",
    val observationInput: String = "",
    val hasUnsavedChanges: Boolean = false,
    val syncTrace: ReportSyncTrace = ReportSyncTrace.localOnly(),
    val closureProjections: List<GuidedSessionClosureProjection> = emptyList(),
    val isSyncSimulationControlVisible: Boolean = false,
    val syncSimulationMode: SyncSimulationMode = SyncSimulationMode.NORMAL_SUCCESS,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val isQueueingSync: Boolean = false
) {
    val syncState: ReportSyncState
        get() = syncTrace.state
}
