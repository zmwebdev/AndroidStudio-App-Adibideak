package com.example.db_room_adibidea

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.db_room_adibidea.databinding.ActivityMainBinding
import com.example.db_room_adibidea.ui.NoteAdapter
import com.example.db_room_adibidea.ui.NoteViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: NoteViewModel by viewModels()
    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = NoteAdapter(emptyList()) { note ->
            viewModel.deleteNote(note)
        }

        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.adapter = adapter

        binding.btnAdd.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val content = binding.etContent.text.toString().trim()
            if (title.isNotEmpty() || content.isNotEmpty()) {
                viewModel.addNote(title, content)
                binding.etTitle.text?.clear()
                binding.etContent.text?.clear()
            }
        }

        viewModel.notes.observe(this) { notes ->
            adapter.submitList(notes)
        }
    }
}