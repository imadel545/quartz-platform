package com.quartz.platform.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.quartz.platform.domain.model.NetworkStatus
import com.quartz.platform.domain.model.PerformanceSession
import com.quartz.platform.domain.model.PerformanceSessionStatus
import com.quartz.platform.domain.model.PerformanceStepCode
import com.quartz.platform.domain.model.PerformanceStepStatus
import com.quartz.platform.domain.model.PerformanceWorkflowType
import com.quartz.platform.domain.model.QosExecutionIssueCode
import com.quartz.platform.domain.model.QosFamilyExecutionResult
import com.quartz.platform.domain.model.QosFamilyExecutionStatus
import com.quartz.platform.domain.model.QosRunSummary
import com.quartz.platform.domain.model.QosTestFamily
import com.quartz.platform.domain.model.ReportDraft
import com.quartz.platform.domain.model.ReportDraftOriginWorkflowType
import com.quartz.platform.domain.model.ReportSyncState
import com.quartz.platform.domain.model.ReportSyncTrace
import com.quartz.platform.domain.model.ReviewerAttentionSignal
import com.quartz.platform.domain.model.ReviewerDraftAgeBucket
import com.quartz.platform.domain.model.ReviewerUrgencyClass
import com.quartz.platform.domain.model.ReviewerUrgencyReason
import com.quartz.platform.domain.model.SiteSummary
import com.quartz.platform.domain.model.ThroughputMetrics
import com.quartz.platform.domain.model.workflow.WorkflowStepState
import com.quartz.platform.domain.repository.PerformanceSessionRepository
import com.quartz.platform.domain.repository.ReportDraftRepository
import com.quartz.platform.domain.repository.RetGuidedSessionRepository
import com.quartz.platform.domain.repository.SiteRepository
import com.quartz.platform.domain.repository.SyncRepository
import com.quartz.platform.domain.repository.XfeederGuidedSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveReviewerControlTowerUseCaseTest {

    @Test
    fun `control tower aggregates multi-site attention and prioritizes deterministic ranking`() = runTest {
        val drafts = listOf(
            ReportDraft(
                id = "draft-qos",
                siteId = "site-1",
                originSessionId = "perf-1",
                originWorkflowType = ReportDraftOriginWorkflowType.PERFORMANCE,
                title = "QoS run",
                observation = "",
                revision = 2,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = System.currentTimeMillis() - 1_000L
            ),
            ReportDraft(
                id = "draft-failed",
                siteId = "site-2",
                title = "Draft failed sync",
                observation = "",
                revision = 1,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 0L
            )
        )
        val reportRepository = FakeReportDraftRepository(drafts)
        val syncRepository = FakeSyncRepository(
            mapOf(
                "draft-qos" to ReportSyncTrace.localOnly(),
                "draft-failed" to ReportSyncTrace(
                    state = ReportSyncState.FAILED,
                    lastAttemptAtEpochMillis = 42L,
                    retryCount = 2,
                    failureReason = "Timeout"
                )
            )
        )
        val siteRepository = FakeSiteRepository(
            listOf(
                SiteSummary(
                    id = "site-1",
                    externalCode = "QRTZ-001",
                    name = "Rabat",
                    latitude = 34.0,
                    longitude = -6.8,
                    status = "IN_SERVICE",
                    sectorsInService = 3,
                    sectorsForecast = 0,
                    indoorOnly = false,
                    updatedAtEpochMillis = 1L
                ),
                SiteSummary(
                    id = "site-2",
                    externalCode = "QRTZ-002",
                    name = "Casablanca",
                    latitude = 33.5,
                    longitude = -7.6,
                    status = "IN_SERVICE",
                    sectorsInService = 2,
                    sectorsForecast = 0,
                    indoorOnly = false,
                    updatedAtEpochMillis = 1L
                )
            )
        )
        val performanceRepository = FakePerformanceSessionRepository(
            sessions = listOf(
                PerformanceSession(
                    id = "perf-1",
                    siteId = "site-1",
                    siteCode = "QRTZ-001",
                    workflowType = PerformanceWorkflowType.QOS_SCRIPT,
                    operatorName = "Op-A",
                    technology = "4G",
                    status = PerformanceSessionStatus.COMPLETED,
                    prerequisiteNetworkReady = false,
                    prerequisiteBatterySufficient = true,
                    prerequisiteLocationReady = true,
                    observedNetworkStatus = NetworkStatus.UNAVAILABLE,
                    observedBatteryLevelPercent = 73,
                    observedLocationAvailable = true,
                    observedSignalsCapturedAtEpochMillis = 100L,
                    throughputMetrics = ThroughputMetrics(),
                    qosRunSummary = QosRunSummary(
                        scriptId = "script-1",
                        scriptName = "QoS Baseline",
                        configuredRepeatCount = 1,
                        selectedTestFamilies = setOf(QosTestFamily.SMS),
                        familyExecutionResults = listOf(
                            QosFamilyExecutionResult(
                                family = QosTestFamily.SMS,
                                status = QosFamilyExecutionStatus.FAILED,
                                failureReasonCode = QosExecutionIssueCode.NETWORK_UNAVAILABLE,
                                failureReason = "No signal"
                            )
                        ),
                        iterationCount = 1,
                        successCount = 0,
                        failureCount = 1
                    ),
                    notes = "",
                    resultSummary = "Failed",
                    createdAtEpochMillis = 1L,
                    updatedAtEpochMillis = 101L,
                    completedAtEpochMillis = 101L,
                    steps = listOf(
                        WorkflowStepState(
                            code = PerformanceStepCode.PRECONDITIONS_CHECK,
                            required = true,
                            status = PerformanceStepStatus.DONE
                        ),
                        WorkflowStepState(
                            code = PerformanceStepCode.EXECUTE_TEST,
                            required = true,
                            status = PerformanceStepStatus.DONE
                        )
                    )
                )
            )
        )

        val observeSiteReportListUseCase = ObserveSiteReportListUseCase(
            reportDraftRepository = reportRepository,
            syncRepository = syncRepository,
            observeSiteReportClosureProjectionsUseCase = ObserveSiteReportClosureProjectionsUseCase(
                xfeederRepository = EmptyXfeederRepository,
                retRepository = EmptyRetRepository,
                performanceSessionRepository = performanceRepository
            )
        )
        val useCase = ObserveReviewerControlTowerUseCase(
            reportDraftRepository = reportRepository,
            observeSiteReportListUseCase = observeSiteReportListUseCase,
            observeSiteListUseCase = ObserveSiteListUseCase(siteRepository),
            observeSupervisorQueueStatesUseCase = ObserveSupervisorQueueStatesUseCase(
                FakeSupervisorQueueRepository()
            )
        )

        val snapshot = useCase().take(1).toList().first()
        assertThat(snapshot.items).hasSize(2)
        assertThat(snapshot.items.map { it.draftId })
            .containsExactly("draft-failed", "draft-qos")
            .inOrder()

        val failedItem = snapshot.items.first()
        assertThat(failedItem.attentionSignals).contains(ReviewerAttentionSignal.SYNC_FAILED)
        assertThat(failedItem.attentionSignals).contains(ReviewerAttentionSignal.STALE_DRAFT)
        assertThat(failedItem.dominantAttentionSignal).isEqualTo(ReviewerAttentionSignal.SYNC_FAILED)
        assertThat(failedItem.staleAgeHours).isAtLeast(0)
        assertThat(failedItem.urgencyClass).isEqualTo(ReviewerUrgencyClass.ACT_NOW)
        assertThat(failedItem.urgencyReason).isEqualTo(ReviewerUrgencyReason.SYNC_FAILED)
        assertThat(failedItem.ageBucket).isEqualTo(ReviewerDraftAgeBucket.OVERDUE)
        assertThat(failedItem.urgencyRank).isGreaterThan(0)

        val qosItem = snapshot.items.last()
        assertThat(qosItem.attentionSignals).contains(ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED)
        assertThat(qosItem.attentionSignals).contains(ReviewerAttentionSignal.QOS_PREREQUISITES_NOT_READY)
        assertThat(qosItem.siteCode).isEqualTo("QRTZ-001")
        assertThat(qosItem.dominantAttentionSignal).isEqualTo(ReviewerAttentionSignal.QOS_FAILED_OR_BLOCKED)
        assertThat(qosItem.urgencyClass).isEqualTo(ReviewerUrgencyClass.HIGH)
        assertThat(qosItem.urgencyReason).isEqualTo(ReviewerUrgencyReason.QOS_FAILED_OR_BLOCKED)

        assertThat(snapshot.summary.totalDraftCount).isEqualTo(2)
        assertThat(snapshot.summary.guidedDraftCount).isEqualTo(1)
        assertThat(snapshot.summary.nonGuidedDraftCount).isEqualTo(1)
        assertThat(snapshot.summary.syncFailedCount).isEqualTo(1)
        assertThat(snapshot.summary.qosRiskCount).isEqualTo(1)
        assertThat(snapshot.summary.staleDraftCount).isEqualTo(1)
        assertThat(snapshot.summary.attentionRequiredCount).isEqualTo(2)
        assertThat(snapshot.summary.actNowCount).isEqualTo(1)
        assertThat(snapshot.summary.overdueCount).isEqualTo(1)
    }

    private class FakeReportDraftRepository(
        initial: List<ReportDraft>
    ) : ReportDraftRepository {
        private val draftsFlow = MutableStateFlow(initial)

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
                .filter { it.siteId == siteId && it.originSessionId == originSessionId && it.originWorkflowType == originWorkflowType }
                .maxByOrNull { it.updatedAtEpochMillis }
        }

        override fun observeDraft(draftId: String): Flow<ReportDraft?> {
            return draftsFlow.map { drafts -> drafts.firstOrNull { it.id == draftId } }
        }

        override fun listAllDrafts(): Flow<List<ReportDraft>> = draftsFlow

        override fun listDraftsBySite(siteId: String): Flow<List<ReportDraft>> {
            return draftsFlow.map { drafts -> drafts.filter { it.siteId == siteId } }
        }
    }

    private class FakeSyncRepository(
        initial: Map<String, ReportSyncTrace>
    ) : SyncRepository {
        private val states = initial.mapValues { MutableStateFlow(it.value) }.toMutableMap()

        override suspend fun enqueueReportUpload(reportDraftId: String) = Unit

        override fun observeSyncTrace(reportDraftId: String): Flow<ReportSyncTrace> {
            return states.getOrPut(reportDraftId) { MutableStateFlow(ReportSyncTrace.localOnly()) }
        }

        override fun observeSyncState(reportDraftId: String): Flow<ReportSyncState> {
            return observeSyncTrace(reportDraftId).map { it.state }
        }

        override fun observePendingJobCount(): Flow<Int> = flowOf(0)

        override suspend fun processPendingJobs(limit: Int): Int = 0
    }

    private class FakeSiteRepository(
        sites: List<SiteSummary>
    ) : SiteRepository {
        private val sitesFlow = MutableStateFlow(sites)

        override fun observeSiteList(): Flow<List<SiteSummary>> = sitesFlow

        override fun observeSiteDetail(siteId: String) = flowOf(null)

        override suspend fun replaceSitesSnapshot(sites: List<com.quartz.platform.domain.model.SiteDetail>) = Unit
    }

    private class FakePerformanceSessionRepository(
        sessions: List<PerformanceSession>
    ) : PerformanceSessionRepository {
        private val sessionsFlow = MutableStateFlow(sessions)

        override fun observeSiteSessionHistory(siteId: String): Flow<List<PerformanceSession>> {
            return sessionsFlow.map { list -> list.filter { it.siteId == siteId } }
        }

        override fun observeLatestSiteSession(
            siteId: String,
            workflowType: PerformanceWorkflowType
        ): Flow<PerformanceSession?> {
            return observeSiteSessionHistory(siteId).map { list ->
                list.filter { it.workflowType == workflowType }.maxByOrNull { it.updatedAtEpochMillis }
            }
        }

        override suspend fun createSession(
            siteId: String,
            siteCode: String,
            workflowType: PerformanceWorkflowType,
            operatorName: String?,
            technology: String?
        ): PerformanceSession = sessionsFlow.value.first()

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
            observedNetworkStatus: NetworkStatus?,
            observedBatteryLevelPercent: Int?,
            observedLocationAvailable: Boolean?,
            observedSignalsCapturedAtEpochMillis: Long?,
            throughputMetrics: ThroughputMetrics,
            qosRunSummary: QosRunSummary,
            notes: String,
            resultSummary: String
        ) = Unit
    }

    private object EmptyXfeederRepository : XfeederGuidedSessionRepository {
        override fun observeSectorSessionHistory(siteId: String, sectorId: String) = flowOf(emptyList<com.quartz.platform.domain.model.XfeederGuidedSession>())
        override fun observeLatestSectorSession(siteId: String, sectorId: String) = flowOf<com.quartz.platform.domain.model.XfeederGuidedSession?>(null)
        override fun observeSiteClosureProjections(siteId: String) = flowOf(emptyList<com.quartz.platform.domain.model.GuidedSessionClosureProjection>())
        override suspend fun createSession(siteId: String, sectorId: String, sectorCode: String) = throw UnsupportedOperationException()
        override suspend fun updateStepStatus(sessionId: String, stepCode: com.quartz.platform.domain.model.XfeederStepCode, status: com.quartz.platform.domain.model.XfeederStepStatus) = Unit
        override suspend fun updateSessionSummary(
            sessionId: String,
            status: com.quartz.platform.domain.model.XfeederSessionStatus,
            sectorOutcome: com.quartz.platform.domain.model.XfeederSectorOutcome,
            closureEvidence: com.quartz.platform.domain.model.XfeederClosureEvidence,
            notes: String,
            resultSummary: String
        ) = Unit

        override suspend fun updateSessionGeospatialContext(
            sessionId: String,
            measurementZoneRadiusMeters: Int,
            measurementZoneExtensionReason: String,
            proximityModeEnabled: Boolean,
            proximityReferenceAltitudeMeters: Double?,
            proximityReferenceAltitudeSource: com.quartz.platform.domain.model.XfeederReferenceAltitudeSourceState
        ) = Unit
    }

    private object EmptyRetRepository : RetGuidedSessionRepository {
        override fun observeSectorSessionHistory(siteId: String, sectorId: String) = flowOf(emptyList<com.quartz.platform.domain.model.RetGuidedSession>())
        override fun observeLatestSectorSession(siteId: String, sectorId: String) = flowOf<com.quartz.platform.domain.model.RetGuidedSession?>(null)
        override fun observeSiteClosureProjections(siteId: String) = flowOf(emptyList<com.quartz.platform.domain.model.RetClosureProjection>())
        override suspend fun createSession(siteId: String, sectorId: String, sectorCode: String) = throw UnsupportedOperationException()
        override suspend fun updateStepStatus(sessionId: String, stepCode: com.quartz.platform.domain.model.RetStepCode, status: com.quartz.platform.domain.model.RetStepStatus) = Unit
        override suspend fun updateSessionSummary(
            sessionId: String,
            status: com.quartz.platform.domain.model.RetSessionStatus,
            resultOutcome: com.quartz.platform.domain.model.RetResultOutcome,
            notes: String,
            resultSummary: String
        ) = Unit

        override suspend fun updateSessionGeospatialContext(
            sessionId: String,
            measurementZoneRadiusMeters: Int,
            measurementZoneExtensionReason: String,
            proximityModeEnabled: Boolean,
            proximityReferenceAltitudeMeters: Double?,
            proximityReferenceAltitudeSource: com.quartz.platform.domain.model.RetReferenceAltitudeSourceState
        ) = Unit
    }
}

private class FakeSupervisorQueueRepository :
    com.quartz.platform.domain.repository.SupervisorQueueRepository {
    override fun observeQueueStates() =
        flowOf(emptyList<com.quartz.platform.domain.model.SupervisorQueueState>())

    override fun observeQueueActions() =
        flowOf(emptyList<com.quartz.platform.domain.model.SupervisorQueueAction>())

    override suspend fun transitionDraftStatus(
        draftId: String,
        toStatus: com.quartz.platform.domain.model.SupervisorQueueStatus,
        actionType: com.quartz.platform.domain.model.SupervisorQueueActionType,
        note: String?,
        triggeredFromFilter: String?,
        triggeredFromPreset: String?
    ) = Unit

    override suspend fun transitionDraftStatuses(
        draftIds: List<String>,
        toStatus: com.quartz.platform.domain.model.SupervisorQueueStatus,
        actionType: com.quartz.platform.domain.model.SupervisorQueueActionType,
        note: String?,
        triggeredFromFilter: String?,
        triggeredFromPreset: String?
    ) = Unit

    override suspend fun recordDraftAction(
        draftId: String,
        actionType: com.quartz.platform.domain.model.SupervisorQueueActionType,
        note: String?,
        triggeredFromFilter: String?,
        triggeredFromPreset: String?
    ) = Unit
}
