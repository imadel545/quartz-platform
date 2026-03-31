package com.quartz.platform.presentation.reviewer.controltower

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.platform.R
import com.quartz.platform.core.text.UiStrings
import com.quartz.platform.domain.usecase.ObserveReviewerControlTowerUseCase
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
    private val uiStrings: UiStrings
) : ViewModel() {

    private val restoredFilter = ReviewerControlTowerFilter.fromPersistedNameOrDefault(
        savedStateHandle[STATE_CONTROL_TOWER_SELECTED_FILTER]
    )

    private val _uiState = MutableStateFlow(
        ReviewerControlTowerUiState(
            selectedFilter = restoredFilter
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

    fun onOpenDraftClicked(draftId: String) {
        _events.tryEmit(ReviewerControlTowerEvent.OpenDraft(draftId))
    }

    fun onOpenSiteClicked(siteId: String) {
        _events.tryEmit(ReviewerControlTowerEvent.OpenSite(siteId))
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
                            errorMessage = null
                        )
                    }
                }
        }
    }
}
