package com.quartz.platform.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosFamilyExecutionResult
import com.quartz.platform.domain.model.QosFamilyExecutionStatus
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.QosTestFamily
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.RetClosureProjection
import com.quartz.platform.domain.model.RetReferenceAltitudeSourceState
import com.quartz.platform.domain.model.RetResultOutcome
import com.quartz.platform.domain.model.RetSessionStatus
import com.quartz.platform.domain.model.RetStepCode
import com.quartz.platform.domain.model.RetStepStatus
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.domain.model.ThroughputMetrics
import com.quartz.platform.domain.model.XfeederClosureEvidence
import com.quartz.platform.domain.model.XfeederGeospatialPolicy
import com.quartz.platform.domain.model.XfeederGuidedSession
import com.quartz.platform.domain.model.XfeederGuidedStep
import com.quartz.platform.domain.model.XfeederReferenceAltitudeSourceState
import com.quartz.platform.domain.model.XfeederSectorOutcome
import com.quartz.platform.domain.model.XfeederSessionStatus
import com.quartz.platform.domain.model.XfeederStepCode
import com.quartz.platform.domain.model.XfeederStepStatus
import com.quartz.platform.domain.model.SiteReportListItem
import com.quartz.platform.domain.repository.ReportDraftRepository
import com.quartz.platform.domain.repository.PerformanceSessionRepository
import com.quartz.platform.domain.repository.RetGuidedSessionRepository
import com.quartz.platform.domain.repository.SyncRepository
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSiteReportListUseCaseTest {

    @Test
    fun `observe use case combines local drafts and sync states`() = runTest {
        val drafts = listOf(
            ReportDraft(
                id = "draft-1",
                siteId = "site-1",
                title = "Rapport 1",
                observation = "",
                revision = 1,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 10L
            ),
            ReportDraft(
                id = "draft-2",
                siteId = "site-1",
                title = "Rapport 2",
                observation = "",
                revision = 2,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 20L
            )
        )
        val reportRepository = FakeReportDraftRepository(drafts)
        val syncRepository = FakeSyncRepository(
            mapOf(
                "draft-1" to ReportSyncTrace(
                    state = ReportSyncState.LOCAL_ONLY,
                    lastAttemptAtEpochMillis = null,
                    retryCount = 0,
                    failureReason = null
                ),
                "draft-2" to ReportSyncTrace(
                    state = ReportSyncState.FAILED,
                    lastAttemptAtEpochMillis = 42L,
                    retryCount = 3,
                    failureReason = "Timeout"
                )
            )
        )
        val useCase = ObserveSiteReportListUseCase(
            reportDraftRepository = reportRepository,
            syncRepository = syncRepository,
            observeSiteReportClosureProjectionsUseCase = ObserveSiteReportClosureProjectionsUseCase(
                xfeederRepository = FakeXfeederGuidedSessionRepository(),
                retRepository = FakeRetGuidedSessionRepository(),
                performanceSessionRepository = FakePerformanceSessionRepository()
            )
        )

        val emissions = mutableListOf<List<SiteReportListItem>>()
        val job = launch {
            useCase("site-1").take(2).toList(emissions)
        }

        advanceUntilIdle()
        syncRepository.updateState(
            "draft-2",
            ReportSyncTrace(
                state = ReportSyncState.PENDING,
                lastAttemptAtEpochMillis = 43L,
                retryCount = 3,
                failureReason = null
            )
        )
        advanceUntilIdle()
        job.join()

        assertThat(emissions).hasSize(2)
        assertThat(emissions[0].map { it.syncState }).containsExactly(
            ReportSyncState.LOCAL_ONLY,
            ReportSyncState.FAILED
        ).inOrder()
        assertThat(emissions[1].map { it.syncState }).containsExactly(
            ReportSyncState.LOCAL_ONLY,
            ReportSyncState.PENDING
        ).inOrder()
        assertThat(emissions[0].last().syncTrace.failureReason).isEqualTo("Timeout")
    }

    @Test
    fun `observe use case exposes workflow typed closure summary for guided drafts`() = runTest {
        val drafts = listOf(
            ReportDraft(
                id = "draft-ret",
                siteId = "site-1",
                originSessionId = "ret-session-1",
                originSectorId = "sector-r1",
                originWorkflowType = ReportDraftOriginWorkflowType.RET,
                title = "RET draft",
                observation = "",
                revision = 1,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 10L
            ),
            ReportDraft(
                id = "draft-local",
                siteId = "site-1",
                title = "Local draft",
                observation = "",
                revision = 1,
                createdAtEpochMillis = 2L,
                updatedAtEpochMillis = 11L
            )
        )
        val reportRepository = FakeReportDraftRepository(drafts)
        val syncRepository = FakeSyncRepository(
            mapOf(
                "draft-ret" to ReportSyncTrace.localOnly(),
                "draft-local" to ReportSyncTrace.localOnly()
            )
        )
        val useCase = ObserveSiteReportListUseCase(
            reportDraftRepository = reportRepository,
            syncRepository = syncRepository,
            observeSiteReportClosureProjectionsUseCase = ObserveSiteReportClosureProjectionsUseCase(
                xfeederRepository = FakeXfeederGuidedSessionRepository(),
                retRepository = FakeRetGuidedSessionRepository(
                    projections = listOf(
                        RetClosureProjection(
                            sessionId = "ret-session-1",
                            siteId = "site-1",
                            sectorId = "sector-r1",
                            sectorCode = "R1",
                            sessionStatus = RetSessionStatus.COMPLETED,
                            resultOutcome = RetResultOutcome.PASS,
                            requiredStepCount = 3,
                            completedRequiredStepCount = 3,
                            measurementZoneRadiusMeters = 70,
                            proximityModeEnabled = true,
                            resultSummary = "Conforme",
                            updatedAtEpochMillis = 99L
                        )
                    )
                ),
                performanceSessionRepository = FakePerformanceSessionRepository()
            )
        )

        val items = useCase("site-1").take(1).toList().first()

        assertThat(items).hasSize(2)
        val guided = items.first { it.draftId == "draft-ret" }
        val nonGuided = items.first { it.draftId == "draft-local" }
        assertThat(guided.closureSummary).isNotNull()
        assertThat(nonGuided.closureSummary).isNull()
    }

    @Test
    fun `observe use case exposes performance summary fallback for non guided draft`() = runTest {
        val drafts = listOf(
            ReportDraft(
                id = "draft-local",
                siteId = "site-1",
                title = "Local draft",
                observation = "",
                revision = 1,
                createdAtEpochMillis = 2L,
                updatedAtEpochMillis = 11L
            )
        )
        val reportRepository = FakeReportDraftRepository(drafts)
        val syncRepository = FakeSyncRepository(
            mapOf("draft-local" to ReportSyncTrace.localOnly())
        )
        val useCase = ObserveSiteReportListUseCase(
            reportDraftRepository = reportRepository,
            syncRepository = syncRepository,
            observeSiteReportClosureProjectionsUseCase = ObserveSiteReportClosureProjectionsUseCase(
                xfeederRepository = FakeXfeederGuidedSessionRepository(),
                retRepository = FakeRetGuidedSessionRepository(),
                performanceSessionRepository = FakePerformanceSessionRepository(
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
                                downloadMbps = 52.1,
                                uploadMbps = 9.3,
                                latencyMs = 31
                            ),
                            qosRunSummary = QosRunSummary(),
                            notes = "",
                            resultSummary = "OK",
                            createdAtEpochMillis = 1L,
                            updatedAtEpochMillis = 100L,
                            completedAtEpochMillis = 100L,
                            steps = listOf(
                                com.quartz.platform.domain.model.PerformanceGuidedStep(
                                    code = PerformanceStepCode.PRECONDITIONS_CHECK,
                                    required = true,
                                    status = PerformanceStepStatus.DONE
                                ),
                                com.quartz.platform.domain.model.PerformanceGuidedStep(
                                    code = PerformanceStepCode.EXECUTE_TEST,
                                    required = true,
                                    status = PerformanceStepStatus.DONE
                                )
                            )
                        )
                    )
                )
            )
        )

        val items = useCase("site-1").take(1).toList().first()
        assertThat(items).hasSize(1)
        assertThat(items.first().closureSummary)
            .isInstanceOf(com.quartz.platform.domain.model.ReportListClosureSummary.Throughput::class.java)
    }

    @Test
    fun `observe use case resolves performance summary for performance linked draft`() = runTest {
        val drafts = listOf(
            ReportDraft(
                id = "draft-perf",
                siteId = "site-1",
                originSessionId = "perf-1",
                originWorkflowType = ReportDraftOriginWorkflowType.PERFORMANCE,
                title = "Perf draft",
                observation = "",
                revision = 1,
                createdAtEpochMillis = 2L,
                updatedAtEpochMillis = 11L
            )
        )
        val reportRepository = FakeReportDraftRepository(drafts)
        val syncRepository = FakeSyncRepository(
            mapOf("draft-perf" to ReportSyncTrace.localOnly())
        )
        val useCase = ObserveSiteReportListUseCase(
            reportDraftRepository = reportRepository,
            syncRepository = syncRepository,
            observeSiteReportClosureProjectionsUseCase = ObserveSiteReportClosureProjectionsUseCase(
                xfeederRepository = FakeXfeederGuidedSessionRepository(),
                retRepository = FakeRetGuidedSessionRepository(),
                performanceSessionRepository = FakePerformanceSessionRepository(
                    sessions = listOf(
                        PerformanceSession(
                            id = "perf-1",
                            siteId = "site-1",
                            siteCode = "SITE-1",
                            workflowType = PerformanceWorkflowType.QOS_SCRIPT,
                            operatorName = "Op-A",
                            technology = "4G",
                            status = PerformanceSessionStatus.COMPLETED,
                            prerequisiteNetworkReady = true,
                            prerequisiteBatterySufficient = true,
                            prerequisiteLocationReady = true,
                            throughputMetrics = ThroughputMetrics(),
                            qosRunSummary = QosRunSummary(
                                scriptId = "qos-1",
                                scriptName = "Latence + Débit",
                                configuredTechnologies = setOf("4G", "5G"),
                                selectedTestFamilies = setOf(
                                    QosTestFamily.SMS,
                                    QosTestFamily.VOLTE_CALL
                                ),
                                familyExecutionResults = listOf(
                                    QosFamilyExecutionResult(
                                        family = QosTestFamily.SMS,
                                        status = QosFamilyExecutionStatus.PASSED
                                    ),
                                    QosFamilyExecutionResult(
                                        family = QosTestFamily.VOLTE_CALL,
                                        status = QosFamilyExecutionStatus.FAILED,
                                        failureReason = "No ack"
                                    )
                                ),
                                targetTechnology = "4G",
                                iterationCount = 2,
                                successCount = 1,
                                failureCount = 1
                            ),
                            notes = "",
                            resultSummary = "OK",
                            createdAtEpochMillis = 1L,
                            updatedAtEpochMillis = 100L,
                            completedAtEpochMillis = 100L,
                            steps = listOf(
                                com.quartz.platform.domain.model.PerformanceGuidedStep(
                                    code = PerformanceStepCode.PRECONDITIONS_CHECK,
                                    required = true,
                                    status = PerformanceStepStatus.DONE
                                ),
                                com.quartz.platform.domain.model.PerformanceGuidedStep(
                                    code = PerformanceStepCode.EXECUTE_TEST,
                                    required = true,
                                    status = PerformanceStepStatus.DONE
                                )
                            )
                        )
                    )
                )
            )
        )

        val items = useCase("site-1").take(1).toList().first()
        assertThat(items).hasSize(1)
        assertThat(items.first().closureSummary)
            .isInstanceOf(com.quartz.platform.domain.model.ReportListClosureSummary.Qos::class.java)
        val qosSummary = items.first().closureSummary as com.quartz.platform.domain.model.ReportListClosureSummary.Qos
        assertThat(qosSummary.testFamilyCount).isEqualTo(2)
        assertThat(qosSummary.completedFamilyCount).isEqualTo(2)
        assertThat(qosSummary.failedFamilyCount).isEqualTo(1)
        assertThat(qosSummary.targetTechnology).isEqualTo("4G")
        assertThat(qosSummary.configuredTechnologyCount).isEqualTo(2)
        assertThat(qosSummary.targetTechnologyAligned).isTrue()
    }

    private class FakeReportDraftRepository(
        initialDrafts: List<ReportDraft>
    ) : ReportDraftRepository {
        private val draftsFlow = MutableStateFlow(initialDrafts)

        override suspend fun createDraft(
            siteId: String,
            originSessionId: String?,
            originSectorId: String?,
            originWorkflowType: ReportDraftOriginWorkflowType?
        ): ReportDraft = draftsFlow.value.first()

        override suspend fun updateDraft(draftId: String, title: String, observation: String): ReportDraft? = null

        override suspend fun findLatestLinkedDraft(
            siteId: String,
            originSessionId: String,
            originWorkflowType: ReportDraftOriginWorkflowType?
        ): ReportDraft? {
            return draftsFlow.value
                .filter { draft ->
                    draft.siteId == siteId &&
                        draft.originSessionId == originSessionId &&
                        draft.originWorkflowType == originWorkflowType
                }
                .maxByOrNull { draft -> draft.updatedAtEpochMillis }
        }

        override fun observeDraft(draftId: String): Flow<ReportDraft?> {
            return draftsFlow.map { drafts -> drafts.firstOrNull { it.id == draftId } }
        }

        override fun listDraftsBySite(siteId: String): Flow<List<ReportDraft>> {
            return draftsFlow.map { drafts -> drafts.filter { it.siteId == siteId } }
        }
    }

    private class FakeSyncRepository(
        initialStates: Map<String, ReportSyncTrace>
    ) : SyncRepository {
        private val stateFlows = initialStates.mapValues { MutableStateFlow(it.value) }

        override suspend fun enqueueReportUpload(reportDraftId: String) = Unit

        override fun observeSyncTrace(reportDraftId: String): Flow<ReportSyncTrace> {
            return stateFlows.getValue(reportDraftId)
        }

        override fun observeSyncState(reportDraftId: String): Flow<ReportSyncState> {
            return stateFlows.getValue(reportDraftId).map { it.state }
        }

        override fun observePendingJobCount(): Flow<Int> = flowOf(0)

        override suspend fun processPendingJobs(limit: Int): Int = 0

        fun updateState(reportDraftId: String, state: ReportSyncTrace) {
            stateFlows.getValue(reportDraftId).value = state
        }
    }

    private class FakeXfeederGuidedSessionRepository : XfeederGuidedSessionRepository {
        override fun observeSectorSessionHistory(
            siteId: String,
            sectorId: String
        ): Flow<List<XfeederGuidedSession>> = flowOf(emptyList())

        override fun observeLatestSectorSession(siteId: String, sectorId: String): Flow<XfeederGuidedSession?> {
            return flowOf(null)
        }

        override fun observeSiteClosureProjections(siteId: String): Flow<List<com.quartz.platform.domain.model.GuidedSessionClosureProjection>> {
            return flowOf(emptyList())
        }

        override suspend fun createSession(
            siteId: String,
            sectorId: String,
            sectorCode: String
        ): XfeederGuidedSession {
            return XfeederGuidedSession(
                id = "xf",
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
                closureEvidence = XfeederClosureEvidence("", null, null),
                notes = "",
                resultSummary = "",
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
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

    private class FakeRetGuidedSessionRepository(
        private val projections: List<RetClosureProjection> = emptyList()
    ) : RetGuidedSessionRepository {
        override fun observeSectorSessionHistory(
            siteId: String,
            sectorId: String
        ): Flow<List<com.quartz.platform.domain.model.RetGuidedSession>> = flowOf(emptyList())

        override fun observeLatestSectorSession(
            siteId: String,
            sectorId: String
        ): Flow<com.quartz.platform.domain.model.RetGuidedSession?> = flowOf(null)

        override fun observeSiteClosureProjections(siteId: String): Flow<List<RetClosureProjection>> {
            return flowOf(projections.filter { it.siteId == siteId })
        }

        override suspend fun createSession(
            siteId: String,
            sectorId: String,
            sectorCode: String
        ): com.quartz.platform.domain.model.RetGuidedSession {
            return com.quartz.platform.domain.model.RetGuidedSession(
                id = "ret",
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
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
                completedAtEpochMillis = null,
                steps = listOf(
                    com.quartz.platform.domain.model.RetGuidedStep(
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

    private class FakePerformanceSessionRepository(
        private val sessions: List<PerformanceSession> = emptyList()
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
}
