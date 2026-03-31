package com.quartz.platform.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.domain.model.GuidedSessionClosureProjection
import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.RetClosureProjection
import com.quartz.platform.domain.model.RetReferenceAltitudeSourceState
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.model.RetStepCode
import com.quartz.platform.domain.model.RetStepStatus
import com.quartz.platform.domain.model.ReportClosureProjection
import com.quartz.platform.domain.model.RetGuidedSession
import com.quartz.platform.domain.model.RetGuidedStep
import com.quartz.platform.domain.model.XfeederClosureEvidence
import com.quartz.platform.domain.model.XfeederGeospatialPolicy
import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.XfeederGuidedStep
import com.quartz.platform.domain.model.XfeederReferenceAltitudeSourceState
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.domain.model.XfeederUnreliableReason
import com.quartz.platform.domain.model.ThroughputReportClosureProjection
import com.quartz.platform.domain.model.ThroughputMetrics
import com.quartz.platform.domain.repository.PerformanceSessionRepository
import com.quartz.platform.domain.repository.RetGuidedSessionRepository
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveSiteReportClosureProjectionsUseCaseTest {

    @Test
    fun `usecase merges xfeeder and ret closure projections for report view`() = runTest {
        val useCase = ObserveSiteReportClosureProjectionsUseCase(
            xfeederRepository = FakeXfeederRepository(
                projections = listOf(
                    GuidedSessionClosureProjection(
                        sessionId = "xf-1",
                        siteId = "site-1",
                        sectorId = "sector-a",
                        sectorCode = "A",
                        sectorOutcome = XfeederSectorOutcome.CROSSED,
                        relatedSectorCode = "B",
                        unreliableReason = null,
                        observedSectorCount = null,
                        updatedAtEpochMillis = 20L
                    )
                )
            ),
            retRepository = FakeRetRepository(
                projections = listOf(
                    RetClosureProjection(
                        sessionId = "ret-1",
                        siteId = "site-1",
                        sectorId = "sector-b",
                        sectorCode = "B",
                        sessionStatus = RetSessionStatus.COMPLETED,
                        resultOutcome = RetResultOutcome.PASS,
                        requiredStepCount = 3,
                        completedRequiredStepCount = 3,
                        measurementZoneRadiusMeters = 70,
                        proximityModeEnabled = true,
                        resultSummary = "Conforme",
                        updatedAtEpochMillis = 25L
                    )
                )
            ),
            performanceSessionRepository = FakePerformanceRepository(
                sessions = listOf(
                    PerformanceSession(
                        id = "perf-1",
                        siteId = "site-1",
                        siteCode = "SITE-1",
                        workflowType = PerformanceWorkflowType.THROUGHPUT,
                        operatorName = "Op-A",
                        technology = "4G",
                        status = PerformanceSessionStatus.COMPLETED,
                        prerequisiteNetworkReady = true,
                        prerequisiteBatterySufficient = true,
                        prerequisiteLocationReady = true,
                        throughputMetrics = ThroughputMetrics(
                            downloadMbps = 48.5,
                            uploadMbps = 12.7,
                            latencyMs = 28
                        ),
                        qosRunSummary = QosRunSummary(),
                        notes = "",
                        resultSummary = "OK local",
                        createdAtEpochMillis = 1L,
                        updatedAtEpochMillis = 30L,
                        completedAtEpochMillis = 30L,
                        steps = listOf(
                            com.quartz.platform.domain.model.PerformanceGuidedStep(
                                code = PerformanceStepCode.PRECONDITIONS_CHECK,
                                required = true,
                                status = PerformanceStepStatus.DONE
                            )
                        )
                    )
                )
            )
        )

        val projections = useCase("site-1")

        projections.collect { items ->
            assertThat(items).hasSize(3)
            assertThat(items.map(ReportClosureProjection::sessionId)).containsExactly(
                "perf-1",
                "ret-1",
                "xf-1"
            ).inOrder()
            assertThat(items.first()).isInstanceOf(ThroughputReportClosureProjection::class.java)
            return@collect
        }
    }

    private class FakePerformanceRepository(
        private val sessions: List<PerformanceSession>
    ) : PerformanceSessionRepository {
        override fun observeSiteSessionHistory(siteId: String): Flow<List<PerformanceSession>> {
            return flowOf(sessions.filter { it.siteId == siteId })
        }

        override fun observeLatestSiteSession(
            siteId: String,
            workflowType: PerformanceWorkflowType
        ): Flow<PerformanceSession?> = flowOf(
            sessions.firstOrNull { it.siteId == siteId && it.workflowType == workflowType }
        )

        override suspend fun createSession(
            siteId: String,
            siteCode: String,
            workflowType: PerformanceWorkflowType,
            operatorName: String?,
            technology: String?
        ): PerformanceSession = sessions.first()

        override suspend fun updateStepStatus(
            sessionId: String,
            stepCode: PerformanceStepCode,
            status: PerformanceStepStatus
        ) = Unit

        override suspend fun updateSessionExecution(
            sessionId: String,
            status: PerformanceSessionStatus,
            prerequisiteNetworkReady: Boolean,
            prerequisiteBatterySufficient: Boolean,
            prerequisiteLocationReady: Boolean,
            throughputMetrics: ThroughputMetrics,
            qosRunSummary: QosRunSummary,
            notes: String,
            resultSummary: String
        ) = Unit
    }

    private class FakeXfeederRepository(
        private val projections: List<GuidedSessionClosureProjection>
    ) : XfeederGuidedSessionRepository {
        override fun observeSectorSessionHistory(
            siteId: String,
            sectorId: String
        ): Flow<List<XfeederGuidedSession>> = flowOf(emptyList())

        override fun observeLatestSectorSession(
            siteId: String,
            sectorId: String
        ): Flow<XfeederGuidedSession?> = flowOf(null)

        override fun observeSiteClosureProjections(siteId: String): Flow<List<GuidedSessionClosureProjection>> {
            return flowOf(projections.filter { it.siteId == siteId })
        }

        override suspend fun createSession(
            siteId: String,
            sectorId: String,
            sectorCode: String
        ): XfeederGuidedSession {
            return XfeederGuidedSession(
                id = "unused",
                siteId = siteId,
                sectorId = sectorId,
                sectorCode = sectorCode,
                measurementZoneRadiusMeters = XfeederGeospatialPolicy.DEFAULT_MEASUREMENT_ZONE_RADIUS_METERS,
                measurementZoneExtensionReason = "",
                proximityModeEnabled = false,
                proximityReferenceAltitudeMeters = null,
                proximityReferenceAltitudeSource = XfeederReferenceAltitudeSourceState.UNAVAILABLE,
                status = XfeederSessionStatus.CREATED,
                sectorOutcome = XfeederSectorOutcome.NOT_TESTED,
                closureEvidence = XfeederClosureEvidence(
                    relatedSectorCode = "",
                    unreliableReason = null,
                    observedSectorCount = null
                ),
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = 0L,
                updatedAtEpochMillis = 0L,
                completedAtEpochMillis = null,
                steps = listOf(
                    XfeederGuidedStep(
                        code = XfeederStepCode.PRECONDITION_NETWORK_READY,
                        required = true,
                        status = XfeederStepStatus.TODO
                    )
                )
            )
        }

        override suspend fun updateStepStatus(
            sessionId: String,
            stepCode: XfeederStepCode,
            status: XfeederStepStatus
        ) = Unit

        override suspend fun updateSessionSummary(
            sessionId: String,
            status: XfeederSessionStatus,
            sectorOutcome: XfeederSectorOutcome,
            closureEvidence: XfeederClosureEvidence,
            notes: String,
            resultSummary: String
        ) = Unit

        override suspend fun updateSessionGeospatialContext(
            sessionId: String,
            measurementZoneRadiusMeters: Int,
            measurementZoneExtensionReason: String,
            proximityModeEnabled: Boolean,
            proximityReferenceAltitudeMeters: Double?,
            proximityReferenceAltitudeSource: XfeederReferenceAltitudeSourceState
        ) = Unit
    }

    private class FakeRetRepository(
        private val projections: List<RetClosureProjection>
    ) : RetGuidedSessionRepository {
        override fun observeSectorSessionHistory(
            siteId: String,
            sectorId: String
        ): Flow<List<RetGuidedSession>> = flowOf(emptyList())

        override fun observeLatestSectorSession(
            siteId: String,
            sectorId: String
        ): Flow<RetGuidedSession?> = flowOf(null)

        override fun observeSiteClosureProjections(siteId: String): Flow<List<RetClosureProjection>> {
            return flowOf(projections.filter { it.siteId == siteId })
        }

        override suspend fun createSession(
            siteId: String,
            sectorId: String,
            sectorCode: String
        ): RetGuidedSession {
            return RetGuidedSession(
                id = "unused-ret",
                siteId = siteId,
                sectorId = sectorId,
                sectorCode = sectorCode,
                measurementZoneRadiusMeters = 70,
                measurementZoneExtensionReason = "",
                proximityModeEnabled = false,
                proximityReferenceAltitudeMeters = null,
                proximityReferenceAltitudeSource = RetReferenceAltitudeSourceState.UNAVAILABLE,
                status = RetSessionStatus.CREATED,
                resultOutcome = RetResultOutcome.NOT_RUN,
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = 0L,
                updatedAtEpochMillis = 0L,
                completedAtEpochMillis = null,
                steps = listOf(
                    RetGuidedStep(
                        code = RetStepCode.CALIBRATION_PRECHECK,
                        required = true,
                        status = RetStepStatus.TODO
                    )
                )
            )
        }

        override suspend fun updateStepStatus(
            sessionId: String,
            stepCode: RetStepCode,
            status: RetStepStatus
        ) = Unit

        override suspend fun updateSessionSummary(
            sessionId: String,
            status: RetSessionStatus,
            resultOutcome: RetResultOutcome,
            notes: String,
            resultSummary: String
        ) = Unit

        override suspend fun updateSessionGeospatialContext(
            sessionId: String,
            measurementZoneRadiusMeters: Int,
            measurementZoneExtensionReason: String,
            proximityModeEnabled: Boolean,
            proximityReferenceAltitudeMeters: Double?,
            proximityReferenceAltitudeSource: RetReferenceAltitudeSourceState
        ) = Unit
    }
}
