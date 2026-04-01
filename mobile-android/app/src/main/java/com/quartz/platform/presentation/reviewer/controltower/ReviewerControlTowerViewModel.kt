package com.quartz.platform.presentation.reviewer.controltower

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.platform.R
import com.quartz.platform.core.text.UiStrings
import com.quartz.platform.domain.usecase.ObserveReviewerControlTowerUseCase
import com.quartz.platform.domain.usecase.RetryControlTowerFailedSyncUseCase
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
class ReviewerControlTowerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val observeReviewerControlTowerUseCase: ObserveReviewerControlTowerUseCase,
    private val retryControlTowerFailedSyncUseCase: RetryControlTowerFailedSyncUseCase,
    private val uiStrings: UiStrings
) : ViewModel() {

    private val restoredFilter = ReviewerControlTowerFilter.fromPersistedNameOrDefault(
        savedStateHandle[STATE_CONTROL_TOWER_SELECTED_FILTER]
    )
    private val restoredGrouping = ReviewerControlTowerGrouping.fromPersistedNameOrDefault(
        savedStateHandle[STATE_CONTROL_TOWER_SELECTED_GROUPING]
    )

    private val _uiState = MutableStateFlow(
        ReviewerControlTowerUiState(
            selectedFilter = restoredFilter,
            selectedGrouping = restoredGrouping
        )
    )
    val uiState: StateFlow<ReviewerControlTowerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ReviewerControlTowerEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    init {
        observeControlTower()
    }

    fun onFilterSelected(filter: ReviewerControlTowerFilter) {
        _uiState.update { state ->
            if (state.selectedFilter == filter) {
                state
            } else {
                savedStateHandle[STATE_CONTROL_TOWER_SELECTED_FILTER] = filter.name
                state.copy(selectedFilter = filter)
            }
        }
    }

    fun onGroupingSelected(grouping: ReviewerControlTowerGrouping) {
        _uiState.update { state ->
            if (state.selectedGrouping == grouping) {
                state
            } else {
                savedStateHandle[STATE_CONTROL_TOWER_SELECTED_GROUPING] = grouping.name
                state.copy(selectedGrouping = grouping)
            }
        }
    }

    fun onOpenDraftClicked(draftId: String) {
        _events.tryEmit(ReviewerControlTowerEvent.OpenDraft(draftId))
    }

    fun onOpenSiteClicked(siteId: String) {
        _events.tryEmit(ReviewerControlTowerEvent.OpenSite(siteId))
    }

    fun onOpenTopPriorityClicked() {
        val draftId = _uiState.value.topPriorityDraftId ?: return
        _events.tryEmit(ReviewerControlTowerEvent.OpenDraft(draftId))
    }

    fun onRetryDraftSyncClicked(draftId: String) {
        val state = _uiState.value
        val item = state.items.firstOrNull { it.draftId == draftId } ?: return
        if (item.syncTrace.state != com.quartz.platform.domain.model.ReportSyncState.FAILED) return
        if (draftId in state.retryingDraftIds) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    retryingDraftIds = it.retryingDraftIds + draftId,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            runCatching { retryControlTowerFailedSyncUseCase(listOf(draftId)) }
                .onSuccess { queued ->
                    _uiState.update {
                        it.copy(
                            infoMessage = if (queued > 0) {
                                uiStrings.get(R.string.info_sync_relaunched)
                            } else {
                                uiStrings.get(R.string.info_retry_only_failed)
                            }
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = throwable.message ?: uiStrings.get(R.string.error_retry_sync))
                    }
                }
            _uiState.update { it.copy(retryingDraftIds = it.retryingDraftIds - draftId) }
        }
    }

    fun onRetryFailedVisibleSyncClicked() {
        val state = _uiState.value
        if (state.isBulkRetryInProgress) return
        val draftIds = state.filteredItems
            .filter { item -> item.syncTrace.state == com.quartz.platform.domain.model.ReportSyncState.FAILED }
            .map { item -> item.draftId }
            .distinct()
        if (draftIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBulkRetryInProgress = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            runCatching { retryControlTowerFailedSyncUseCase(draftIds) }
                .onSuccess { queued ->
                    _uiState.update {
                        it.copy(
                            infoMessage = uiStrings.get(
                                R.string.info_control_tower_bulk_retry_result,
                                queued,
                                draftIds.size
                            )
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = throwable.message ?: uiStrings.get(R.string.error_retry_sync))
                    }
                }
            _uiState.update { it.copy(isBulkRetryInProgress = false) }
        }
    }

    private fun observeControlTower() {
        viewModelScope.launch {
            observeReviewerControlTowerUseCase()
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: uiStrings.get(
                                R.string.error_load_reviewer_control_tower
                            )
                        )
                    }
                }
                .collect { snapshot ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            items = snapshot.items,
                            summary = snapshot.summary,
                            retryingDraftIds = state.retryingDraftIds.intersect(snapshot.items.map { it.draftId }.toSet()),
                            errorMessage = null
                        )
                    }
                }
        }
    }
}
