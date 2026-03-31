package com.quartz.platform.presentation.performance.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.platform.R
import com.quartz.platform.core.text.UiStrings
import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.ThroughputMetrics
import com.quartz.platform.domain.usecase.CreateSitePerformanceSessionUseCase
import com.quartz.platform.domain.usecase.ObserveSiteDetailUseCase
import com.quartz.platform.domain.usecase.ObserveSitePerformanceSessionHistoryUseCase
import com.quartz.platform.domain.usecase.UpdatePerformanceSessionExecutionUseCase
import com.quartz.platform.domain.usecase.UpdatePerformanceStepStatusUseCase
import com.quartz.platform.presentation.navigation.QuartzDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PerformanceSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeSiteDetailUseCase: ObserveSiteDetailUseCase,
    private val observeSitePerformanceSessionHistoryUseCase: ObserveSitePerformanceSessionHistoryUseCase,
    private val createSitePerformanceSessionUseCase: CreateSitePerformanceSessionUseCase,
    private val updatePerformanceStepStatusUseCase: UpdatePerformanceStepStatusUseCase,
    private val updatePerformanceSessionExecutionUseCase: UpdatePerformanceSessionExecutionUseCase,
    private val uiStrings: UiStrings
) : ViewModel() {

    private val siteId: String = checkNotNull(savedStateHandle[QuartzDestination.PerformanceSession.ARG_SITE_ID])
    private val mutableState = MutableStateFlow(
        PerformanceSessionUiState(siteId = siteId)
    )
    val uiState: StateFlow<PerformanceSessionUiState> = mutableState.asStateFlow()

    init {
        observeContext()
    }

    fun onCreateThroughputSessionClicked() {
        createSession(PerformanceWorkflowType.THROUGHPUT)
    }

    fun onCreateQosSessionClicked() {
        createSession(PerformanceWorkflowType.QOS_SCRIPT)
    }

    fun onSelectHistorySessionClicked(sessionId: String) {
        mutableState.update { state ->
            state.copy(
                selectedSessionId = sessionId,
                hasUnsavedChanges = false,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onOperatorSelected(operator: String?) {
        mutableState.update { state ->
            state.copy(
                selectedOperator = operator,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onTechnologySelected(technology: String?) {
        mutableState.update { state ->
            state.copy(
                selectedTechnology = technology,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onStepStatusSelected(stepCode: PerformanceStepCode, status: PerformanceStepStatus) {
        val sessionId = mutableState.value.session?.id ?: return
        viewModelScope.launch {
            runCatching {
                updatePerformanceStepStatusUseCase(
                    sessionId = sessionId,
                    stepCode = stepCode,
                    status = status
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        errorMessage = throwable.message
                            ?: uiStrings.get(R.string.error_performance_update_step)
                    )
                }
            }
        }
    }

    fun onSessionStatusSelected(status: PerformanceSessionStatus) {
        val currentSession = mutableState.value.session
        if (currentSession != null && status == PerformanceSessionStatus.COMPLETED) {
            val guard = currentSession.completionGuard()
            if (!guard.canComplete) {
                mutableState.update { state ->
                    state.copy(
                        completionGuardMessage = uiStrings.get(
                            R.string.error_performance_complete_requires_required_steps
                        ),
                        errorMessage = null,
                        infoMessage = null
                    )
                }
                return
            }
        }
        mutableState.update { state ->
            state.copy(
                selectedStatus = status,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onPrerequisiteNetworkChanged(value: Boolean) {
        mutableState.update { state ->
            state.copy(
                prerequisiteNetworkReady = value,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onPrerequisiteBatteryChanged(value: Boolean) {
        mutableState.update { state ->
            state.copy(
                prerequisiteBatterySufficient = value,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onPrerequisiteLocationChanged(value: Boolean) {
        mutableState.update { state ->
            state.copy(
                prerequisiteLocationReady = value,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onThroughputDownloadChanged(value: String) = updateField { copy(throughputDownloadInput = sanitizeDecimal(value)) }
    fun onThroughputUploadChanged(value: String) = updateField { copy(throughputUploadInput = sanitizeDecimal(value)) }
    fun onThroughputLatencyChanged(value: String) = updateField { copy(throughputLatencyInput = sanitizeInteger(value)) }
    fun onThroughputMinDownloadChanged(value: String) = updateField { copy(throughputMinDownloadInput = sanitizeDecimal(value)) }
    fun onThroughputMinUploadChanged(value: String) = updateField { copy(throughputMinUploadInput = sanitizeDecimal(value)) }
    fun onThroughputMaxLatencyChanged(value: String) = updateField { copy(throughputMaxLatencyInput = sanitizeInteger(value)) }

    fun onQosScriptSelected(scriptId: String, scriptName: String) {
        mutableState.update { state ->
            state.copy(
                qosSelectedScriptId = scriptId,
                qosSelectedScriptName = scriptName,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onQosTargetTechnologyChanged(value: String) = updateField { copy(qosTargetTechnologyInput = value) }
    fun onQosTargetPhoneChanged(value: String) = updateField { copy(qosTargetPhoneInput = value.filter { it.isDigit() || it == '+' }) }
    fun onQosIterationCountChanged(value: String) = updateField { copy(qosIterationCountInput = sanitizeInteger(value)) }
    fun onQosSuccessCountChanged(value: String) = updateField { copy(qosSuccessCountInput = sanitizeInteger(value)) }
    fun onQosFailureCountChanged(value: String) = updateField { copy(qosFailureCountInput = sanitizeInteger(value)) }
    fun onNotesChanged(value: String) = updateField { copy(notesInput = value) }
    fun onResultSummaryChanged(value: String) = updateField { copy(resultSummaryInput = value) }

    fun onSaveSummaryClicked() {
        val current = mutableState.value
        val session = current.session ?: return
        if (current.isSavingSummary || !current.hasUnsavedChanges) return

        val throughputMetrics = parseThroughputMetrics(current) ?: return
        val qosSummary = parseQosRunSummary(current) ?: return

        if (current.selectedStatus == PerformanceSessionStatus.COMPLETED) {
            if (!(current.prerequisiteNetworkReady &&
                    current.prerequisiteBatterySufficient &&
                    current.prerequisiteLocationReady)
            ) {
                mutableState.update { state ->
                    state.copy(errorMessage = uiStrings.get(R.string.error_performance_complete_requires_prerequisites))
                }
                return
            }
            when (session.workflowType) {
                PerformanceWorkflowType.THROUGHPUT -> {
                    if (!throughputMetrics.hasAnyMeasurement) {
                        mutableState.update { state ->
                            state.copy(errorMessage = uiStrings.get(R.string.error_performance_throughput_result_required))
                        }
                        return
                    }
                }

                PerformanceWorkflowType.QOS_SCRIPT -> {
                    if (qosSummary.scriptId.isNullOrBlank() || qosSummary.scriptName.isNullOrBlank()) {
                        mutableState.update { state ->
                            state.copy(errorMessage = uiStrings.get(R.string.error_performance_qos_script_required))
                        }
                        return
                    }
                }
            }
        }

        viewModelScope.launch {
            mutableState.update { state ->
                state.copy(isSavingSummary = true, errorMessage = null, infoMessage = null)
            }

            runCatching {
                updatePerformanceSessionExecutionUseCase(
                    sessionId = session.id,
                    status = current.selectedStatus,
                    prerequisiteNetworkReady = current.prerequisiteNetworkReady,
                    prerequisiteBatterySufficient = current.prerequisiteBatterySufficient,
                    prerequisiteLocationReady = current.prerequisiteLocationReady,
                    throughputMetrics = throughputMetrics,
                    qosRunSummary = qosSummary,
                    notes = current.notesInput.trim(),
                    resultSummary = current.resultSummaryInput.trim()
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        isSavingSummary = false,
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_performance_save_summary)
                    )
                }
            }.onSuccess {
                mutableState.update { state ->
                    state.copy(
                        isSavingSummary = false,
                        hasUnsavedChanges = false,
                        completionGuardMessage = null,
                        infoMessage = uiStrings.get(R.string.info_performance_summary_saved)
                    )
                }
            }
        }
    }

    private fun createSession(workflowType: PerformanceWorkflowType) {
        val current = mutableState.value
        if (current.isCreatingSession || current.siteCode.isBlank()) return

        viewModelScope.launch {
            mutableState.update { state ->
                state.copy(
                    isCreatingSession = true,
                    errorMessage = null,
                    infoMessage = null,
                    completionGuardMessage = null
                )
            }
            runCatching {
                createSitePerformanceSessionUseCase(
                    siteId = siteId,
                    siteCode = mutableState.value.siteCode,
                    workflowType = workflowType,
                    operatorName = mutableState.value.selectedOperator,
                    technology = mutableState.value.selectedTechnology
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        isCreatingSession = false,
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_performance_create_session)
                    )
                }
            }.onSuccess { created ->
                mutableState.update { state ->
                    state.copy(
                        isCreatingSession = false,
                        selectedSessionId = created.id,
                        hasUnsavedChanges = false,
                        infoMessage = uiStrings.get(R.string.info_performance_session_created)
                    )
                }
            }
        }
    }

    private fun observeContext() {
        viewModelScope.launch {
            combine(
                observeSiteDetailUseCase(siteId),
                observeSitePerformanceSessionHistoryUseCase(siteId)
            ) { site, history -> site to history }
                .catch { throwable ->
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: uiStrings.get(R.string.error_performance_observe_session)
                        )
                    }
                }
                .collect { (site, history) ->
                    val operators = site?.sectors
                        .orEmpty()
                        .flatMap { sector -> sector.cells }
                        .map { cell -> cell.operatorName }
                        .distinct()
                        .sorted()
                    val technologies = site?.sectors
                        .orEmpty()
                        .flatMap { sector -> sector.cells }
                        .map { cell -> cell.technology }
                        .distinct()
                        .sorted()
                    val current = mutableState.value
                    val selectedSessionId = when {
                        current.selectedSessionId != null &&
                            history.any { session -> session.id == current.selectedSessionId } -> {
                            current.selectedSessionId
                        }
                        history.isNotEmpty() -> history.first().id
                        else -> null
                    }
                    val selectedSession = history.firstOrNull { session -> session.id == selectedSessionId }
                    val shouldHydrateInputs =
                        !current.hasUnsavedChanges || current.session?.id != selectedSession?.id

                    val baseState = current.copy(
                        isLoading = false,
                        siteCode = site?.externalCode.orEmpty(),
                        siteLabel = listOf(site?.name.orEmpty(), site?.externalCode.orEmpty())
                            .filter { it.isNotBlank() }
                            .joinToString(" - "),
                        availableOperators = operators,
                        availableTechnologies = technologies,
                        selectedOperator = current.selectedOperator ?: operators.firstOrNull(),
                        selectedTechnology = current.selectedTechnology ?: technologies.firstOrNull(),
                        selectedSessionId = selectedSessionId,
                        session = selectedSession,
                        sessionHistory = history
                    )

                    mutableState.value = if (shouldHydrateInputs) {
                        hydrateInputsFromSession(baseState, selectedSession)
                    } else {
                        baseState
                    }
                }
        }
    }

    private fun hydrateInputsFromSession(
        state: PerformanceSessionUiState,
        session: PerformanceSession?
    ): PerformanceSessionUiState {
        if (session == null) {
            return state.copy(
                selectedStatus = PerformanceSessionStatus.CREATED,
                prerequisiteNetworkReady = false,
                prerequisiteBatterySufficient = false,
                prerequisiteLocationReady = false,
                throughputDownloadInput = "",
                throughputUploadInput = "",
                throughputLatencyInput = "",
                throughputMinDownloadInput = "",
                throughputMinUploadInput = "",
                throughputMaxLatencyInput = "",
                qosSelectedScriptId = null,
                qosSelectedScriptName = null,
                qosTargetTechnologyInput = "",
                qosTargetPhoneInput = "",
                qosIterationCountInput = "",
                qosSuccessCountInput = "",
                qosFailureCountInput = "",
                notesInput = "",
                resultSummaryInput = "",
                hasUnsavedChanges = false,
                completionGuardMessage = null
            )
        }

        return state.copy(
            selectedStatus = session.status,
            selectedOperator = session.operatorName ?: state.selectedOperator,
            selectedTechnology = session.technology ?: state.selectedTechnology,
            prerequisiteNetworkReady = session.prerequisiteNetworkReady,
            prerequisiteBatterySufficient = session.prerequisiteBatterySufficient,
            prerequisiteLocationReady = session.prerequisiteLocationReady,
            throughputDownloadInput = session.throughputMetrics.downloadMbps?.toString().orEmpty(),
            throughputUploadInput = session.throughputMetrics.uploadMbps?.toString().orEmpty(),
            throughputLatencyInput = session.throughputMetrics.latencyMs?.toString().orEmpty(),
            throughputMinDownloadInput = session.throughputMetrics.minDownloadMbps?.toString().orEmpty(),
            throughputMinUploadInput = session.throughputMetrics.minUploadMbps?.toString().orEmpty(),
            throughputMaxLatencyInput = session.throughputMetrics.maxLatencyMs?.toString().orEmpty(),
            qosSelectedScriptId = session.qosRunSummary.scriptId,
            qosSelectedScriptName = session.qosRunSummary.scriptName,
            qosTargetTechnologyInput = session.qosRunSummary.targetTechnology.orEmpty(),
            qosTargetPhoneInput = session.qosRunSummary.targetPhoneNumber.orEmpty(),
            qosIterationCountInput = session.qosRunSummary.iterationCount.toString(),
            qosSuccessCountInput = session.qosRunSummary.successCount.toString(),
            qosFailureCountInput = session.qosRunSummary.failureCount.toString(),
            notesInput = session.notes,
            resultSummaryInput = session.resultSummary,
            hasUnsavedChanges = false,
            completionGuardMessage = null
        )
    }

    private fun updateField(transform: PerformanceSessionUiState.() -> PerformanceSessionUiState) {
        mutableState.update { state ->
            state.transform().copy(
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    private fun parseThroughputMetrics(state: PerformanceSessionUiState): ThroughputMetrics? {
        val download = parseOptionalDouble(
            value = state.throughputDownloadInput,
            errorRes = R.string.error_performance_invalid_number
        ) ?: return null
        val upload = parseOptionalDouble(
            value = state.throughputUploadInput,
            errorRes = R.string.error_performance_invalid_number
        ) ?: return null
        val latency = parseOptionalInt(
            value = state.throughputLatencyInput,
            errorRes = R.string.error_performance_invalid_integer
        ) ?: return null
        val minDownload = parseOptionalDouble(
            value = state.throughputMinDownloadInput,
            errorRes = R.string.error_performance_invalid_number
        ) ?: return null
        val minUpload = parseOptionalDouble(
            value = state.throughputMinUploadInput,
            errorRes = R.string.error_performance_invalid_number
        ) ?: return null
        val maxLatency = parseOptionalInt(
            value = state.throughputMaxLatencyInput,
            errorRes = R.string.error_performance_invalid_integer
        ) ?: return null

        return ThroughputMetrics(
            downloadMbps = download,
            uploadMbps = upload,
            latencyMs = latency,
            minDownloadMbps = minDownload,
            minUploadMbps = minUpload,
            maxLatencyMs = maxLatency
        )
    }

    private fun parseQosRunSummary(state: PerformanceSessionUiState): QosRunSummary? {
        val iterations = parseOptionalInt(
            value = state.qosIterationCountInput,
            errorRes = R.string.error_performance_invalid_integer
        ) ?: return null
        val success = parseOptionalInt(
            value = state.qosSuccessCountInput,
            errorRes = R.string.error_performance_invalid_integer
        ) ?: return null
        val failure = parseOptionalInt(
            value = state.qosFailureCountInput,
            errorRes = R.string.error_performance_invalid_integer
        ) ?: return null

        return QosRunSummary(
            scriptId = state.qosSelectedScriptId,
            scriptName = state.qosSelectedScriptName,
            targetTechnology = state.qosTargetTechnologyInput.trim().ifBlank { null },
            targetPhoneNumber = state.qosTargetPhoneInput.trim().ifBlank { null },
            iterationCount = iterations ?: 0,
            successCount = success ?: 0,
            failureCount = failure ?: 0
        )
    }

    private fun parseOptionalDouble(value: String, errorRes: Int): Double? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.toDoubleOrNull()?.also {
            if (it < 0.0) {
                mutableState.update { state ->
                    state.copy(errorMessage = uiStrings.get(errorRes))
                }
                return null
            }
        } ?: run {
            mutableState.update { state ->
                state.copy(errorMessage = uiStrings.get(errorRes))
            }
            null
        }
    }

    private fun parseOptionalInt(value: String, errorRes: Int): Int? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.toIntOrNull()?.takeIf { it >= 0 } ?: run {
            mutableState.update { state ->
                state.copy(errorMessage = uiStrings.get(errorRes))
            }
            null
        }
    }

    private fun sanitizeDecimal(value: String): String {
        return value.replace(',', '.').filterIndexed { index, c ->
            c.isDigit() || c == '.' || (c == '-' && index == 0)
        }
    }

    private fun sanitizeInteger(value: String): String {
        return value.filterIndexed { index, c ->
            c.isDigit() || (c == '-' && index == 0)
        }
    }
}
