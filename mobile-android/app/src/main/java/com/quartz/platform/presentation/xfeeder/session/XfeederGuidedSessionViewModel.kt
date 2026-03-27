package com.quartz.platform.presentation.xfeeder.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.platform.R
import com.quartz.platform.core.text.UiStrings
import com.quartz.platform.domain.model.XfeederClosureEvidence
import com.quartz.platform.domain.model.XfeederClosureEvidenceIssue
import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.domain.model.XfeederUnreliableReason
import com.quartz.platform.domain.model.validateClosureEvidenceForFinalization
import com.quartz.platform.domain.usecase.CreateSectorXfeederSessionUseCase
import com.quartz.platform.domain.usecase.OpenOrCreateGuidedSessionReportDraftUseCase
import com.quartz.platform.domain.usecase.ObserveSectorXfeederSessionHistoryUseCase
import com.quartz.platform.domain.usecase.ObserveSiteDetailUseCase
import com.quartz.platform.domain.usecase.UpdateXfeederSessionSummaryUseCase
import com.quartz.platform.domain.usecase.UpdateXfeederStepStatusUseCase
import com.quartz.platform.presentation.navigation.QuartzDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class XfeederGuidedSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeSiteDetailUseCase: ObserveSiteDetailUseCase,
    private val observeSectorXfeederSessionHistoryUseCase: ObserveSectorXfeederSessionHistoryUseCase,
    private val createSectorXfeederSessionUseCase: CreateSectorXfeederSessionUseCase,
    private val updateXfeederStepStatusUseCase: UpdateXfeederStepStatusUseCase,
    private val updateXfeederSessionSummaryUseCase: UpdateXfeederSessionSummaryUseCase,
    private val openOrCreateGuidedSessionReportDraftUseCase: OpenOrCreateGuidedSessionReportDraftUseCase,
    private val uiStrings: UiStrings
) : ViewModel() {

    private val siteId: String = checkNotNull(savedStateHandle[QuartzDestination.XfeederGuidedSession.ARG_SITE_ID])
    private val sectorId: String = checkNotNull(savedStateHandle[QuartzDestination.XfeederGuidedSession.ARG_SECTOR_ID])

    private val mutableState = MutableStateFlow(
        XfeederGuidedSessionUiState(
            siteId = siteId,
            sectorId = sectorId
        )
    )
    val uiState: StateFlow<XfeederGuidedSessionUiState> = mutableState.asStateFlow()
    private val _events = MutableSharedFlow<XfeederGuidedSessionEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    init {
        observeContext()
    }

    fun onCreateSessionClicked() {
        val current = mutableState.value
        if (current.isCreatingSession) return
        if (current.sectorCode.isBlank()) {
            mutableState.update { state ->
                state.copy(errorMessage = uiStrings.get(R.string.error_xfeeder_sector_not_found))
            }
            return
        }

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
                createSectorXfeederSessionUseCase(
                    siteId = siteId,
                    sectorId = sectorId,
                    sectorCode = mutableState.value.sectorCode
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        isCreatingSession = false,
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_xfeeder_create_session)
                    )
                }
            }.onSuccess { created ->
                mutableState.update { state ->
                    state.copy(
                        isCreatingSession = false,
                        infoMessage = uiStrings.get(R.string.info_xfeeder_session_created),
                        latestSessionId = created.id,
                        hasUnsavedChanges = false,
                        completionGuardMessage = null
                    )
                }
            }
        }
    }

    fun onResumeLatestClicked() {
        val latestId = mutableState.value.sessionHistory.firstOrNull()?.id ?: return
        selectSessionById(latestId)
    }

    fun onSelectHistorySessionClicked(sessionId: String) {
        selectSessionById(sessionId)
    }

    fun onStepStatusSelected(stepCode: XfeederStepCode, status: XfeederStepStatus) {
        val sessionId = mutableState.value.session?.id ?: return
        viewModelScope.launch {
            runCatching {
                updateXfeederStepStatusUseCase(
                    sessionId = sessionId,
                    stepCode = stepCode,
                    status = status
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_xfeeder_update_step)
                    )
                }
            }
        }
    }

    fun onNotesChanged(value: String) {
        mutableState.update { state ->
            state.copy(
                notesInput = value,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onResultSummaryChanged(value: String) {
        mutableState.update { state ->
            state.copy(
                resultSummaryInput = value,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onSessionStatusSelected(status: XfeederSessionStatus) {
        val session = mutableState.value.session
        val current = mutableState.value
        val completionValidationMessage = if (session != null) {
            validateCompletionRequirements(
                status = status,
                session = session,
                outcome = current.selectedOutcome,
                closureEvidence = buildClosureEvidence(current)
            )
        } else {
            null
        }
        if (completionValidationMessage != null) {
            mutableState.update { state ->
                state.copy(
                    completionGuardMessage = completionValidationMessage,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            return
        }

        mutableState.update { state ->
            state.copy(
                selectedStatus = status,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onSectorOutcomeSelected(outcome: XfeederSectorOutcome) {
        mutableState.update { state ->
            val nextRelatedSector = when (outcome) {
                XfeederSectorOutcome.CROSSED,
                XfeederSectorOutcome.MIXFEEDER -> state.relatedSectorCodeInput
                else -> ""
            }
            val nextUnreliableReason = if (outcome == XfeederSectorOutcome.UNRELIABLE) {
                state.selectedUnreliableReason
            } else {
                null
            }
            val nextObservedSectorCount = if (outcome == XfeederSectorOutcome.UNRELIABLE) {
                state.observedSectorCountInput
            } else {
                ""
            }
            state.copy(
                selectedOutcome = outcome,
                relatedSectorCodeInput = nextRelatedSector,
                selectedUnreliableReason = nextUnreliableReason,
                observedSectorCountInput = nextObservedSectorCount,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onRelatedSectorCodeChanged(value: String) {
        mutableState.update { state ->
            state.copy(
                relatedSectorCodeInput = value,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onUnreliableReasonSelected(reason: XfeederUnreliableReason?) {
        mutableState.update { state ->
            state.copy(
                selectedUnreliableReason = reason,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onObservedSectorCountChanged(value: String) {
        val sanitized = value.filter { it.isDigit() }.take(2)
        mutableState.update { state ->
            state.copy(
                observedSectorCountInput = sanitized,
                hasUnsavedChanges = true,
                completionGuardMessage = null,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun onSaveSummaryClicked() {
        val current = mutableState.value
        val session = current.session ?: return
        if (current.isSavingSummary || !current.hasUnsavedChanges) return
        val completionValidationMessage = validateCompletionRequirements(
            status = current.selectedStatus,
            session = session,
            outcome = current.selectedOutcome,
            closureEvidence = buildClosureEvidence(current)
        )
        if (completionValidationMessage != null) {
            mutableState.update { state ->
                state.copy(
                    completionGuardMessage = completionValidationMessage
                )
            }
            return
        }

        viewModelScope.launch {
            mutableState.update { state ->
                state.copy(isSavingSummary = true, errorMessage = null, infoMessage = null)
            }
            runCatching {
                updateXfeederSessionSummaryUseCase(
                    sessionId = session.id,
                    status = mutableState.value.selectedStatus,
                    sectorOutcome = mutableState.value.selectedOutcome,
                    closureEvidence = buildClosureEvidence(mutableState.value),
                    notes = mutableState.value.notesInput.trim(),
                    resultSummary = mutableState.value.resultSummaryInput.trim()
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        isSavingSummary = false,
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_xfeeder_save_summary)
                    )
                }
            }.onSuccess {
                mutableState.update { state ->
                    state.copy(
                        isSavingSummary = false,
                        hasUnsavedChanges = false,
                        completionGuardMessage = null,
                        infoMessage = uiStrings.get(R.string.info_xfeeder_summary_saved)
                    )
                }
            }
        }
    }

    fun onCreateReportDraftClicked() {
        val current = mutableState.value
        val currentSession = current.session ?: return
        if (current.isCreatingDraft) return

        viewModelScope.launch {
            mutableState.update { state ->
                state.copy(
                    isCreatingDraft = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            runCatching {
                openOrCreateGuidedSessionReportDraftUseCase(
                    siteId = currentSession.siteId,
                    originSessionId = currentSession.id,
                    originSectorId = currentSession.sectorId
                )
            }.onFailure { throwable ->
                mutableState.update { state ->
                    state.copy(
                        isCreatingDraft = false,
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_xfeeder_create_report_draft)
                    )
                }
            }.onSuccess { result ->
                _events.tryEmit(XfeederGuidedSessionEvent.OpenDraft(result.draft.id))
                mutableState.update { state ->
                    state.copy(
                        isCreatingDraft = false,
                        infoMessage = if (result.created) {
                            uiStrings.get(R.string.info_local_draft_created)
                        } else {
                            uiStrings.get(R.string.info_xfeeder_opened_linked_draft)
                        }
                    )
                }
            }
        }
    }

    private fun observeContext() {
        viewModelScope.launch {
            combine(
                observeSiteDetailUseCase(siteId),
                observeSectorXfeederSessionHistoryUseCase(siteId, sectorId)
            ) { site, history ->
                mutableState.update { current ->
                    val siteName = site?.name.orEmpty()
                    val siteCode = site?.externalCode.orEmpty()
                    val sector = site?.sectors?.firstOrNull { it.id == sectorId }
                    val sectorCode = sector?.code.orEmpty()
                    val selectedSessionId = current.latestSessionId?.takeIf { selected ->
                        history.any { it.id == selected }
                    } ?: history.firstOrNull()?.id
                    val selectedSession = history.firstOrNull { it.id == selectedSessionId }
                    val shouldHydrate = selectedSession != null &&
                        (!current.hasUnsavedChanges || current.session?.id != selectedSession.id)

                    current.copy(
                        isLoading = false,
                        siteLabel = listOf(siteName, siteCode).filter { it.isNotBlank() }.joinToString(" - "),
                        sectorCode = sectorCode,
                        sessionHistory = history,
                        latestSessionId = selectedSessionId,
                        session = selectedSession,
                        selectedStatus = if (shouldHydrate) {
                            selectedSession.status
                        } else {
                            current.selectedStatus
                        },
                        selectedOutcome = if (shouldHydrate) {
                            selectedSession.sectorOutcome
                        } else {
                            current.selectedOutcome
                        },
                        notesInput = if (shouldHydrate) selectedSession.notes else current.notesInput,
                        resultSummaryInput = if (shouldHydrate) {
                            selectedSession.resultSummary
                        } else {
                            current.resultSummaryInput
                        },
                        relatedSectorCodeInput = if (shouldHydrate) {
                            selectedSession.closureEvidence.relatedSectorCode
                        } else {
                            current.relatedSectorCodeInput
                        },
                        selectedUnreliableReason = if (shouldHydrate) {
                            selectedSession.closureEvidence.unreliableReason
                        } else {
                            current.selectedUnreliableReason
                        },
                        observedSectorCountInput = if (shouldHydrate) {
                            selectedSession.closureEvidence.observedSectorCount?.toString().orEmpty()
                        } else {
                            current.observedSectorCountInput
                        },
                        hasUnsavedChanges = if (shouldHydrate) false else current.hasUnsavedChanges,
                        completionGuardMessage = if (shouldHydrate) null else current.completionGuardMessage,
                        errorMessage = if (site == null || sector == null) {
                            uiStrings.get(R.string.error_xfeeder_sector_not_found)
                        } else {
                            current.errorMessage
                        }
                    )
                }
            }
                .catch { throwable ->
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: uiStrings.get(R.string.error_xfeeder_observe_session)
                        )
                    }
                }
                .collect {}
        }
    }

    private fun hasIncompleteRequiredSteps(session: XfeederGuidedSession): Boolean {
        return session.steps.any { step ->
            step.required && step.status != XfeederStepStatus.DONE
        }
    }

    private fun buildClosureEvidence(state: XfeederGuidedSessionUiState): XfeederClosureEvidence {
        return XfeederClosureEvidence(
            relatedSectorCode = state.relatedSectorCodeInput.trim(),
            unreliableReason = state.selectedUnreliableReason,
            observedSectorCount = state.observedSectorCountInput.toIntOrNull()
        )
    }

    private fun validateCompletionRequirements(
        status: XfeederSessionStatus,
        session: XfeederGuidedSession,
        outcome: XfeederSectorOutcome,
        closureEvidence: XfeederClosureEvidence
    ): String? {
        if (status != XfeederSessionStatus.COMPLETED) return null

        if (hasIncompleteRequiredSteps(session)) {
            return uiStrings.get(R.string.error_xfeeder_complete_requires_required_steps)
        }

        return when (validateClosureEvidenceForFinalization(outcome, closureEvidence)) {
            XfeederClosureEvidenceIssue.RELATED_SECTOR_REQUIRED -> {
                uiStrings.get(R.string.error_xfeeder_closure_requires_related_sector)
            }

            XfeederClosureEvidenceIssue.UNRELIABLE_REASON_REQUIRED -> {
                uiStrings.get(R.string.error_xfeeder_closure_requires_unreliable_reason)
            }

            XfeederClosureEvidenceIssue.OBSERVED_SECTOR_COUNT_INVALID -> {
                uiStrings.get(R.string.error_xfeeder_closure_requires_observed_sector_count)
            }

            null -> null
        }
    }

    private fun selectSessionById(sessionId: String) {
        mutableState.update { state ->
            val selected = state.sessionHistory.firstOrNull { it.id == sessionId } ?: return@update state
            state.copy(
                latestSessionId = sessionId,
                session = selected,
                selectedStatus = selected.status,
                selectedOutcome = selected.sectorOutcome,
                relatedSectorCodeInput = selected.closureEvidence.relatedSectorCode,
                selectedUnreliableReason = selected.closureEvidence.unreliableReason,
                observedSectorCountInput = selected.closureEvidence.observedSectorCount?.toString().orEmpty(),
                notesInput = selected.notes,
                resultSummaryInput = selected.resultSummary,
                hasUnsavedChanges = false,
                completionGuardMessage = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }
}

sealed interface XfeederGuidedSessionEvent {
    data class OpenDraft(val draftId: String) : XfeederGuidedSessionEvent
}
