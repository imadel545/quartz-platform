package com.quartz.platform.presentation.report.draft

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.platform.R
import com.quartz.platform.BuildConfig
import com.quartz.platform.core.text.UiStrings
import com.quartz.platform.data.remote.simulation.SyncSimulationControl
import com.quartz.platform.data.remote.simulation.SyncSimulationMode
import com.quartz.platform.domain.model.ReportClosureProjection
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.usecase.EnqueueReportDraftSyncUseCase
import com.quartz.platform.domain.usecase.ObserveSiteReportClosureProjectionsUseCase
import com.quartz.platform.domain.usecase.ObserveReportDraftSyncTraceUseCase
import com.quartz.platform.domain.usecase.ObserveReportDraftUseCase
import com.quartz.platform.domain.usecase.UpdateReportDraftUseCase
import com.quartz.platform.presentation.navigation.QuartzDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.jvm.JvmSuppressWildcards
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ReportDraftViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeReportDraftUseCase: ObserveReportDraftUseCase,
    private val updateReportDraftUseCase: UpdateReportDraftUseCase,
    private val enqueueReportDraftSyncUseCase: EnqueueReportDraftSyncUseCase,
    private val observeReportDraftSyncTraceUseCase: ObserveReportDraftSyncTraceUseCase,
    private val observeSiteReportClosureProjectionsUseCase: ObserveSiteReportClosureProjectionsUseCase,
    private val syncSimulationControls: Set<@JvmSuppressWildcards SyncSimulationControl>,
    private val uiStrings: UiStrings
) : ViewModel() {

    private val draftId: String = checkNotNull(savedStateHandle[QuartzDestination.ReportDraft.ARG_DRAFT_ID])

    private val _uiState = MutableStateFlow(ReportDraftUiState())
    val uiState: StateFlow<ReportDraftUiState> = _uiState.asStateFlow()
    private val syncSimulationControl: SyncSimulationControl? = syncSimulationControls.firstOrNull()

    init {
        _uiState.update { state ->
            state.copy(
                isSyncSimulationControlVisible = BuildConfig.DEBUG && syncSimulationControl != null
            )
        }
        observeDraft()
        observeSyncState()
        observeSyncSimulationMode()
    }

    fun onTitleChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                titleInput = value,
                hasUnsavedChanges = true,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onObservationChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                observationInput = value,
                hasUnsavedChanges = true,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onSaveClicked() {
        val current = _uiState.value
        if (current.draft == null || current.isSaving) return
        if (current.titleInput.trim().isEmpty()) {
            _uiState.update { state ->
                state.copy(errorMessage = uiStrings.get(R.string.error_report_title_empty))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isSaving = true, errorMessage = null, infoMessage = null)
            }
            runCatching {
                requireNotNull(
                    updateReportDraftUseCase(
                        draftId = draftId,
                        title = _uiState.value.titleInput.trim(),
                        observation = _uiState.value.observationInput.trim()
                    )
                ) { uiStrings.get(R.string.error_draft_not_found_during_update) }
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_save_local_draft)
                    )
                }
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        hasUnsavedChanges = false,
                        infoMessage = uiStrings.get(R.string.info_local_draft_saved)
                    )
                }
            }
        }
    }

    fun onQueueSyncClicked() {
        val current = _uiState.value
        if (current.draft == null || current.isQueueingSync || current.isSaving) return
        if (current.hasUnsavedChanges) {
            _uiState.update { state ->
                state.copy(errorMessage = uiStrings.get(R.string.error_save_before_sync))
            }
            return
        }
        if (current.syncState == ReportSyncState.PENDING) return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isQueueingSync = true, errorMessage = null, infoMessage = null)
            }
            runCatching {
                enqueueReportDraftSyncUseCase(draftId)
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isQueueingSync = false,
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_enqueue_report_sync)
                    )
                }
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isQueueingSync = false,
                        infoMessage = uiStrings.get(R.string.info_enqueued_report_sync)
                    )
                }
            }
        }
    }

    fun onSyncSimulationModeSelected(mode: SyncSimulationMode) {
        val control = syncSimulationControl ?: return
        viewModelScope.launch {
            control.setMode(mode)
            _uiState.update { state ->
                state.copy(
                    infoMessage = uiStrings.get(R.string.info_debug_mode_updated, mode.name),
                    errorMessage = null
                )
            }
        }
    }

    private fun observeDraft() {
        viewModelScope.launch {
            val draftFlow = observeReportDraftUseCase(draftId)
            val closureProjectionFlow = draftFlow.flatMapLatest { draft ->
                if (draft == null) {
                    flowOf(emptyList())
                } else {
                    observeSiteReportClosureProjectionsUseCase(draft.siteId)
                }
            }

            combine(draftFlow, closureProjectionFlow) { draft, closureProjections ->
                draft to closureProjections
            }
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: uiStrings.get(R.string.error_observe_local_draft)
                        )
                    }
                }
                .collect { (draft, closureProjections) ->
                    _uiState.update { state ->
                        val shouldHydrateInputs = !state.hasUnsavedChanges || state.draft?.id != draft?.id
                        val projectedClosures = projectClosuresForDraft(
                            draft = draft,
                            siteClosures = closureProjections
                        )
                        state.copy(
                            isLoading = false,
                            draft = draft,
                            titleInput = if (shouldHydrateInputs) draft?.title.orEmpty() else state.titleInput,
                            observationInput = if (shouldHydrateInputs) {
                                draft?.observation.orEmpty()
                            } else {
                                state.observationInput
                            },
                            hasUnsavedChanges = if (shouldHydrateInputs) false else state.hasUnsavedChanges,
                            closureProjections = projectedClosures
                        )
                    }
                }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            observeReportDraftSyncTraceUseCase(draftId)
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(errorMessage = throwable.message ?: uiStrings.get(R.string.error_observe_sync_state))
                    }
                }
                .collect { syncTrace ->
                    _uiState.update { state -> state.copy(syncTrace = syncTrace) }
                }
        }
    }

    private fun observeSyncSimulationMode() {
        val control = syncSimulationControl ?: return
        viewModelScope.launch {
            control.observeMode()
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            errorMessage = throwable.message ?: uiStrings.get(R.string.error_observe_debug_mode)
                        )
                    }
                }
                .collect { mode ->
                    _uiState.update { state -> state.copy(syncSimulationMode = mode) }
                }
        }
    }

    private fun projectClosuresForDraft(
        draft: ReportDraft?,
        siteClosures: List<ReportClosureProjection>
    ): List<ReportClosureProjection> {
        if (draft == null || siteClosures.isEmpty()) return emptyList()

        val workflowScopedClosures = if (draft.originWorkflowType == null) {
            siteClosures
        } else {
            siteClosures.filter { projection ->
                projection.workflowType == draft.originWorkflowType
            }
        }
        if (workflowScopedClosures.isEmpty()) return emptyList()

        val bySession = draft.originSessionId?.let { originSessionId ->
            workflowScopedClosures.firstOrNull { projection -> projection.sessionId == originSessionId }
        }
        if (bySession != null) return listOf(bySession)

        val bySector = draft.originSectorId?.let { originSectorId ->
            workflowScopedClosures.firstOrNull { projection -> projection.sectorId == originSectorId }
        }
        if (bySector != null) return listOf(bySector)

        return workflowScopedClosures
    }
}
