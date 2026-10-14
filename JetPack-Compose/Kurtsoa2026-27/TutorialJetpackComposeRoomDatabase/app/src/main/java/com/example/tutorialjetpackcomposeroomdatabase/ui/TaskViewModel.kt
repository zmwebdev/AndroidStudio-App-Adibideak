package com.example.tutorialjetpackcomposeroomdatabase.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tutorialjetpackcomposeroomdatabase.data.AppDatabase
import com.example.tutorialjetpackcomposeroomdatabase.data.Task
import com.example.tutorialjetpackcomposeroomdatabase.data.TaskDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    // Flow from Room -> StateFlow that Compose can collect.
    val tasks: StateFlow<List<Task>> = dao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { dao.insert(Task(title = title.trim())) }
    }

    fun toggleDone(task: Task) {
        viewModelScope.launch { dao.update(task.copy(isDone = !task.isDone)) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { dao.delete(task) }
    }

    fun clearCompleted() {
        viewModelScope.launch { dao.deleteCompleted() }
    }

    companion object {
        // Factory: tells Android how to build the ViewModel with its dependency.
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                TaskViewModel(AppDatabase.getInstance(app).taskDao())
            }
        }
    }
}