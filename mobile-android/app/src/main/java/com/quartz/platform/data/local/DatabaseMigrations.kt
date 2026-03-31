package com.quartz.platform.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS report_drafts (
                    id TEXT NOT NULL PRIMARY KEY,
                    siteId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    observation TEXT NOT NULL,
                    revision INTEGER NOT NULL,
                    createdAtEpochMillis INTEGER NOT NULL,
                    updatedAtEpochMillis INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_report_drafts_siteId
                ON report_drafts(siteId)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_sync_jobs_status_nextAttemptAtEpochMillis
                ON sync_jobs(status, nextAttemptAtEpochMillis)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS site_sectors (
                    id TEXT NOT NULL PRIMARY KEY,
                    siteId TEXT NOT NULL,
                    code TEXT NOT NULL,
                    azimuthDegrees INTEGER,
                    status TEXT NOT NULL,
                    hasConnectedCell INTEGER NOT NULL,
                    displayOrder INTEGER NOT NULL,
                    FOREIGN KEY(siteId) REFERENCES sites(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_site_sectors_siteId
                ON site_sectors(siteId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_site_sectors_siteId_displayOrder
                ON site_sectors(siteId, displayOrder)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS site_antennas (
                    id TEXT NOT NULL PRIMARY KEY,
                    siteId TEXT NOT NULL,
                    sectorId TEXT NOT NULL,
                    reference TEXT NOT NULL,
                    installedState TEXT NOT NULL,
                    forecastState TEXT,
                    tiltConfiguredDegrees REAL,
                    tiltObservedDegrees REAL,
                    documentationRef TEXT,
                    displayOrder INTEGER NOT NULL,
                    FOREIGN KEY(siteId) REFERENCES sites(id) ON DELETE CASCADE,
                    FOREIGN KEY(sectorId) REFERENCES site_sectors(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_site_antennas_siteId
                ON site_antennas(siteId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_site_antennas_sectorId
                ON site_antennas(sectorId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_site_antennas_sectorId_displayOrder
                ON site_antennas(sectorId, displayOrder)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS site_cells (
                    id TEXT NOT NULL PRIMARY KEY,
                    siteId TEXT NOT NULL,
                    sectorId TEXT NOT NULL,
                    antennaId TEXT,
                    label TEXT NOT NULL,
                    technology TEXT NOT NULL,
                    operatorName TEXT NOT NULL,
                    band TEXT NOT NULL,
                    pci TEXT,
                    status TEXT NOT NULL,
                    isConnected INTEGER NOT NULL,
                    displayOrder INTEGER NOT NULL,
                    FOREIGN KEY(siteId) REFERENCES sites(id) ON DELETE CASCADE,
                    FOREIGN KEY(sectorId) REFERENCES site_sectors(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_site_cells_siteId
                ON site_cells(siteId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_site_cells_sectorId
                ON site_cells(sectorId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_site_cells_sectorId_displayOrder
                ON site_cells(sectorId, displayOrder)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS xfeeder_sessions (
                    id TEXT NOT NULL PRIMARY KEY,
                    siteId TEXT NOT NULL,
                    sectorId TEXT NOT NULL,
                    sectorCode TEXT NOT NULL,
                    status TEXT NOT NULL,
                    sectorOutcome TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    resultSummary TEXT NOT NULL,
                    createdAtEpochMillis INTEGER NOT NULL,
                    updatedAtEpochMillis INTEGER NOT NULL,
                    completedAtEpochMillis INTEGER,
                    FOREIGN KEY(siteId) REFERENCES sites(id) ON DELETE CASCADE,
                    FOREIGN KEY(sectorId) REFERENCES site_sectors(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_xfeeder_sessions_siteId
                ON xfeeder_sessions(siteId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_xfeeder_sessions_sectorId
                ON xfeeder_sessions(sectorId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_xfeeder_sessions_siteId_sectorId_createdAtEpochMillis
                ON xfeeder_sessions(siteId, sectorId, createdAtEpochMillis)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS xfeeder_steps (
                    sessionId TEXT NOT NULL,
                    code TEXT NOT NULL,
                    required INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    displayOrder INTEGER NOT NULL,
                    PRIMARY KEY(sessionId, code),
                    FOREIGN KEY(sessionId) REFERENCES xfeeder_sessions(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_xfeeder_steps_sessionId
                ON xfeeder_steps(sessionId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_xfeeder_steps_sessionId_displayOrder
                ON xfeeder_steps(sessionId, displayOrder)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE xfeeder_sessions
                ADD COLUMN closureRelatedSectorCode TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE xfeeder_sessions
                ADD COLUMN closureUnreliableReason TEXT
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE xfeeder_sessions
                ADD COLUMN closureObservedSectorCount INTEGER
                """.trimIndent()
            )
        }
    }

    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE report_drafts
                ADD COLUMN originSessionId TEXT
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE report_drafts
                ADD COLUMN originSectorId TEXT
                """.trimIndent()
            )
        }
    }

    val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                UPDATE report_drafts
                SET originSessionId = NULL,
                    originSectorId = NULL
                WHERE originSessionId IS NOT NULL
                  AND EXISTS (
                    SELECT 1
                    FROM report_drafts newer
                    WHERE newer.siteId = report_drafts.siteId
                      AND newer.originSessionId = report_drafts.originSessionId
                      AND (
                        newer.updatedAtEpochMillis > report_drafts.updatedAtEpochMillis
                        OR (
                            newer.updatedAtEpochMillis = report_drafts.updatedAtEpochMillis
                            AND newer.createdAtEpochMillis > report_drafts.createdAtEpochMillis
                        )
                        OR (
                            newer.updatedAtEpochMillis = report_drafts.updatedAtEpochMillis
                            AND newer.createdAtEpochMillis = report_drafts.createdAtEpochMillis
                            AND newer.id > report_drafts.id
                        )
                      )
                  )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_report_drafts_siteId_originSessionId
                ON report_drafts(siteId, originSessionId)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_7_8: Migration = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ret_sessions (
                    id TEXT NOT NULL PRIMARY KEY,
                    siteId TEXT NOT NULL,
                    sectorId TEXT NOT NULL,
                    sectorCode TEXT NOT NULL,
                    status TEXT NOT NULL,
                    resultOutcome TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    resultSummary TEXT NOT NULL,
                    createdAtEpochMillis INTEGER NOT NULL,
                    updatedAtEpochMillis INTEGER NOT NULL,
                    completedAtEpochMillis INTEGER,
                    FOREIGN KEY(siteId) REFERENCES sites(id) ON DELETE CASCADE,
                    FOREIGN KEY(sectorId) REFERENCES site_sectors(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_ret_sessions_siteId
                ON ret_sessions(siteId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_ret_sessions_sectorId
                ON ret_sessions(sectorId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_ret_sessions_siteId_sectorId_createdAtEpochMillis
                ON ret_sessions(siteId, sectorId, createdAtEpochMillis)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ret_steps (
                    sessionId TEXT NOT NULL,
                    code TEXT NOT NULL,
                    required INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    displayOrder INTEGER NOT NULL,
                    PRIMARY KEY(sessionId, code),
                    FOREIGN KEY(sessionId) REFERENCES ret_sessions(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_ret_steps_sessionId
                ON ret_steps(sessionId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_ret_steps_sessionId_displayOrder
                ON ret_steps(sessionId, displayOrder)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_8_9: Migration = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE report_drafts
                ADD COLUMN originWorkflowType TEXT
                """.trimIndent()
            )

            db.execSQL(
                """
                UPDATE report_drafts
                SET originWorkflowType = 'XFEEDER'
                WHERE originSessionId IS NOT NULL
                  AND originWorkflowType IS NULL
                  AND EXISTS (
                    SELECT 1
                    FROM xfeeder_sessions x
                    WHERE x.id = report_drafts.originSessionId
                  )
                """.trimIndent()
            )

            db.execSQL(
                """
                UPDATE report_drafts
                SET originWorkflowType = 'RET'
                WHERE originSessionId IS NOT NULL
                  AND originWorkflowType IS NULL
                  AND EXISTS (
                    SELECT 1
                    FROM ret_sessions r
                    WHERE r.id = report_drafts.originSessionId
                  )
                """.trimIndent()
            )

            db.execSQL("DROP INDEX IF EXISTS index_report_drafts_siteId_originSessionId")
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_report_drafts_siteId_originSessionId_originWorkflowType
                ON report_drafts(siteId, originSessionId, originWorkflowType)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_9_10: Migration = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Defensive cleanup in case of unexpected legacy/manual values.
            db.execSQL(
                """
                UPDATE report_drafts
                SET originWorkflowType = NULL
                WHERE originWorkflowType IS NOT NULL
                  AND originWorkflowType NOT IN ('XFEEDER', 'RET')
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS trg_report_drafts_origin_workflow_type_insert
                BEFORE INSERT ON report_drafts
                WHEN NEW.originWorkflowType IS NOT NULL
                     AND NEW.originWorkflowType NOT IN ('XFEEDER', 'RET')
                BEGIN
                    SELECT RAISE(ABORT, 'invalid originWorkflowType');
                END
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS trg_report_drafts_origin_workflow_type_update
                BEFORE UPDATE OF originWorkflowType ON report_drafts
                WHEN NEW.originWorkflowType IS NOT NULL
                     AND NEW.originWorkflowType NOT IN ('XFEEDER', 'RET')
                BEGIN
                    SELECT RAISE(ABORT, 'invalid originWorkflowType');
                END
                """.trimIndent()
            )
        }
    }

    val MIGRATION_10_11: Migration = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE xfeeder_sessions
                ADD COLUMN measurementZoneRadiusMeters INTEGER NOT NULL DEFAULT 120
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE xfeeder_sessions
                ADD COLUMN measurementZoneExtensionReason TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE xfeeder_sessions
                ADD COLUMN proximityModeEnabled INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
        }
    }

    val MIGRATION_11_12: Migration = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE xfeeder_sessions
                ADD COLUMN proximityReferenceAltitudeMeters REAL
                """.trimIndent()
            )
        }
    }

    val MIGRATION_12_13: Migration = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE site_antennas
                ADD COLUMN referenceAltitudeMeters REAL
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE xfeeder_sessions
                ADD COLUMN proximityReferenceAltitudeSource TEXT NOT NULL DEFAULT 'UNAVAILABLE'
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE xfeeder_sessions
                SET proximityReferenceAltitudeSource = 'OPERATOR_OVERRIDE'
                WHERE proximityReferenceAltitudeMeters IS NOT NULL
                """.trimIndent()
            )
        }
    }

    val MIGRATION_13_14: Migration = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE ret_sessions
                ADD COLUMN measurementZoneRadiusMeters INTEGER NOT NULL DEFAULT 120
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE ret_sessions
                ADD COLUMN measurementZoneExtensionReason TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE ret_sessions
                ADD COLUMN proximityModeEnabled INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE ret_sessions
                ADD COLUMN proximityReferenceAltitudeMeters REAL
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE ret_sessions
                ADD COLUMN proximityReferenceAltitudeSource TEXT NOT NULL DEFAULT 'UNAVAILABLE'
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE ret_sessions
                SET proximityReferenceAltitudeSource = 'OPERATOR_OVERRIDE'
                WHERE proximityReferenceAltitudeMeters IS NOT NULL
                """.trimIndent()
            )
        }
    }

    val MIGRATION_14_15: Migration = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS performance_sessions (
                    id TEXT NOT NULL PRIMARY KEY,
                    siteId TEXT NOT NULL,
                    siteCode TEXT NOT NULL,
                    workflowType TEXT NOT NULL,
                    operatorName TEXT,
                    technology TEXT,
                    status TEXT NOT NULL,
                    prerequisiteNetworkReady INTEGER NOT NULL,
                    prerequisiteBatterySufficient INTEGER NOT NULL,
                    prerequisiteLocationReady INTEGER NOT NULL,
                    throughputDownloadMbps REAL,
                    throughputUploadMbps REAL,
                    throughputLatencyMs INTEGER,
                    throughputMinDownloadMbps REAL,
                    throughputMinUploadMbps REAL,
                    throughputMaxLatencyMs INTEGER,
                    qosScriptId TEXT,
                    qosScriptName TEXT,
                    qosTargetTechnology TEXT,
                    qosTargetPhoneNumber TEXT,
                    qosIterationCount INTEGER NOT NULL,
                    qosSuccessCount INTEGER NOT NULL,
                    qosFailureCount INTEGER NOT NULL,
                    notes TEXT NOT NULL,
                    resultSummary TEXT NOT NULL,
                    createdAtEpochMillis INTEGER NOT NULL,
                    updatedAtEpochMillis INTEGER NOT NULL,
                    completedAtEpochMillis INTEGER,
                    FOREIGN KEY(siteId) REFERENCES sites(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_performance_sessions_siteId
                ON performance_sessions(siteId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_performance_sessions_siteId_workflowType_createdAtEpochMillis
                ON performance_sessions(siteId, workflowType, createdAtEpochMillis)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS performance_steps (
                    sessionId TEXT NOT NULL,
                    code TEXT NOT NULL,
                    required INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    displayOrder INTEGER NOT NULL,
                    PRIMARY KEY(sessionId, code),
                    FOREIGN KEY(sessionId) REFERENCES performance_sessions(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_performance_steps_sessionId
                ON performance_steps(sessionId)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_performance_steps_sessionId_displayOrder
                ON performance_steps(sessionId, displayOrder)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_15_16: Migration = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                UPDATE report_drafts
                SET originWorkflowType = NULL
                WHERE originWorkflowType IS NOT NULL
                  AND originWorkflowType NOT IN ('XFEEDER', 'RET', 'PERFORMANCE')
                """.trimIndent()
            )

            db.execSQL("DROP TRIGGER IF EXISTS trg_report_drafts_origin_workflow_type_insert")
            db.execSQL("DROP TRIGGER IF EXISTS trg_report_drafts_origin_workflow_type_update")

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS trg_report_drafts_origin_workflow_type_insert
                BEFORE INSERT ON report_drafts
                WHEN NEW.originWorkflowType IS NOT NULL
                     AND NEW.originWorkflowType NOT IN ('XFEEDER', 'RET', 'PERFORMANCE')
                BEGIN
                    SELECT RAISE(ABORT, 'invalid originWorkflowType');
                END
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS trg_report_drafts_origin_workflow_type_update
                BEFORE UPDATE OF originWorkflowType ON report_drafts
                WHEN NEW.originWorkflowType IS NOT NULL
                     AND NEW.originWorkflowType NOT IN ('XFEEDER', 'RET', 'PERFORMANCE')
                BEGIN
                    SELECT RAISE(ABORT, 'invalid originWorkflowType');
                END
                """.trimIndent()
            )
        }
    }

    val MIGRATION_16_17: Migration = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE performance_sessions
                ADD COLUMN qosConfiguredRepeatCount INTEGER
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE performance_sessions
                ADD COLUMN qosTestFamiliesCsv TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS qos_scripts (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    repeatCount INTEGER NOT NULL,
                    targetTechnologiesCsv TEXT NOT NULL,
                    testFamiliesCsv TEXT NOT NULL,
                    isArchived INTEGER NOT NULL,
                    createdAtEpochMillis INTEGER NOT NULL,
                    updatedAtEpochMillis INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_qos_scripts_isArchived_updatedAtEpochMillis
                ON qos_scripts(isArchived, updatedAtEpochMillis)
                """.trimIndent()
            )

            val now = System.currentTimeMillis()
            db.execSQL(
                """
                INSERT OR IGNORE INTO qos_scripts (
                    id, name, repeatCount, targetTechnologiesCsv, testFamiliesCsv, isArchived, createdAtEpochMillis, updatedAtEpochMillis
                ) VALUES (
                    'qos-script-latency-throughput',
                    'Latence + Débit',
                    1,
                    '4G,5G',
                    'THROUGHPUT_LATENCY',
                    0,
                    $now,
                    $now
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO qos_scripts (
                    id, name, repeatCount, targetTechnologiesCsv, testFamiliesCsv, isArchived, createdAtEpochMillis, updatedAtEpochMillis
                ) VALUES (
                    'qos-script-voice-sms',
                    'Voix / SMS',
                    1,
                    '4G,5G',
                    'SMS,VOLTE_CALL,CSFB_CALL,EMERGENCY_CALL,STANDARD_CALL',
                    0,
                    $now,
                    $now
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO qos_scripts (
                    id, name, repeatCount, targetTechnologiesCsv, testFamiliesCsv, isArchived, createdAtEpochMillis, updatedAtEpochMillis
                ) VALUES (
                    'qos-script-video-streaming',
                    'Streaming vidéo',
                    1,
                    '4G,5G',
                    'VIDEO_STREAMING',
                    0,
                    $now,
                    $now
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_17_18: Migration = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS performance_qos_family_results (
                    sessionId TEXT NOT NULL,
                    family TEXT NOT NULL,
                    status TEXT NOT NULL,
                    failureReason TEXT,
                    observedLatencyMs INTEGER,
                    observedDownloadMbps REAL,
                    observedUploadMbps REAL,
                    updatedAtEpochMillis INTEGER NOT NULL,
                    PRIMARY KEY(sessionId, family),
                    FOREIGN KEY(sessionId) REFERENCES performance_sessions(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_performance_qos_family_results_sessionId
                ON performance_qos_family_results(sessionId)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_performance_qos_family_results_sessionId_updatedAtEpochMillis
                ON performance_qos_family_results(sessionId, updatedAtEpochMillis)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_18_19: Migration = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE performance_sessions
                ADD COLUMN qosConfiguredTechnologiesCsv TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE performance_sessions
                ADD COLUMN qosScriptSnapshotUpdatedAtEpochMillis INTEGER
                """.trimIndent()
            )
        }
    }

    val MIGRATION_19_20: Migration = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS performance_qos_timeline_events (
                    sessionId TEXT NOT NULL,
                    family TEXT NOT NULL,
                    repetitionIndex INTEGER NOT NULL,
                    eventType TEXT NOT NULL,
                    reason TEXT,
                    occurredAtEpochMillis INTEGER NOT NULL,
                    PRIMARY KEY(sessionId, family, repetitionIndex, eventType),
                    FOREIGN KEY(sessionId) REFERENCES performance_sessions(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_performance_qos_timeline_events_sessionId
                ON performance_qos_timeline_events(sessionId)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_performance_qos_timeline_events_sessionId_occurredAtEpochMillis
                ON performance_qos_timeline_events(sessionId, occurredAtEpochMillis)
                """.trimIndent()
            )
        }
    }
}
