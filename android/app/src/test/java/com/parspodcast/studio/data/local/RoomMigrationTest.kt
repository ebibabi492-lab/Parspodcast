package com.parspodcast.studio.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class RoomMigrationTest {

    @Test
    fun testMigration1To2_executesSqlStatementsCorrectly() {
        // Mock SupportSQLiteDatabase
        val mockDb = mock(SupportSQLiteDatabase::class.java)

        // Execute migration
        ParsPodcastDatabase.MIGRATION_1_2.migrate(mockDb)

        // Verify podcast_shows table creation statement was executed
        verify(mockDb).execSQL(argThat { sql ->
            sql.contains("CREATE TABLE IF NOT EXISTS `podcast_shows`") &&
            sql.contains("`id` TEXT NOT NULL PRIMARY KEY") &&
            sql.contains("`title` TEXT NOT NULL") &&
            sql.contains("`language` TEXT NOT NULL")
        })

        // Verify hosting_configs table creation statement was executed
        verify(mockDb).execSQL(argThat { sql ->
            sql.contains("CREATE TABLE IF NOT EXISTS `hosting_configs`") &&
            sql.contains("`serverUrl` TEXT NOT NULL") &&
            sql.contains("`encryptedPassword` TEXT NOT NULL")
        })

        // Verify default show insertion
        verify(mockDb).execSQL(argThat { sql ->
            sql.contains("INSERT OR IGNORE INTO `podcast_shows`") &&
            sql.contains("'default_show'")
        })

        // Verify podcast_episodes migration to v2
        verify(mockDb).execSQL(argThat { sql ->
            sql.contains("CREATE TABLE IF NOT EXISTS `podcast_episodes_v2`")
        })

        verify(mockDb).execSQL(argThat { sql ->
            sql.contains("INSERT INTO `podcast_episodes_v2`") &&
            sql.contains("SELECT") &&
            sql.contains("FROM `podcast_episodes`")
        })

        verify(mockDb).execSQL("DROP TABLE `podcast_episodes`")
        verify(mockDb).execSQL("ALTER TABLE `podcast_episodes_v2` RENAME TO `podcast_episodes`")
    }
}
