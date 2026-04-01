package com.quartz.platform.data.repository

import androidx.room.withTransaction
import com.quartz.platform.core.dispatchers.DispatcherProvider
import com.quartz.platform.data.local.QuartzDatabase
import com.quartz.platform.data.local.dao.SupervisorQueueActionDao
import com.quartz.platform.data.local.dao.SupervisorQueueStateDao
import com.quartz.platform.data.local.entity.SupervisorQueueActionEntity
import com.quartz.platform.data.local.entity.SupervisorQueueStateEntity
import com.quartz.platform.domain.model.SupervisorQueueAction
import com.quartz.platform.domain.model.SupervisorQueueActionType
import com.quartz.platform.domain.model.SupervisorQueueState
import com.quartz.platform.domain.model.SupervisorQueueStatus
import com.quartz.platform.domain.repository.SupervisorQueueRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class OfflineFirstSupervisorQueueRepository @Inject constructor(
    private val database: QuartzDatabase,
    private val queueStateDao: SupervisorQueueStateDao,
    private val queueActionDao: SupervisorQueueActionDao,
    private val dispatcherProvider: DispatcherProvider
) : SupervisorQueueRepository {

    override fun observeQueueStates(): Flow<List<SupervisorQueueState>> {
        return queueStateDao.observeAll().map { entities ->
            entities.map { entity ->
                SupervisorQueueState(
                    draftId = entity.draftId,
                    status = entity.status,
                    lastActionType = entity.lastActionType,
                    lastActionAtEpochMillis = entity.lastActionAtEpochMillis,
                    lastActionNote = entity.lastActionNote,
                    updatedAtEpochMillis = entity.updatedAtEpochMillis
                )
            }
        }
    }

    override fun observeQueueActions(): Flow<List<SupervisorQueueAction>> {
        return queueActionDao.observeAll().map { entities ->
            entities.map { entity ->
                SupervisorQueueAction(
                    id = entity.id,
                    draftId = entity.draftId,
                    actionType = entity.actionType,
                    fromStatus = entity.fromStatus,
                    toStatus = entity.toStatus,
                    note = entity.note,
                    triggeredFromFilter = entity.triggeredFromFilter,
                    triggeredFromPreset = entity.triggeredFromPreset,
                    actedAtEpochMillis = entity.actedAtEpochMillis
                )
            }
        }
    }

    override suspend fun transitionDraftStatus(
        draftId: String,
        toStatus: SupervisorQueueStatus,
        actionType: SupervisorQueueActionType,
        note: String?,
        triggeredFromFilter: String?,
        triggeredFromPreset: String?
    ) {
        withContext(dispatcherProvider.io) {
            val now = System.currentTimeMillis()
            database.withTransaction {
                val current = queueStateDao.getByDraftId(draftId)
                queueStateDao.upsert(
                    SupervisorQueueStateEntity(
                        draftId = draftId,
                        status = toStatus,
                        lastActionType = actionType,
                        lastActionAtEpochMillis = now,
                        lastActionNote = note,
                        updatedAtEpochMillis = now
                    )
                )
                queueActionDao.insert(
                    SupervisorQueueActionEntity(
                        id = UUID.randomUUID().toString(),
                        draftId = draftId,
                        actionType = actionType,
                        fromStatus = current?.status,
                        toStatus = toStatus,
                        note = note,
                        triggeredFromFilter = triggeredFromFilter,
                        triggeredFromPreset = triggeredFromPreset,
                        actedAtEpochMillis = now
                    )
                )
            }
        }
    }

    override suspend fun transitionDraftStatuses(
        draftIds: List<String>,
        toStatus: SupervisorQueueStatus,
        actionType: SupervisorQueueActionType,
        note: String?,
        triggeredFromFilter: String?,
        triggeredFromPreset: String?
    ) {
        if (draftIds.isEmpty()) return
        withContext(dispatcherProvider.io) {
            val now = System.currentTimeMillis()
            database.withTransaction {
                draftIds.distinct().forEach { draftId ->
                    val current = queueStateDao.getByDraftId(draftId)
                    queueStateDao.upsert(
                        SupervisorQueueStateEntity(
                            draftId = draftId,
                            status = toStatus,
                            lastActionType = actionType,
                            lastActionAtEpochMillis = now,
                            lastActionNote = note,
                            updatedAtEpochMillis = now
                        )
                    )
                    queueActionDao.insert(
                        SupervisorQueueActionEntity(
                            id = UUID.randomUUID().toString(),
                            draftId = draftId,
                            actionType = actionType,
                            fromStatus = current?.status,
                            toStatus = toStatus,
                            note = note,
                            triggeredFromFilter = triggeredFromFilter,
                            triggeredFromPreset = triggeredFromPreset,
                            actedAtEpochMillis = now
                        )
                    )
                }
            }
        }
    }

    override suspend fun recordDraftAction(
        draftId: String,
        actionType: SupervisorQueueActionType,
        note: String?,
        triggeredFromFilter: String?,
        triggeredFromPreset: String?
    ) {
        withContext(dispatcherProvider.io) {
            val now = System.currentTimeMillis()
            val current = queueStateDao.getByDraftId(draftId)
            queueActionDao.insert(
                SupervisorQueueActionEntity(
                    id = UUID.randomUUID().toString(),
                    draftId = draftId,
                    actionType = actionType,
                    fromStatus = current?.status,
                    toStatus = current?.status,
                    note = note,
                    triggeredFromFilter = triggeredFromFilter,
                    triggeredFromPreset = triggeredFromPreset,
                    actedAtEpochMillis = now
                )
            )
        }
    }
}
