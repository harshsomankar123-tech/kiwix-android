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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.kiwix.kiwixmobile.core.dao.entities.HighlightRoomEntity

@Dao
interface HighlightRoomDao {
  @Query("SELECT * FROM HighlightRoomEntity WHERE zimId = :zimId AND url = :url")
  fun getHighlights(zimId: String, url: String): Flow<List<HighlightRoomEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun saveHighlight(highlight: HighlightRoomEntity): Long

  @Query("DELETE FROM HighlightRoomEntity WHERE id = :id")
  fun deleteHighlight(id: Long)

  @Query("DELETE FROM HighlightRoomEntity WHERE zimId = :zimId AND url = :url AND rangeJSON = :rangeJSON")
  fun deleteHighlightByRange(zimId: String, url: String, rangeJSON: String)
}
