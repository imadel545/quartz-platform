package com.quartz.platform.presentation.report.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.platform.R
import com.quartz.platform.core.text.UiStrings
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.usecase.ObserveSiteReportListUseCase
import com.quartz.platform.domain.usecase.RetryFailedReportDraftSyncUseCase
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ReportListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val observeSiteReportListUseCase: ObserveSiteReportListUseCase,
    private val retryFailedReportDraftSyncUseCase: RetryFailedReportDraftSyncUseCase,
    private val uiStrings: UiStrings
) : ViewModel() {

    private val siteId: String = checkNotNull(savedStateHandle[QuartzDestination.ReportList.ARG_SITE_ID])
    private val restoredFilter: ReportListFilter = ReportListFilter.fromPersistedNameOrDefault(
        savedStateHandle[STATE_SELECTED_FILTER]
    )

    private val _uiState = MutableStateFlow(
        ReportListUiState(
            siteId = siteId,
            selectedFilter = restoredFilter
        )
    )
    val uiState: StateFlow<ReportListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ReportListEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    init {
        observeReportList()
    }

    fun onOpenDraftClicked(draftId: String) {
        _events.tryEmit(ReportListEvent.OpenDraft(draftId))
    }

    fun onFilterSelected(filter: ReportListFilter) {
        _uiState.update { state ->
            if (state.selectedFilter == filter) {
                state
            } else {
                savedStateHandle[STATE_SELECTED_FILTER] = filter.name
                state.copy(selectedFilter = filter)
            }
        }
    }

    fun onRetryFailedSyncClicked(draftId: String) {
        val current = _uiState.value
        val target = current.reports.firstOrNull { it.draftId == draftId } ?: return
        if (target.syncState != ReportSyncState.FAILED) return
        if (draftId in current.retryingDraftIds) return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    retryingDraftIds = state.retryingDraftIds + draftId,
                    errorMessage = null,
                    infoMessage = null
                )
            }

            runCatching { retryFailedReportDraftSyncUseCase(draftId) }
                .onSuccess { queued ->
                    _uiState.update { state ->
                        state.copy(
                            infoMessage = if (queued) {
                                uiStrings.get(R.string.info_sync_relaunched)
                            } else {
                                uiStrings.get(R.string.info_retry_only_failed)
                            }
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            errorMessage = throwable.message ?: uiStrings.get(R.string.error_retry_sync)
                        )
                    }
                }

            _uiState.update { state ->
                state.copy(retryingDraftIds = state.retryingDraftIds - draftId)
            }
        }
    }

    private fun observeReportList() {
        viewModelScope.launch {
            observeSiteReportListUseCase(siteId)
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: uiStrings.get(R.string.error_load_local_reports)
                        )
                    }
                }
                .collect { reports ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            reports = reports,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}

internal const val STATE_SELECTED_FILTER = "state_selected_filter"

sealed interface ReportListEvent {
    data class OpenDraft(val draftId: String) : ReportListEvent
}
