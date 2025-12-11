package com.example.db_room_adibidea.data

class NoteRepository(private val dao: NoteDao) {
    val notes = dao.getAll()

    suspend fun add(title: String, content: String) {
        dao.insert(Note(title = title, content = content))
    }

    suspend fun remove(note: Note) {
        dao.delete(note)
    }
}