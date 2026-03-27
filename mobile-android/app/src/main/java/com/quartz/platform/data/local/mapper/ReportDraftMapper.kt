package com.quartz.platform.data.local.mapper

import com.quartz.platform.data.local.entity.ReportDraftEntity
import com.quartz.platform.domain.model.ReportDraft

fun ReportDraftEntity.toDomain(): ReportDraft {
    return ReportDraft(
        id = id,
        siteId = siteId,
        originSessionId = originSessionId,
        originSectorId = originSectorId,
        title = title,
        observation = observation,
        revision = revision,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}
