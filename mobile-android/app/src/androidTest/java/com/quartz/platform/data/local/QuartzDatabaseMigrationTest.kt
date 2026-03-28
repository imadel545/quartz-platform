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
