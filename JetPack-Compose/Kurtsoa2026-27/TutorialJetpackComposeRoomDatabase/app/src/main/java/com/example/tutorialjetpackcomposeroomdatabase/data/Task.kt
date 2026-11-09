package com.example.tutorialjetpackcomposeroomdatabase.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val priority: Int = 0,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)