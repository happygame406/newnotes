package com.example.newnotes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteDao = NoteDatabase.getDatabase(application).noteDao()
    val allNotes = noteDao.getAllNotes()

    fun insertNote(title: String, content: String) {
        viewModelScope.launch {
            noteDao.insertNote(
                Note(
                    title = title,
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }
}