package com.example.todoapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todoapp.databinding.ItemTaskBinding

class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val onTaskCheckedChange: (Task) -> Unit,
    private val onTaskLongClick: (Int) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.binding.taskTitle.text = task.title
        holder.binding.taskCheckbox.setOnCheckedChangeListener(null)
        holder.binding.taskCheckbox.isChecked = task.done

        holder.binding.taskCheckbox.setOnCheckedChangeListener { _, isChecked ->
            task.copy(done = isChecked).also {
                tasks[position] = it
                onTaskCheckedChange(it)
            }
        }

        // 👇 Long press to delete
        holder.itemView.setOnLongClickListener {
            onTaskLongClick(position)
            true
        }
    }

    override fun getItemCount() = tasks.size
}