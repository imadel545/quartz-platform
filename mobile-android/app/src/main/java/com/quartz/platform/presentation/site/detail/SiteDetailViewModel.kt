package com.quartz.platform.presentation.site.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.platform.R
import com.quartz.platform.core.text.UiStrings
import com.quartz.platform.presentation.navigation.QuartzDestination
import com.quartz.platform.domain.usecase.CreateReportDraftUseCase
import com.quartz.platform.domain.usecase.ObserveSiteDetailUseCase
import com.quartz.platform.domain.usecase.ObserveSiteDraftsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.launch

@HiltViewModel
class SiteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeSiteDetailUseCase: ObserveSiteDetailUseCase,
    private val observeSiteDraftsUseCase: ObserveSiteDraftsUseCase,
    private val createReportDraftUseCase: CreateReportDraftUseCase,
    private val uiStrings: UiStrings
) : ViewModel() {

    private val siteId: String = checkNotNull(savedStateHandle[QuartzDestination.SiteDetail.ARG_SITE_ID])

    private val mutableState = MutableStateFlow(SiteDetailUiState())
    val uiState: StateFlow<SiteDetailUiState> = mutableState.asStateFlow()

    private val _events = MutableSharedFlow<SiteDetailEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    init {
        observeSiteSnapshot()
    }

    fun onCreateDraftClicked() {
        if (mutableState.value.isCreatingDraft) return

        viewModelScope.launch {
            mutableState.update { state ->
                state.copy(isCreatingDraft = true, errorMessage = null, infoMessage = null)
            }
            runCatching { createReportDraftUseCase(siteId) }
                .onSuccess { draft ->
                    _events.tryEmit(SiteDetailEvent.OpenDraft(draft.id))
                    mutableState.update { state ->
                        state.copy(
                            isCreatingDraft = false,
                            infoMessage = uiStrings.get(R.string.info_local_draft_created)
                        )
                    }
                }
                .onFailure { throwable ->
                    mutableState.update { state ->
                        state.copy(
                            isCreatingDraft = false,
                            errorMessage = throwable.message ?: uiStrings.get(R.string.error_create_local_draft)
                        )
                    }
                }
        }
    }

    private fun observeSiteSnapshot() {
        viewModelScope.launch {
            combine(
                observeSiteDetailUseCase(siteId),
                observeSiteDraftsUseCase(siteId)
            ) { site, drafts ->
                SiteDetailUiState(
                    isLoading = false,
                    site = site,
                    drafts = drafts,
                    infoMessage = mutableState.value.infoMessage,
                    errorMessage = null,
                    isCreatingDraft = mutableState.value.isCreatingDraft
                )
            }
                .catch { throwable ->
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: uiStrings.get(R.string.error_load_site_detail)
                    )
                }
                .collect { state ->
                    mutableState.value = state
                }
        }
    }
}

sealed interface SiteDetailEvent {
    data class OpenDraft(val draftId: String) : SiteDetailEvent
}
