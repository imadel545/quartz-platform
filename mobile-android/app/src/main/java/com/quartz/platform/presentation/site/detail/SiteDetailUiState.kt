package com.quartz.platform.presentation.site.detail

import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.SiteDetail

data class SiteDetailUiState(
    val isLoading: Boolean = true,
    val site: SiteDetail? = null,
    val drafts: List<ReportDraft> = emptyList(),
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val isCreatingDraft: Boolean = false
)
