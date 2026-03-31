package com.quartz.platform.presentation.performance.session

import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosScriptDefinition
import com.quartz.platform.domain.model.QosFamilyExecutionStatus
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.QosTestFamily
import com.quartz.platform.domain.model.ThroughputMetrics

data class PerformanceSessionUiState(
    val isLoading: Boolean = true,
    val siteId: String = "",
    val siteCode: String = "",
    val siteLabel: String = "",
    val availableOperators: List<String> = emptyList(),
    val availableTechnologies: List<String> = emptyList(),
    val selectedOperator: String? = null,
    val selectedTechnology: String? = null,
    val selectedSessionId: String? = null,
    val session: PerformanceSession? = null,
    val sessionHistory: List<PerformanceSession> = emptyList(),
    val selectedStatus: PerformanceSessionStatus = PerformanceSessionStatus.CREATED,
    val prerequisiteNetworkReady: Boolean = false,
    val prerequisiteBatterySufficient: Boolean = false,
    val prerequisiteLocationReady: Boolean = false,
    val throughputDownloadInput: String = "",
    val throughputUploadInput: String = "",
    val throughputLatencyInput: String = "",
    val throughputMinDownloadInput: String = "",
    val throughputMinUploadInput: String = "",
    val throughputMaxLatencyInput: String = "",
    val qosSelectedScriptId: String? = null,
    val qosSelectedScriptName: String? = null,
    val qosSelectedTestFamilies: Set<QosTestFamily> = emptySet(),
    val qosConfiguredRepeatInput: String = "",
    val qosConfiguredTechnologies: Set<String> = emptySet(),
    val qosScriptSnapshotUpdatedAtEpochMillis: Long? = null,
    val availableQosScripts: List<QosScriptDefinition> = emptyList(),
    val qosScriptEditorNameInput: String = "",
    val qosScriptEditorRepeatInput: String = "1",
    val qosScriptEditorTechnologiesInput: String = "",
    val qosScriptEditorSelectedFamilies: Set<QosTestFamily> = emptySet(),
    val isSavingQosScript: Boolean = false,
    val qosFamilyStatusByType: Map<QosTestFamily, QosFamilyExecutionStatus> = emptyMap(),
    val qosFamilyFailureReasonByType: Map<QosTestFamily, String> = emptyMap(),
    val qosTargetTechnologyInput: String = "",
    val qosTargetPhoneInput: String = "",
    val qosIterationCountInput: String = "",
    val qosSuccessCountInput: String = "",
    val qosFailureCountInput: String = "",
    val notesInput: String = "",
    val resultSummaryInput: String = "",
    val completionGuardMessage: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val isCreatingSession: Boolean = false,
    val isSavingSummary: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
) {
    val selectedSessionWorkflowType: PerformanceWorkflowType?
        get() = session?.workflowType
}

fun PerformanceSession.toEditableThroughputMetrics(): ThroughputMetrics = throughputMetrics

fun PerformanceSession.toEditableQosRunSummary(): QosRunSummary = qosRunSummary
