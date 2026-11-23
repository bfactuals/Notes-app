package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
  @Query("SELECT * FROM notes WHERE isArchived = 0 AND isTrashed = 0 AND isHidden = 0 ORDER BY isPinned DESC, sortOrder ASC, updatedAt DESC")
  fun getActiveNotes(): Flow<List<NoteEntity>>

  @Query("SELECT * FROM notes WHERE isHidden = 1 AND isArchived = 0 AND isTrashed = 0 ORDER BY sortOrder ASC, updatedAt DESC")
  fun getHiddenNotes(): Flow<List<NoteEntity>>

  @Query("SELECT * FROM notes WHERE isArchived = 1 AND isTrashed = 0 AND isHidden = 0 ORDER BY updatedAt DESC")
  fun getArchivedNotes(): Flow<List<NoteEntity>>

  @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY updatedAt DESC")
  fun getTrashedNotes(): Flow<List<NoteEntity>>

  @Query("SELECT * FROM notes WHERE reminderTime > 0 AND isTrashed = 0 AND isHidden = 0 ORDER BY reminderTime ASC")
  fun getReminderNotes(): Flow<List<NoteEntity>>

  @Query("SELECT * FROM notes")
  suspend fun getAllNotesSnapshot(): List<NoteEntity>

  @Query("SELECT * FROM notes WHERE id = :noteId")
  suspend fun getNoteById(noteId: Long): NoteEntity?

  @Query("SELECT MIN(sortOrder) FROM notes")
  suspend fun getMinSortOrder(): Int?

  @Query("UPDATE notes SET sortOrder = :sortOrder WHERE id = :noteId")
  suspend fun updateNoteSortOrder(noteId: Long, sortOrder: Int)

  @Transaction
  suspend fun updateNotePositions(orderedIds: List<Long>) {
    orderedIds.forEachIndexed { index, id ->
      updateNoteSortOrder(noteId = id, sortOrder = index)
    }
  }

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNote(note: NoteEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotes(notes: List<NoteEntity>)

  @Update
  suspend fun updateNote(note: NoteEntity)

  @Query("DELETE FROM notes WHERE id = :noteId")
  suspend fun deleteNoteById(noteId: Long)

  @Query("DELETE FROM notes WHERE isTrashed = 1")
  suspend fun emptyTrash()
}
