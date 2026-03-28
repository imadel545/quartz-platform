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
}
