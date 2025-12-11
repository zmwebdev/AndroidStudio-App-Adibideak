package com.example.db_room_adibidea.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.db_room_adibidea.data.Note
import com.example.db_room_adibidea.databinding.ItemNoteBinding

class NoteAdapter(
    private var items: List<Note>,
    private val onDelete: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteVH>() {

    inner class NoteVH(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteVH {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NoteVH(binding)
    }

    override fun onBindViewHolder(holder: NoteVH, position: Int) {
        val note = items[position]
        holder.binding.tvTitle.text = note.title
        holder.binding.tvContent.text = note.content
        holder.binding.btnDelete.setOnClickListener { onDelete(note) }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<Note>) {
        items = newItems
        notifyDataSetChanged()
    }
}