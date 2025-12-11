package com.example.db_room_adibidea.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.db_room_adibidea.data.AppDatabase
import com.example.db_room_adibidea.data.Note
import com.example.db_room_adibidea.data.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).noteDao()
    private val repo = NoteRepository(dao)

    val notes: LiveData<List<Note>> = repo.notes

    fun addNote(title: String, content: String) = viewModelScope.launch {
        repo.add(title, content)
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        repo.remove(note)
    }
}