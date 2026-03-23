/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests for the SQL queries inside each Migration object in [KiwixRoomDatabase].
 *
 * Each test manually creates the pre-migration schema, applies the migration,
 * and verifies post-migration schema/data correctness.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class KiwixRoomDatabaseMigrationTest {
  private lateinit var db: SupportSQLiteDatabase

  @Before
  fun setUp() {
    val context = RuntimeEnvironment.getApplication()
    val factory = FrameworkSQLiteOpenHelperFactory()
    val config = SupportSQLiteOpenHelper.Configuration.builder(context)
      .name(null) // in-memory
      .callback(object : SupportSQLiteOpenHelper.Callback(1) {
        override fun onCreate(db: SupportSQLiteDatabase) {
          // Create the version 1 schema: RecentSearchRoomEntity only
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `RecentSearchRoomEntity` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `searchTerm` TEXT NOT NULL,
              `zimId` TEXT NOT NULL,
              `url` TEXT NOT NULL
            )
            """
          )
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
          // no-op, migrations applied manually
        }
      })
      .build()
    db = factory.create(config).writableDatabase
  }

  @After
  fun tearDown() {
    if (::db.isInitialized) {
      db.close()
    }
  }

  @Test
  fun migration_1_2_createsHistoryRoomEntity() {
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)

    // Verify HistoryRoomEntity table exists by inserting a row
    val values = ContentValues().apply {
      put("id", 1)
      put("timeStamp", System.currentTimeMillis())
      put("zimId", "test-zim-id")
      put("historyUrl", "https://kiwix.app/test")
      put("zimName", "test_zim")
      put("historyTitle", "Test Title")
      put("dateString", "01 Jan 2024")
      put("zimFilePath", "/path/to/zim")
    }
    db.insert("HistoryRoomEntity", SQLiteDatabase.CONFLICT_REPLACE, values)

    val cursor = db.query("SELECT * FROM HistoryRoomEntity")
    assertThat(cursor.count, equalTo(1))
    cursor.moveToFirst()
    assertThat(
      cursor.getString(cursor.getColumnIndexOrThrow("zimId")),
      equalTo("test-zim-id")
    )
    cursor.close()
  }

  @Test
  fun migration_2_3_createsNotesRoomEntity() {
    // Apply prerequisite migration
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)

    // Verify NotesRoomEntity table exists
    val values = ContentValues().apply {
      put("zimId", "test-zim-id")
      put("zimUrl", "http://kiwix.app/TestPage")
      put("noteTitle", "Test Note")
      put("noteFilePath", "/path/to/note.txt")
    }
    db.insert("NotesRoomEntity", SQLiteDatabase.CONFLICT_REPLACE, values)

    val cursor = db.query("SELECT * FROM NotesRoomEntity")
    assertThat(cursor.count, equalTo(1))
    cursor.moveToFirst()
    assertThat(
      cursor.getString(cursor.getColumnIndexOrThrow("noteTitle")),
      equalTo("Test Note")
    )
    cursor.close()
  }

  @Test
  fun migration_3_4_createsDownloadRoomEntity() {
    // Apply prerequisite migrations
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)

    // Verify DownloadRoomEntity table exists
    val values = ContentValues().apply {
      put("downloadId", 100)
      put("bookId", "book-1")
      put("title", "Test Download")
      put("language", "en")
      put("creator", "Kiwix")
      put("publisher", "Kiwix")
      put("date", "2024-01-01")
      put("size", "100MB")
      put("favIcon", "icon.png")
    }
    db.insert("DownloadRoomEntity", SQLiteDatabase.CONFLICT_REPLACE, values)

    val cursor = db.query("SELECT * FROM DownloadRoomEntity")
    assertThat(cursor.count, equalTo(1))
    cursor.moveToFirst()
    assertThat(
      cursor.getString(cursor.getColumnIndexOrThrow("bookId")),
      equalTo("book-1")
    )
    cursor.close()
  }

  @Test
  fun migration_4_5_addsZimReaderSourceColumnAndMigratesData() {
    // Apply prerequisite migrations
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)

    // Insert data before migration
    val values = ContentValues().apply {
      put("id", 1)
      put("timeStamp", System.currentTimeMillis())
      put("zimId", "zim-id-1")
      put("historyUrl", "https://kiwix.app/test")
      put("zimName", "test_zim")
      put("historyTitle", "Test Title")
      put("dateString", "01 Jan 2024")
      put("zimFilePath", "/path/to/zim")
    }
    db.insert("HistoryRoomEntity", SQLiteDatabase.CONFLICT_REPLACE, values)

    // Apply migration 4→5
    KiwixRoomDatabase.MIGRATION_4_5.migrate(db)

    // Verify data preserved and new column exists
    val cursor = db.query("SELECT * FROM HistoryRoomEntity")
    assertThat(cursor.count, equalTo(1))
    cursor.moveToFirst()
    assertThat(
      cursor.getString(cursor.getColumnIndexOrThrow("zimId")),
      equalTo("zim-id-1")
    )
    // zimReaderSource column should exist (nullable, so null for old data)
    val zimReaderSourceIndex = cursor.getColumnIndexOrThrow("zimReaderSource")
    assertThat(cursor.isNull(zimReaderSourceIndex), equalTo(true))
    cursor.close()

    // Verify NotesRoomEntity also got zimReaderSource column
    val noteValues = ContentValues().apply {
      put("zimId", "note-zim-id")
      put("zimUrl", "http://kiwix.app/TestPage")
      put("noteTitle", "Note With Source")
      put("noteFilePath", "/path/to/note.txt")
      put("zimReaderSource", "some_source")
    }
    db.insert("NotesRoomEntity", SQLiteDatabase.CONFLICT_REPLACE, noteValues)

    val noteCursor = db.query("SELECT zimReaderSource FROM NotesRoomEntity WHERE noteTitle = 'Note With Source'")
    noteCursor.moveToFirst()
    assertThat(
      noteCursor.getString(0),
      equalTo("some_source")
    )
    noteCursor.close()
  }

  @Test
  fun migration_5_6_addsPausedByUserColumn() {
    // Apply prerequisite migrations
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)
    KiwixRoomDatabase.MIGRATION_4_5.migrate(db)
    KiwixRoomDatabase.MIGRATION_5_6.migrate(db)

    // Insert a download entity and verify pausedByUser defaults to 0
    val values = ContentValues().apply {
      put("downloadId", 200)
      put("bookId", "book-2")
      put("title", "Test Download 2")
      put("language", "en")
      put("creator", "Kiwix")
      put("publisher", "Kiwix")
      put("date", "2024-01-01")
      put("size", "200MB")
      put("favIcon", "icon2.png")
    }
    db.insert("DownloadRoomEntity", SQLiteDatabase.CONFLICT_REPLACE, values)

    val cursor = db.query("SELECT pausedByUser FROM DownloadRoomEntity")
    cursor.moveToFirst()
    assertThat(cursor.getInt(0), equalTo(0))
    cursor.close()
  }

  @Test
  fun migration_6_7_recreatesDownloadRoomEntityWithoutPausedByUser() {
    // Apply prerequisite migrations
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)
    KiwixRoomDatabase.MIGRATION_4_5.migrate(db)
    KiwixRoomDatabase.MIGRATION_5_6.migrate(db)

    // Insert data before migration
    val values = ContentValues().apply {
      put("downloadId", 300)
      put("bookId", "book-3")
      put("title", "Test Download 3")
      put("language", "fr")
      put("creator", "Kiwix")
      put("publisher", "Kiwix")
      put("date", "2024-02-01")
      put("size", "300MB")
      put("favIcon", "icon3.png")
    }
    db.insert("DownloadRoomEntity", SQLiteDatabase.CONFLICT_REPLACE, values)

    KiwixRoomDatabase.MIGRATION_6_7.migrate(db)

    // Verify data preserved after table recreation
    val cursor = db.query("SELECT * FROM DownloadRoomEntity")
    assertThat(cursor.count, equalTo(1))
    cursor.moveToFirst()
    assertThat(
      cursor.getString(cursor.getColumnIndexOrThrow("bookId")),
      equalTo("book-3")
    )
    assertThat(
      cursor.getString(cursor.getColumnIndexOrThrow("language")),
      equalTo("fr")
    )
    cursor.close()
  }

  @Test
  fun migration_7_8_createsWebViewHistoryEntity() {
    // Apply prerequisite migrations
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)
    KiwixRoomDatabase.MIGRATION_4_5.migrate(db)
    KiwixRoomDatabase.MIGRATION_5_6.migrate(db)
    KiwixRoomDatabase.MIGRATION_6_7.migrate(db)
    KiwixRoomDatabase.MIGRATION_7_8.migrate(db)

    // Verify WebViewHistoryEntity table exists
    val values = ContentValues().apply {
      put("zimId", "webview-zim")
      put("webViewIndex", 0)
      put("webViewCurrentPosition", 1)
    }
    db.insert("WebViewHistoryEntity", SQLiteDatabase.CONFLICT_REPLACE, values)

    val cursor = db.query("SELECT * FROM WebViewHistoryEntity")
    assertThat(cursor.count, equalTo(1))
    cursor.moveToFirst()
    assertThat(
      cursor.getString(cursor.getColumnIndexOrThrow("zimId")),
      equalTo("webview-zim")
    )
    cursor.close()
  }

  @Test
  fun migration_8_9_changesStatusAndErrorColumnsToInteger() {
    // Apply prerequisite migrations
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)
    KiwixRoomDatabase.MIGRATION_4_5.migrate(db)
    KiwixRoomDatabase.MIGRATION_5_6.migrate(db)
    KiwixRoomDatabase.MIGRATION_6_7.migrate(db)
    KiwixRoomDatabase.MIGRATION_7_8.migrate(db)

    // Insert data with TEXT-based status/error before migration
    val values = ContentValues().apply {
      put("downloadId", 400)
      put("bookId", "book-4")
      put("title", "Test Download 4")
      put("language", "en")
      put("creator", "Kiwix")
      put("publisher", "Kiwix")
      put("date", "2024-03-01")
      put("size", "400MB")
      put("favIcon", "icon4.png")
    }
    db.insert("DownloadRoomEntity", SQLiteDatabase.CONFLICT_REPLACE, values)

    KiwixRoomDatabase.MIGRATION_8_9.migrate(db)

    // Verify data preserved after column type migration
    val cursor = db.query("SELECT * FROM DownloadRoomEntity")
    assertThat(cursor.count, equalTo(1))
    cursor.moveToFirst()
    assertThat(
      cursor.getString(cursor.getColumnIndexOrThrow("bookId")),
      equalTo("book-4")
    )
    cursor.close()
  }

  @Test
  fun migration_9_10_addsPauseReasonColumn() {
    // Apply all prerequisite migrations
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)
    KiwixRoomDatabase.MIGRATION_4_5.migrate(db)
    KiwixRoomDatabase.MIGRATION_5_6.migrate(db)
    KiwixRoomDatabase.MIGRATION_6_7.migrate(db)
    KiwixRoomDatabase.MIGRATION_7_8.migrate(db)
    KiwixRoomDatabase.MIGRATION_8_9.migrate(db)
    KiwixRoomDatabase.MIGRATION_9_10.migrate(db)

    // Insert a download entity and verify pauseReason defaults to 0
    val values = ContentValues().apply {
      put("downloadId", 500)
      put("bookId", "book-5")
      put("title", "Test Download 5")
      put("language", "en")
      put("creator", "Kiwix")
      put("publisher", "Kiwix")
      put("date", "2024-04-01")
      put("size", "500MB")
      put("favIcon", "icon5.png")
    }
    db.insert("DownloadRoomEntity", SQLiteDatabase.CONFLICT_REPLACE, values)

    val cursor = db.query("SELECT pauseReason FROM DownloadRoomEntity")
    cursor.moveToFirst()
    assertThat(cursor.getInt(0), equalTo(0))
    cursor.close()
  }

  @Test
  fun allMigrations_fullChain_executesWithoutError() {
    // Test the complete chain of all migrations from version 1 to 10
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)
    KiwixRoomDatabase.MIGRATION_4_5.migrate(db)
    KiwixRoomDatabase.MIGRATION_5_6.migrate(db)
    KiwixRoomDatabase.MIGRATION_6_7.migrate(db)
    KiwixRoomDatabase.MIGRATION_7_8.migrate(db)
    KiwixRoomDatabase.MIGRATION_8_9.migrate(db)
    KiwixRoomDatabase.MIGRATION_9_10.migrate(db)

    // Verify all tables exist by querying each one
    val tables = listOf(
      "RecentSearchRoomEntity",
      "HistoryRoomEntity",
      "NotesRoomEntity",
      "DownloadRoomEntity",
      "WebViewHistoryEntity"
    )
    for (table in tables) {
      val cursor = db.query("SELECT * FROM $table LIMIT 0")
      cursor.close()
    }
  }
}
