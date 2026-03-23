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

package org.kiwix.kiwixmobile.core.dao

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Test
import org.kiwix.kiwixmobile.core.dao.entities.HistoryRoomEntity
import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import java.io.File

class HistoryRoomDaoConvertsTest {
  private val converter = HistoryRoomDaoCoverts()

  @Test
  fun fromHistoryRoomEntity_convertsAllFieldsCorrectly() {
    val entity = HistoryRoomEntity(
      id = 42L,
      zimId = "test-zim-id",
      zimName = "test_zim",
      zimFilePath = "/path/to/zim",
      zimReaderSource = ZimReaderSource(File("/path/to/zim.zim")),
      favicon = "favicon_data",
      historyUrl = "https://kiwix.app/A/TestPage",
      historyTitle = "Test Page Title",
      dateString = "15 Mar 2024",
      timeStamp = 1710504000000L
    )

    val result = converter.fromHistoryRoomEntity(entity)

    // Should produce a HistoryListItem.HistoryItem
    val historyItem = result as HistoryListItem.HistoryItem
    assertThat(historyItem.databaseId, equalTo(42L))
    assertThat(historyItem.zimId, equalTo("test-zim-id"))
    assertThat(historyItem.zimName, equalTo("test_zim"))
    assertThat(historyItem.historyUrl, equalTo("https://kiwix.app/A/TestPage"))
    assertThat(historyItem.title, equalTo("Test Page Title"))
    assertThat(historyItem.dateString, equalTo("15 Mar 2024"))
    assertThat(historyItem.timeStamp, equalTo(1710504000000L))
    assertThat(historyItem.favicon, equalTo("favicon_data"))
  }

  @Test
  fun fromHistoryRoomEntity_handlesNullFields() {
    val entity = HistoryRoomEntity(
      id = 1L,
      zimId = "zim-id",
      zimName = "zim_name",
      zimFilePath = null,
      zimReaderSource = null,
      favicon = null,
      historyUrl = "https://kiwix.app/A/Page",
      historyTitle = "Page",
      dateString = "01 Jan 2024",
      timeStamp = 1704067200000L
    )

    val result = converter.fromHistoryRoomEntity(entity) as HistoryListItem.HistoryItem
    assertThat(result.favicon, equalTo(null))
  }

  @Test
  fun historyItemToHistoryListItem_convertsAllFieldsCorrectly() {
    val historyItem = HistoryListItem.HistoryItem(
      databaseId = 99L,
      zimId = "convert-zim-id",
      zimName = "convert_zim",
      historyUrl = "https://kiwix.app/A/ConvertPage",
      title = "Convert Page",
      zimReaderSource = ZimReaderSource(File("/storage/emulated/0/Downloads/test.zim")),
      favicon = "convert_favicon",
      dateString = "20 Mar 2024",
      timeStamp = 1710936000000L
    )

    val result = converter.historyItemToHistoryListItem(historyItem)

    assertThat(result.id, equalTo(99L))
    assertThat(result.zimId, equalTo("convert-zim-id"))
    assertThat(result.zimName, equalTo("convert_zim"))
    assertThat(result.historyUrl, equalTo("https://kiwix.app/A/ConvertPage"))
    assertThat(result.historyTitle, equalTo("Convert Page"))
    assertThat(result.dateString, equalTo("20 Mar 2024"))
    assertThat(result.timeStamp, equalTo(1710936000000L))
    assertThat(result.favicon, equalTo("convert_favicon"))
    assertThat(
      result.zimReaderSource,
      equalTo(ZimReaderSource(File("/storage/emulated/0/Downloads/test.zim")))
    )
  }

  @Test
  fun historyItemToHistoryListItem_handlesNullFavicon() {
    val historyItem = HistoryListItem.HistoryItem(
      databaseId = 0L,
      zimId = "zim-id",
      zimName = "zim_name",
      historyUrl = "https://kiwix.app/A/Page",
      title = "Page",
      zimReaderSource = null,
      favicon = null,
      dateString = "01 Jan 2024",
      timeStamp = 1704067200000L
    )

    val result = converter.historyItemToHistoryListItem(historyItem)
    assertThat(result.favicon, equalTo(null))
    assertThat(result.zimReaderSource, equalTo(null))
  }

  @Test
  fun roundTrip_entityToItemAndBack() {
    val originalEntity = HistoryRoomEntity(
      id = 7L,
      zimId = "roundtrip-zim",
      zimName = "roundtrip_zim_name",
      zimFilePath = null,
      zimReaderSource = ZimReaderSource(File("/path/to/roundtrip.zim")),
      favicon = "rt_favicon",
      historyUrl = "https://kiwix.app/A/RoundTrip",
      historyTitle = "Round Trip Page",
      dateString = "25 Mar 2024",
      timeStamp = 1711368000000L
    )

    // Entity → HistoryItem → Entity
    val historyItem = converter.fromHistoryRoomEntity(originalEntity) as HistoryListItem.HistoryItem
    val resultEntity = converter.historyItemToHistoryListItem(historyItem)

    assertThat(resultEntity.zimId, equalTo(originalEntity.zimId))
    assertThat(resultEntity.zimName, equalTo(originalEntity.zimName))
    assertThat(resultEntity.historyUrl, equalTo(originalEntity.historyUrl))
    assertThat(resultEntity.historyTitle, equalTo(originalEntity.historyTitle))
    assertThat(resultEntity.dateString, equalTo(originalEntity.dateString))
    assertThat(resultEntity.timeStamp, equalTo(originalEntity.timeStamp))
    assertThat(resultEntity.favicon, equalTo(originalEntity.favicon))
  }
}
