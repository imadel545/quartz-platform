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
