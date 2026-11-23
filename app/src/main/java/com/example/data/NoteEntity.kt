package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val title: String = "",
  val content: String = "",
  val color: String = "plain", // plain, yellow, lavender, mint, peach, blue, pink
  val isPinned: Boolean = false,
  val isArchived: Boolean = false,
  val isTrashed: Boolean = false,
  val isHidden: Boolean = false,
  val labels: String = "", // comma separated tags
  val checklistJson: String = "", // JSON list of checklist items
  val reminderTime: Long = 0L,
  val sortOrder: Int = 0,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
