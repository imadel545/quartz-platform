package com.quartz.platform.data.local.mapper

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.data.local.entity.XfeederSessionEntity
import com.quartz.platform.data.local.entity.XfeederStepEntity
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import org.junit.Test

class XfeederSessionMapperTest {

    @Test
    fun `toDomain maps typed session and steps`() {
        val entity = XfeederSessionEntity(
            id = "session-1",
            siteId = "site-1",
            sectorId = "sector-1",
            sectorCode = "S0",
            status = XfeederSessionStatus.IN_PROGRESS.name,
            sectorOutcome = XfeederSectorOutcome.CROSSED.name,
            closureRelatedSectorCode = "S1",
            closureUnreliableReason = null,
            closureObservedSectorCount = null,
            notes = "Crossing seen",
            resultSummary = "Shell result",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
            completedAtEpochMillis = null
        )
        val steps = listOf(
            XfeederStepEntity(
                sessionId = "session-1",
                code = XfeederStepCode.PRECONDITION_NETWORK_READY.name,
                required = true,
                status = XfeederStepStatus.DONE.name,
                displayOrder = 0
            )
        )

        val domain = entity.toDomain(steps)

        assertThat(domain.status).isEqualTo(XfeederSessionStatus.IN_PROGRESS)
        assertThat(domain.sectorOutcome).isEqualTo(XfeederSectorOutcome.CROSSED)
        assertThat(domain.closureEvidence.relatedSectorCode).isEqualTo("S1")
        assertThat(domain.steps.single().status).isEqualTo(XfeederStepStatus.DONE)
    }
}
