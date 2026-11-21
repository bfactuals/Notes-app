package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
  val activeNotes: Flow<List<NoteEntity>> = noteDao.getActiveNotes()
  val hiddenNotes: Flow<List<NoteEntity>> = noteDao.getHiddenNotes()
  val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
  val trashedNotes: Flow<List<NoteEntity>> = noteDao.getTrashedNotes()
  val reminderNotes: Flow<List<NoteEntity>> = noteDao.getReminderNotes()

  suspend fun getAllNotesSnapshot(): List<NoteEntity> = noteDao.getAllNotesSnapshot()

  suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

  suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)

  suspend fun insertNotes(notes: List<NoteEntity>) = noteDao.insertNotes(notes)

  suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)

  suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)

  suspend fun emptyTrash() = noteDao.emptyTrash()
}
