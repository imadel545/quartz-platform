package com.quartz.platform.domain.usecase

import com.quartz.platform.domain.model.SiteReportListItem
import com.quartz.platform.domain.repository.ReportDraftRepository
import com.quartz.platform.domain.repository.SyncRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSiteReportListUseCase @Inject constructor(
    private val reportDraftRepository: ReportDraftRepository,
    private val syncRepository: SyncRepository
) {
    operator fun invoke(siteId: String): Flow<List<SiteReportListItem>> {
        return reportDraftRepository.listDraftsBySite(siteId)
            .flatMapLatest { drafts ->
                if (drafts.isEmpty()) return@flatMapLatest flowOf(emptyList())

                combine(
                    drafts.map { draft ->
                        syncRepository.observeSyncTrace(draft.id)
                            .map { syncTrace ->
                                SiteReportListItem(
                                    draftId = draft.id,
                                    siteId = draft.siteId,
                                    title = draft.title,
                                    revision = draft.revision,
                                    updatedAtEpochMillis = draft.updatedAtEpochMillis,
                                    syncTrace = syncTrace
                                )
                            }
                    }
                ) { items -> items.toList() }
            }
    }
}
