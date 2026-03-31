package com.quartz.platform.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

@RunWith(AndroidJUnit4::class)
class QuartzDatabaseMigrationTest {

    @get:Rule
    val migrationTestHelper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = QuartzDatabase::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration_6_7_deduplicates_guided_links_and_enforces_unique_index() {
        val dbName = "quartz-migration-6-7-test"

        migrationTestHelper.createDatabase(dbName, 6).apply {
            insertDraft(
                id = "draft-old",
                siteId = "site-1",
                originSessionId = "session-1",
                originSectorId = "sector-s0",
                updatedAtEpochMillis = 100L,
                createdAtEpochMillis = 100L
            )
            insertDraft(
                id = "draft-new",
                siteId = "site-1",
                originSessionId = "session-1",
                originSectorId = "sector-s0",
                updatedAtEpochMillis = 200L,
                createdAtEpochMillis = 200L
            )
            insertDraft(
                id = "draft-null",
                siteId = "site-1",
                originSessionId = null,
                originSectorId = null,
                updatedAtEpochMillis = 150L,
                createdAtEpochMillis = 150L
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            7,
            true,
            DatabaseMigrations.MIGRATION_6_7
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'index'
                  AND name = 'index_report_drafts_siteId_originSessionId'
                """.trimIndent()
            )
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM report_drafts
                WHERE siteId = 'site-1' AND originSessionId = 'session-1'
                """.trimIndent()
            )
        )

        assertEquals(
            "draft-new",
            migrated.stringQuery(
                """
                SELECT id FROM report_drafts
                WHERE siteId = 'site-1' AND originSessionId = 'session-1'
                LIMIT 1
                """.trimIndent()
            )
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM report_drafts
                WHERE id = 'draft-old'
                  AND originSessionId IS NULL
                  AND originSectorId IS NULL
                """.trimIndent()
            )
        )

        val duplicateInsert = runCatching {
            migrated.insertDraft(
                id = "draft-dup",
                siteId = "site-1",
                originSessionId = "session-1",
                originSectorId = "sector-s9",
                updatedAtEpochMillis = 300L,
                createdAtEpochMillis = 300L
            )
        }
        assertNotNull(duplicateInsert.exceptionOrNull())

        migrated.close()
    }

    @Test
    fun migration_8_9_backfills_workflow_type_and_enforces_typed_unique_index() {
        val dbName = "quartz-migration-8-9-test"

        migrationTestHelper.createDatabase(dbName, 8).apply {
            insertXfeederSession(id = "xfeeder-session-1", siteId = "site-1", sectorId = "sector-a")
            insertRetSession(id = "ret-session-1", siteId = "site-1", sectorId = "sector-a")
            insertDraft(
                id = "draft-xfeeder",
                siteId = "site-1",
                originSessionId = "xfeeder-session-1",
                originSectorId = "sector-a",
                updatedAtEpochMillis = 110L,
                createdAtEpochMillis = 100L
            )
            insertDraft(
                id = "draft-ret",
                siteId = "site-1",
                originSessionId = "ret-session-1",
                originSectorId = "sector-a",
                updatedAtEpochMillis = 120L,
                createdAtEpochMillis = 100L
            )
            insertDraft(
                id = "draft-non-guided",
                siteId = "site-1",
                originSessionId = null,
                originSectorId = null,
                updatedAtEpochMillis = 130L,
                createdAtEpochMillis = 100L
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            9,
            true,
            DatabaseMigrations.MIGRATION_8_9
        )

        assertEquals(
            "XFEEDER",
            migrated.stringQuery(
                """
                SELECT originWorkflowType FROM report_drafts WHERE id = 'draft-xfeeder'
                """.trimIndent()
            )
        )
        assertEquals(
            "RET",
            migrated.stringQuery(
                """
                SELECT originWorkflowType FROM report_drafts WHERE id = 'draft-ret'
                """.trimIndent()
            )
        )
        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM report_drafts
                WHERE id = 'draft-non-guided' AND originWorkflowType IS NULL
                """.trimIndent()
            )
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'index'
                  AND name = 'index_report_drafts_siteId_originSessionId_originWorkflowType'
                """.trimIndent()
            )
        )

        val duplicateInsert = runCatching {
            migrated.insertDraftWithWorkflow(
                id = "draft-dup-xfeeder",
                siteId = "site-1",
                originSessionId = "xfeeder-session-1",
                originSectorId = "sector-a",
                originWorkflowType = "XFEEDER",
                updatedAtEpochMillis = 140L,
                createdAtEpochMillis = 100L
            )
        }
        assertNotNull(duplicateInsert.exceptionOrNull())

        migrated.close()
    }

    @Test
    fun migration_9_10_enforces_allowed_origin_workflow_type_values() {
        val dbName = "quartz-migration-9-10-test"

        migrationTestHelper.createDatabase(dbName, 9).apply {
            insertDraftWithWorkflow(
                id = "draft-valid",
                siteId = "site-1",
                originSessionId = "session-x",
                originSectorId = "sector-a",
                originWorkflowType = "XFEEDER",
                updatedAtEpochMillis = 100L,
                createdAtEpochMillis = 100L
            )
            insertDraftWithWorkflow(
                id = "draft-invalid-legacy",
                siteId = "site-1",
                originSessionId = "session-z",
                originSectorId = "sector-b",
                originWorkflowType = "LEGACY",
                updatedAtEpochMillis = 110L,
                createdAtEpochMillis = 110L
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            10,
            true,
            DatabaseMigrations.MIGRATION_9_10
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM report_drafts
                WHERE id = 'draft-invalid-legacy' AND originWorkflowType IS NULL
                """.trimIndent()
            )
        )

        val invalidInsert = runCatching {
            migrated.insertDraftWithWorkflow(
                id = "draft-invalid-insert",
                siteId = "site-1",
                originSessionId = "session-new",
                originSectorId = "sector-c",
                originWorkflowType = "INVALID",
                updatedAtEpochMillis = 120L,
                createdAtEpochMillis = 120L
            )
        }
        assertNotNull(invalidInsert.exceptionOrNull())

        val nullInsert = runCatching {
            migrated.insertDraftWithWorkflow(
                id = "draft-null",
                siteId = "site-1",
                originSessionId = null,
                originSectorId = null,
                originWorkflowType = null,
                updatedAtEpochMillis = 130L,
                createdAtEpochMillis = 130L
            )
        }
        assertEquals(true, nullInsert.isSuccess)

        migrated.close()
    }

    @Test
    fun migration_10_11_adds_xfeeder_geospatial_session_defaults() {
        val dbName = "quartz-migration-10-11-test"

        migrationTestHelper.createDatabase(dbName, 10).apply {
            insertXfeederSession(
                id = "xfeeder-session-geo-1",
                siteId = "site-1",
                sectorId = "sector-a"
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            11,
            true,
            DatabaseMigrations.MIGRATION_10_11
        )

        assertEquals(
            120L,
            migrated.longQuery(
                """
                SELECT measurementZoneRadiusMeters
                FROM xfeeder_sessions
                WHERE id = 'xfeeder-session-geo-1'
                """.trimIndent()
            )
        )

        assertEquals(
            0L,
            migrated.longQuery(
                """
                SELECT proximityModeEnabled
                FROM xfeeder_sessions
                WHERE id = 'xfeeder-session-geo-1'
                """.trimIndent()
            )
        )

        assertEquals(
            "",
            migrated.stringQuery(
                """
                SELECT measurementZoneExtensionReason
                FROM xfeeder_sessions
                WHERE id = 'xfeeder-session-geo-1'
                """.trimIndent()
            )
        )

        migrated.close()
    }

    @Test
    fun migration_11_12_adds_proximity_reference_altitude_column() {
        val dbName = "quartz-migration-11-12-test"

        migrationTestHelper.createDatabase(dbName, 11).apply {
            insertXfeederSessionV11(
                id = "xfeeder-session-proximity-1",
                siteId = "site-1",
                sectorId = "sector-a"
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            12,
            true,
            DatabaseMigrations.MIGRATION_11_12
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('xfeeder_sessions')
                WHERE name = 'proximityReferenceAltitudeMeters'
                """.trimIndent()
            )
        )
        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM xfeeder_sessions
                WHERE id = 'xfeeder-session-proximity-1'
                  AND proximityReferenceAltitudeMeters IS NULL
                """.trimIndent()
            )
        )

        migrated.close()
    }

    @Test
    fun migration_12_13_adds_technical_altitude_source_columns_and_backfills_session_source() {
        val dbName = "quartz-migration-12-13-test"

        migrationTestHelper.createDatabase(dbName, 12).apply {
            insertXfeederSessionV12(
                id = "xfeeder-session-altitude-source-1",
                siteId = "site-1",
                sectorId = "sector-a",
                proximityReferenceAltitudeMeters = 119.5
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            13,
            true,
            DatabaseMigrations.MIGRATION_12_13
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('site_antennas')
                WHERE name = 'referenceAltitudeMeters'
                """.trimIndent()
            )
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('xfeeder_sessions')
                WHERE name = 'proximityReferenceAltitudeSource'
                """.trimIndent()
            )
        )

        assertEquals(
            "OPERATOR_OVERRIDE",
            migrated.stringQuery(
                """
                SELECT proximityReferenceAltitudeSource
                FROM xfeeder_sessions
                WHERE id = 'xfeeder-session-altitude-source-1'
                """.trimIndent()
            )
        )

        migrated.close()
    }

    @Test
    fun migration_13_14_adds_ret_geospatial_columns_with_safe_defaults() {
        val dbName = "quartz-migration-13-14-test"

        migrationTestHelper.createDatabase(dbName, 13).apply {
            insertRetSession(
                id = "ret-session-geo-1",
                siteId = "site-1",
                sectorId = "sector-a"
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            14,
            true,
            DatabaseMigrations.MIGRATION_13_14
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('ret_sessions')
                WHERE name = 'measurementZoneRadiusMeters'
                """.trimIndent()
            )
        )
        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('ret_sessions')
                WHERE name = 'proximityReferenceAltitudeSource'
                """.trimIndent()
            )
        )
        assertEquals(
            120L,
            migrated.longQuery(
                """
                SELECT measurementZoneRadiusMeters
                FROM ret_sessions
                WHERE id = 'ret-session-geo-1'
                """.trimIndent()
            )
        )
        assertEquals(
            "UNAVAILABLE",
            migrated.stringQuery(
                """
                SELECT proximityReferenceAltitudeSource
                FROM ret_sessions
                WHERE id = 'ret-session-geo-1'
                """.trimIndent()
            )
        )

        migrated.close()
    }

    @Test
    fun migration_17_18_adds_qos_family_results_table_with_indexes() {
        val dbName = "quartz-migration-17-18-test"

        migrationTestHelper.createDatabase(dbName, 17).apply {
            insertPerformanceSessionV17(
                id = "perf-qos-1",
                siteId = "site-1",
                workflowType = "QOS_SCRIPT"
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            18,
            true,
            DatabaseMigrations.MIGRATION_17_18
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'table'
                  AND name = 'performance_qos_family_results'
                """.trimIndent()
            )
        )
        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'index'
                  AND name = 'index_performance_qos_family_results_sessionId'
                """.trimIndent()
            )
        )
        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'index'
                  AND name = 'index_performance_qos_family_results_sessionId_updatedAtEpochMillis'
                """.trimIndent()
            )
        )

        migrated.execSQL(
            """
            INSERT INTO performance_qos_family_results(
                sessionId,
                family,
                status,
                failureReason,
                observedLatencyMs,
                observedDownloadMbps,
                observedUploadMbps,
                updatedAtEpochMillis
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf("perf-qos-1", "SMS", "PASSED", null, 120, 20.5, 10.2, 200L)
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM performance_qos_family_results
                WHERE sessionId = 'perf-qos-1' AND family = 'SMS' AND status = 'PASSED'
                """.trimIndent()
            )
        )

        val duplicateInsert = runCatching {
            migrated.execSQL(
                """
                INSERT INTO performance_qos_family_results(
                    sessionId,
                    family,
                    status,
                    failureReason,
                    observedLatencyMs,
                    observedDownloadMbps,
                    observedUploadMbps,
                    updatedAtEpochMillis
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf("perf-qos-1", "SMS", "FAILED", "duplicate", null, null, null, 300L)
            )
        }
        assertNotNull(duplicateInsert.exceptionOrNull())

        migrated.close()
    }

    @Test
    fun migration_18_19_adds_qos_script_snapshot_columns() {
        val dbName = "quartz-migration-18-19-test"

        migrationTestHelper.createDatabase(dbName, 18).apply {
            insertPerformanceSessionV17(
                id = "perf-qos-18",
                siteId = "site-1",
                workflowType = "QOS_SCRIPT"
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            19,
            true,
            DatabaseMigrations.MIGRATION_18_19
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('performance_sessions')
                WHERE name = 'qosConfiguredTechnologiesCsv'
                """.trimIndent()
            )
        )
        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('performance_sessions')
                WHERE name = 'qosScriptSnapshotUpdatedAtEpochMillis'
                """.trimIndent()
            )
        )
        assertEquals(
            "",
            migrated.stringQuery(
                """
                SELECT qosConfiguredTechnologiesCsv
                FROM performance_sessions
                WHERE id = 'perf-qos-18'
                """.trimIndent()
            )
        )
        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM performance_sessions
                WHERE id = 'perf-qos-18'
                  AND qosScriptSnapshotUpdatedAtEpochMillis IS NULL
                """.trimIndent()
            )
        )

        migrated.close()
    }

    @Test
    fun migration_19_20_adds_qos_timeline_events_table_with_indexes() {
        val dbName = "quartz-migration-19-20-test"

        migrationTestHelper.createDatabase(dbName, 19).apply {
            insertPerformanceSessionV19(
                id = "perf-qos-19",
                siteId = "site-1",
                workflowType = "QOS_SCRIPT"
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            20,
            true,
            DatabaseMigrations.MIGRATION_19_20
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'table'
                  AND name = 'performance_qos_timeline_events'
                """.trimIndent()
            )
        )
        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'index'
                  AND name = 'index_performance_qos_timeline_events_sessionId'
                """.trimIndent()
            )
        )
        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'index'
                  AND name = 'index_performance_qos_timeline_events_sessionId_occurredAtEpochMillis'
                """.trimIndent()
            )
        )

        migrated.execSQL(
            """
            INSERT INTO performance_qos_timeline_events(
                sessionId,
                family,
                repetitionIndex,
                eventType,
                reason,
                occurredAtEpochMillis
            ) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf("perf-qos-19", "SMS", 1, "STARTED", null, 300L)
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM performance_qos_timeline_events
                WHERE sessionId = 'perf-qos-19'
                  AND family = 'SMS'
                  AND repetitionIndex = 1
                  AND eventType = 'STARTED'
                """.trimIndent()
            )
        )

        val duplicateInsert = runCatching {
            migrated.execSQL(
                """
                INSERT INTO performance_qos_timeline_events(
                    sessionId,
                    family,
                    repetitionIndex,
                    eventType,
                    reason,
                    occurredAtEpochMillis
                ) VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf("perf-qos-19", "SMS", 1, "STARTED", "duplicate", 400L)
            )
        }
        assertNotNull(duplicateInsert.exceptionOrNull())

        migrated.close()
    }

    @Test
    fun migration_20_21_makes_qos_timeline_transactional_with_checkpoint_sequence() {
        val dbName = "quartz-migration-20-21-test"

        migrationTestHelper.createDatabase(dbName, 20).apply {
            insertPerformanceSessionV19(
                id = "perf-qos-20",
                siteId = "site-1",
                workflowType = "QOS_SCRIPT"
            )
            execSQL(
                """
                INSERT INTO performance_qos_timeline_events(
                    sessionId,
                    family,
                    repetitionIndex,
                    eventType,
                    reason,
                    occurredAtEpochMillis
                ) VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf("perf-qos-20", "SMS", 1, "STARTED", null, 300L)
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            21,
            true,
            DatabaseMigrations.MIGRATION_20_21
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('performance_qos_timeline_events')
                WHERE name = 'eventId'
                """.trimIndent()
            )
        )
        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('performance_qos_timeline_events')
                WHERE name = 'checkpointSequence'
                """.trimIndent()
            )
        )
        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT checkpointSequence FROM performance_qos_timeline_events
                WHERE sessionId = 'perf-qos-20'
                  AND family = 'SMS'
                  AND repetitionIndex = 1
                  AND eventType = 'STARTED'
                """.trimIndent()
            )
        )

        migrated.execSQL(
            """
            INSERT INTO performance_qos_timeline_events(
                sessionId,
                family,
                repetitionIndex,
                eventType,
                reason,
                occurredAtEpochMillis,
                checkpointSequence
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf("perf-qos-20", "SMS", 1, "STARTED", "duplicate allowed", 400L, 2)
        )

        assertEquals(
            2L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM performance_qos_timeline_events
                WHERE sessionId = 'perf-qos-20'
                  AND family = 'SMS'
                  AND repetitionIndex = 1
                  AND eventType = 'STARTED'
                """.trimIndent()
            )
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'index'
                  AND name = 'index_performance_qos_timeline_events_sessionId_checkpointSequence'
                """.trimIndent()
            )
        )

        migrated.close()
    }

    @Test
    fun migration_21_22_adds_qos_reason_code_columns() {
        val dbName = "quartz-migration-21-22-test"

        migrationTestHelper.createDatabase(dbName, 21).apply {
            insertPerformanceSessionV19(
                id = "perf-qos-21",
                siteId = "site-1",
                workflowType = "QOS_SCRIPT"
            )
            execSQL(
                """
                INSERT INTO performance_qos_family_results(
                    sessionId,
                    family,
                    status,
                    failureReason,
                    observedLatencyMs,
                    observedDownloadMbps,
                    observedUploadMbps,
                    updatedAtEpochMillis
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf("perf-qos-21", "SMS", "FAILED", "legacy reason", null, null, null, 500L)
            )
            execSQL(
                """
                INSERT INTO performance_qos_timeline_events(
                    sessionId,
                    family,
                    repetitionIndex,
                    eventType,
                    reason,
                    occurredAtEpochMillis,
                    checkpointSequence
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf("perf-qos-21", "SMS", 1, "FAILED", "legacy reason", 500L, 1)
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            22,
            true,
            DatabaseMigrations.MIGRATION_21_22
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('performance_qos_family_results')
                WHERE name = 'failureReasonCode'
                """.trimIndent()
            )
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('performance_qos_timeline_events')
                WHERE name = 'reasonCode'
                """.trimIndent()
            )
        )

        migrated.execSQL(
            """
            UPDATE performance_qos_family_results
            SET failureReasonCode = 'NETWORK_UNAVAILABLE'
            WHERE sessionId = 'perf-qos-21' AND family = 'SMS'
            """.trimIndent()
        )
        migrated.execSQL(
            """
            UPDATE performance_qos_timeline_events
            SET reasonCode = 'NETWORK_UNAVAILABLE'
            WHERE sessionId = 'perf-qos-21' AND family = 'SMS' AND repetitionIndex = 1
            """.trimIndent()
        )

        assertEquals(
            "NETWORK_UNAVAILABLE",
            migrated.stringQuery(
                """
                SELECT failureReasonCode
                FROM performance_qos_family_results
                WHERE sessionId = 'perf-qos-21' AND family = 'SMS'
                """.trimIndent()
            )
        )
        assertEquals(
            "NETWORK_UNAVAILABLE",
            migrated.stringQuery(
                """
                SELECT reasonCode
                FROM performance_qos_timeline_events
                WHERE sessionId = 'perf-qos-21' AND family = 'SMS' AND repetitionIndex = 1
                """.trimIndent()
            )
        )

        migrated.close()
    }

    @Test
    fun migration_22_23_adds_performance_session_observed_device_signal_columns() {
        val dbName = "quartz-migration-22-23-test"

        migrationTestHelper.createDatabase(dbName, 22).apply {
            insertPerformanceSessionV19(
                id = "perf-qos-22",
                siteId = "site-1",
                workflowType = "QOS_SCRIPT"
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            23,
            true,
            DatabaseMigrations.MIGRATION_22_23
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('performance_sessions')
                WHERE name = 'observedNetworkStatus'
                """.trimIndent()
            )
        )
        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('performance_sessions')
                WHERE name = 'observedBatteryLevelPercent'
                """.trimIndent()
            )
        )
        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('performance_sessions')
                WHERE name = 'observedLocationAvailable'
                """.trimIndent()
            )
        )
        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM pragma_table_info('performance_sessions')
                WHERE name = 'observedSignalsCapturedAtEpochMillis'
                """.trimIndent()
            )
        )

        assertEquals(
            1L,
            migrated.longQuery(
                """
                SELECT COUNT(*) FROM performance_sessions
                WHERE id = 'perf-qos-22'
                  AND observedNetworkStatus IS NULL
                  AND observedBatteryLevelPercent IS NULL
                  AND observedLocationAvailable IS NULL
                  AND observedSignalsCapturedAtEpochMillis IS NULL
                """.trimIndent()
            )
        )

        migrated.execSQL(
            """
            UPDATE performance_sessions
            SET observedNetworkStatus = 'AVAILABLE',
                observedBatteryLevelPercent = 73,
                observedLocationAvailable = 1,
                observedSignalsCapturedAtEpochMillis = 1000
            WHERE id = 'perf-qos-22'
            """.trimIndent()
        )

        assertEquals(
            "AVAILABLE",
            migrated.stringQuery(
                """
                SELECT observedNetworkStatus
                FROM performance_sessions
                WHERE id = 'perf-qos-22'
                """.trimIndent()
            )
        )
        assertEquals(
            73L,
            migrated.longQuery(
                """
                SELECT observedBatteryLevelPercent
                FROM performance_sessions
                WHERE id = 'perf-qos-22'
                """.trimIndent()
            )
        )

        migrated.close()
    }
}

private fun SupportSQLiteDatabase.insertDraft(
    id: String,
    siteId: String,
    originSessionId: String?,
    originSectorId: String?,
    updatedAtEpochMillis: Long,
    createdAtEpochMillis: Long
) {
    execSQL(
        """
        INSERT INTO report_drafts(
            id,
            siteId,
            originSessionId,
            originSectorId,
            title,
            observation,
            revision,
            createdAtEpochMillis,
            updatedAtEpochMillis
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf(
            id,
            siteId,
            originSessionId,
            originSectorId,
            "Draft $id",
            "",
            1,
            createdAtEpochMillis,
            updatedAtEpochMillis
        )
    )
}

private fun SupportSQLiteDatabase.insertDraftWithWorkflow(
    id: String,
    siteId: String,
    originSessionId: String?,
    originSectorId: String?,
    originWorkflowType: String?,
    updatedAtEpochMillis: Long,
    createdAtEpochMillis: Long
) {
    execSQL(
        """
        INSERT INTO report_drafts(
            id,
            siteId,
            originSessionId,
            originSectorId,
            originWorkflowType,
            title,
            observation,
            revision,
            createdAtEpochMillis,
            updatedAtEpochMillis
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf(
            id,
            siteId,
            originSessionId,
            originSectorId,
            originWorkflowType,
            "Draft $id",
            "",
            1,
            createdAtEpochMillis,
            updatedAtEpochMillis
        )
    )
}

private fun SupportSQLiteDatabase.insertXfeederSession(
    id: String,
    siteId: String,
    sectorId: String
) {
    execSQL(
        """
        INSERT INTO xfeeder_sessions(
            id,
            siteId,
            sectorId,
            sectorCode,
            status,
            sectorOutcome,
            notes,
            resultSummary,
            closureRelatedSectorCode,
            closureUnreliableReason,
            closureObservedSectorCount,
            createdAtEpochMillis,
            updatedAtEpochMillis,
            completedAtEpochMillis
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf(
            id,
            siteId,
            sectorId,
            "S0",
            "CREATED",
            "NOT_TESTED",
            "",
            "",
            "",
            null,
            null,
            100L,
            100L,
            null
        )
    )
}

private fun SupportSQLiteDatabase.insertXfeederSessionV11(
    id: String,
    siteId: String,
    sectorId: String
) {
    execSQL(
        """
        INSERT INTO xfeeder_sessions(
            id,
            siteId,
            sectorId,
            sectorCode,
            measurementZoneRadiusMeters,
            measurementZoneExtensionReason,
            proximityModeEnabled,
            status,
            sectorOutcome,
            notes,
            resultSummary,
            closureRelatedSectorCode,
            closureUnreliableReason,
            closureObservedSectorCount,
            createdAtEpochMillis,
            updatedAtEpochMillis,
            completedAtEpochMillis
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf(
            id,
            siteId,
            sectorId,
            "S0",
            120,
            "",
            0,
            "CREATED",
            "NOT_TESTED",
            "",
            "",
            "",
            null,
            null,
            100L,
            100L,
            null
        )
    )
}

private fun SupportSQLiteDatabase.insertXfeederSessionV12(
    id: String,
    siteId: String,
    sectorId: String,
    proximityReferenceAltitudeMeters: Double?
) {
    execSQL(
        """
        INSERT INTO xfeeder_sessions(
            id,
            siteId,
            sectorId,
            sectorCode,
            measurementZoneRadiusMeters,
            measurementZoneExtensionReason,
            proximityModeEnabled,
            proximityReferenceAltitudeMeters,
            status,
            sectorOutcome,
            notes,
            resultSummary,
            closureRelatedSectorCode,
            closureUnreliableReason,
            closureObservedSectorCount,
            createdAtEpochMillis,
            updatedAtEpochMillis,
            completedAtEpochMillis
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf(
            id,
            siteId,
            sectorId,
            "S0",
            120,
            "",
            0,
            proximityReferenceAltitudeMeters,
            "CREATED",
            "NOT_TESTED",
            "",
            "",
            "",
            null,
            null,
            100L,
            100L,
            null
        )
    )
}

private fun SupportSQLiteDatabase.insertRetSession(
    id: String,
    siteId: String,
    sectorId: String
) {
    execSQL(
        """
        INSERT INTO ret_sessions(
            id,
            siteId,
            sectorId,
            sectorCode,
            status,
            resultOutcome,
            notes,
            resultSummary,
            createdAtEpochMillis,
            updatedAtEpochMillis,
            completedAtEpochMillis
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf(
            id,
            siteId,
            sectorId,
            "S0",
            "CREATED",
            "NOT_RUN",
            "",
            "",
            100L,
            100L,
            null
        )
    )
}

private fun SupportSQLiteDatabase.insertPerformanceSessionV17(
    id: String,
    siteId: String,
    workflowType: String
) {
    execSQL(
        """
        INSERT INTO performance_sessions(
            id,
            siteId,
            siteCode,
            workflowType,
            operatorName,
            technology,
            status,
            prerequisiteNetworkReady,
            prerequisiteBatterySufficient,
            prerequisiteLocationReady,
            throughputDownloadMbps,
            throughputUploadMbps,
            throughputLatencyMs,
            throughputMinDownloadMbps,
            throughputMinUploadMbps,
            throughputMaxLatencyMs,
            qosScriptId,
            qosScriptName,
            qosConfiguredRepeatCount,
            qosTestFamiliesCsv,
            qosTargetTechnology,
            qosTargetPhoneNumber,
            qosIterationCount,
            qosSuccessCount,
            qosFailureCount,
            notes,
            resultSummary,
            createdAtEpochMillis,
            updatedAtEpochMillis,
            completedAtEpochMillis
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf(
            id,
            siteId,
            "SITE-1",
            workflowType,
            null,
            null,
            "IN_PROGRESS",
            1,
            1,
            1,
            null,
            null,
            null,
            null,
            null,
            null,
            "qos-script-latency-throughput",
            "Latence + Débit",
            1,
            "THROUGHPUT_LATENCY",
            "4G",
            null,
            0,
            0,
            0,
            "",
            "",
            100L,
            100L,
            null
        )
    )
}

private fun SupportSQLiteDatabase.insertPerformanceSessionV19(
    id: String,
    siteId: String,
    workflowType: String
) {
    execSQL(
        """
        INSERT INTO performance_sessions(
            id,
            siteId,
            siteCode,
            workflowType,
            operatorName,
            technology,
            status,
            prerequisiteNetworkReady,
            prerequisiteBatterySufficient,
            prerequisiteLocationReady,
            throughputDownloadMbps,
            throughputUploadMbps,
            throughputLatencyMs,
            throughputMinDownloadMbps,
            throughputMinUploadMbps,
            throughputMaxLatencyMs,
            qosScriptId,
            qosScriptName,
            qosConfiguredRepeatCount,
            qosConfiguredTechnologiesCsv,
            qosScriptSnapshotUpdatedAtEpochMillis,
            qosTestFamiliesCsv,
            qosTargetTechnology,
            qosTargetPhoneNumber,
            qosIterationCount,
            qosSuccessCount,
            qosFailureCount,
            notes,
            resultSummary,
            createdAtEpochMillis,
            updatedAtEpochMillis,
            completedAtEpochMillis
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf(
            id,
            siteId,
            "SITE-1",
            workflowType,
            null,
            null,
            "IN_PROGRESS",
            1,
            1,
            1,
            null,
            null,
            null,
            null,
            null,
            null,
            "qos-script-latency-throughput",
            "Latence + Débit",
            1,
            "4G,5G",
            100L,
            "THROUGHPUT_LATENCY",
            "4G",
            null,
            0,
            0,
            0,
            "",
            "",
            100L,
            100L,
            null
        )
    )
}

private fun SupportSQLiteDatabase.longQuery(sql: String): Long {
    query(sql).use { cursor ->
        check(cursor.moveToFirst()) { "Expected a row for query: $sql" }
        return cursor.getLong(0)
    }
}

private fun SupportSQLiteDatabase.stringQuery(sql: String): String {
    query(sql).use { cursor ->
        check(cursor.moveToFirst()) { "Expected a row for query: $sql" }
        return cursor.getString(0)
    }
}
