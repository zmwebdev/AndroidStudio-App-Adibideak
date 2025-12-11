package com.example.todoapp

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todoapp.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TaskAdapter
    private val tasks = mutableListOf<Task>()
    private val fileName = "tasks.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadTasks()

        adapter = TaskAdapter(
            tasks,
            onTaskCheckedChange = { saveTasks() },
            onTaskLongClick = { position -> confirmDelete(position) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.addButton.setOnClickListener {
            val title = binding.taskInput.text.toString().trim()
            if (title.isNotEmpty()) {
                val newTask = Task(title, false)
                tasks.add(newTask)
                adapter.notifyItemInserted(tasks.size - 1)
                binding.taskInput.text.clear()
                saveTasks()
            }
        }
    }

    private fun confirmDelete(position: Int) {
        val task = tasks[position]
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Delete \"${task.title}\"?")
            .setPositiveButton("Yes") { _, _ ->
                tasks.removeAt(position)
                adapter.notifyItemRemoved(position)
                saveTasks()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadTasks() {
        val file = File(filesDir, fileName)
        val jsonString: String = if (file.exists()) {
            file.readText()
        } else {
            JsonUtils.loadJSONFromRaw(this, R.raw.tasks).also {
                file.writeText(it)
            }
        }

        val jsonArray = JSONArray(jsonString)
        tasks.clear()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            tasks.add(Task(obj.getString("title"), obj.getBoolean("done")))
        }
    }

    private fun saveTasks() {
        val jsonArray = JSONArray()
        for (task in tasks) {
            val obj = JSONObject()
            obj.put("title", task.title)
            obj.put("done", task.done)
            jsonArray.put(obj)
        }
        val file = File(filesDir, fileName)
        file.writeText(jsonArray.toString())
    }
}