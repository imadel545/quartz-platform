package com.quartz.platform.presentation.site.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.platform.R
import com.quartz.platform.core.text.UiStrings
import com.quartz.platform.domain.usecase.BootstrapDemoSiteSnapshotUseCase
import com.quartz.platform.domain.usecase.ObserveSiteListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SiteListViewModel @Inject constructor(
    private val observeSiteListUseCase: ObserveSiteListUseCase,
    private val bootstrapDemoSiteSnapshotUseCase: BootstrapDemoSiteSnapshotUseCase,
    private val uiStrings: UiStrings
) : ViewModel() {

    private val _uiState = MutableStateFlow(SiteListUiState())
    val uiState: StateFlow<SiteListUiState> = _uiState.asStateFlow()

    init {
        observeSites()
    }

    fun onLoadDemoSnapshotClicked() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isBootstrappingDemo = true, errorMessage = null, infoMessage = null)
            }

            runCatching { bootstrapDemoSiteSnapshotUseCase() }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isBootstrappingDemo = false,
                            infoMessage = uiStrings.get(R.string.info_demo_snapshot_loaded)
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isBootstrappingDemo = false,
                            errorMessage = throwable.message ?: uiStrings.get(R.string.error_load_demo_snapshot)
                        )
                    }
                }
        }
    }

    private fun observeSites() {
        viewModelScope.launch {
            observeSiteListUseCase()
                .onStart {
                    _uiState.update { state ->
                        state.copy(isLoading = true, errorMessage = null)
                    }
                }
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: uiStrings.get(R.string.error_load_cached_sites)
                        )
                    }
                }
                .collect { sites ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            sites = sites,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}
