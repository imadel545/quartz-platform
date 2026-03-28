package com.quartz.platform.data.local.mapper

import com.quartz.platform.data.local.entity.ReportDraftEntity
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType

fun ReportDraftEntity.toDomain(): ReportDraft {
    return ReportDraft(
        id = id,
        siteId = siteId,
        originSessionId = originSessionId,
        originSectorId = originSectorId,
        originWorkflowType = originWorkflowType?.let(ReportDraftOriginWorkflowType::valueOf),
        title = title,
        observation = observation,
        revision = revision,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}
