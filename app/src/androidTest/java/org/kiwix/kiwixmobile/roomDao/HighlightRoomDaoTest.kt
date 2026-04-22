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

package org.kiwix.kiwixmobile.roomDao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.dao.HighlightRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.HighlightRoomEntity
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase

@RunWith(AndroidJUnit4::class)
class HighlightRoomDaoTest {
  private lateinit var kiwixRoomDatabase: KiwixRoomDatabase
  private lateinit var highlightRoomDao: HighlightRoomDao

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    kiwixRoomDatabase = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java).build()
    highlightRoomDao = kiwixRoomDatabase.highlightRoomDao()
  }

  @After
  fun tearDown() {
    kiwixRoomDatabase.close()
  }

  @Test
  fun testHighlightRoomDao() =
    runBlocking {
      val highlight = HighlightRoomEntity(
        zimId = "test-zim-id",
        url = "http://kiwix.app/MainPage",
        highlightText = "Sample highlighted text",
        rangeJSON = "{\"startOffset\":0, \"endOffset\":10}",
        color = 0xFFFF00
      )

      // Save and retrieve a highlight
      val id = highlightRoomDao.saveHighlight(highlight)
      var highlights = highlightRoomDao.getHighlights("test-zim-id", "http://kiwix.app/MainPage").first()
      assertEquals(1, highlights.size)
      with(highlights.first()) {
        assertThat(zimId, equalTo(highlight.zimId))
        assertThat(url, equalTo(highlight.url))
        assertThat(highlightText, equalTo(highlight.highlightText))
        assertThat(rangeJSON, equalTo(highlight.rangeJSON))
        assertThat(color, equalTo(highlight.color))
      }

      // Test delete
      highlightRoomDao.deleteHighlight(id)
      highlights = highlightRoomDao.getHighlights("test-zim-id", "http://kiwix.app/MainPage").first()
      assertEquals(0, highlights.size)

      // Test deleteHighlightsForPage
      highlightRoomDao.saveHighlight(highlight)
      highlightRoomDao.deleteHighlightsForPage("test-zim-id", "http://kiwix.app/MainPage")
      highlights = highlightRoomDao.getHighlights("test-zim-id", "http://kiwix.app/MainPage").first()
      assertEquals(0, highlights.size)
    }
}
