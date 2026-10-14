# Tutorial: Jetpack Compose + Room Database


Claude: "I need a tutorial (step-by-step) to create an Android Jetpack Compose app using Room DB. Students knows Jetpack Compose, but they have no idea about how to integrate Room."

https://claude.ai/share/7ebd15ba-16fe-40f7-843c-e40eebf763f8

*Build a persistent To-Do app step by step*

**Audience:** students who already know Jetpack Compose (state, `remember`, `LazyColumn`, `ViewModel` basics) but have never used Room. **Time:** about 90 minutes. **Result:** a task list app that survives app restarts, updates the UI automatically when data changes, and uses a clean architecture.

---

## 1. What is Room and why do we need it?

So far, anything stored in `remember` or in a `ViewModel` disappears when the app process is killed. To keep data permanently we need a database. Android includes SQLite, but using it directly means writing a lot of boilerplate and raw SQL strings with no compile-time checks.

**Room** is a library from Google that sits on top of SQLite. You describe your data with Kotlin classes and annotations, and Room generates the SQLite code for you. It also checks your SQL queries **at compile time**, so typos become build errors instead of crashes.

Room has exactly three main pieces. Memorize these:

| Piece | Annotation | What it is | Analogy |
| --- | --- | --- | --- |
| **Entity** | `@Entity` | A Kotlin data class = one table | A row template |
| **DAO** (Data Access Object) | `@Dao` | An interface with the operations (insert, query, delete...) | The "menu" of things you can ask the database |
| **Database** | `@Database` | An abstract class that ties entities and DAOs together | The database itself |

How they connect to Compose:

```
Compose UI  <-->  ViewModel  <-->  DAO  <-->  Room Database (SQLite file)
```

The key idea of this tutorial: the DAO returns a **`Flow`**. Every time the table changes, Room emits a new list, the ViewModel exposes it as state, and Compose recomposes automatically. We never refresh the screen manually.

---

## 2. Create the project

1. In Android Studio choose **New Project → Empty Activity** (the Compose template).
2. Name it `RoomTasks`, package `com.example.roomtasks`, minimum SDK 24 or higher.
3. Let Gradle sync.

---

## 3. Add the dependencies

Room uses an annotation processor to generate code. Today the recommended one is **KSP** (Kotlin Symbol Processing), which replaces the older `kapt`.

### 3.1 Project-level `build.gradle.kts`

Add the KSP plugin. The KSP version must match your Kotlin version (the first part of the KSP version is the Kotlin version). Check the [KSP releases page](https://github.com/google/ksp/releases) for the right one.

```kotlin
plugins {
    // ...existing plugins (android application, kotlin android, compose)...
    id("com.google.devtools.ksp") version "2.3.6" apply false
}
```

### 3.2 Module-level `app/build.gradle.kts`

```kotlin
plugins {
    // ...existing plugins...
    id("com.google.devtools.ksp")
}

android {
    // ...existing config...
}

// Room can export its schema as JSON. Useful later for migrations.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // ...existing dependencies...

    val roomVersion = "2.7.2" // check the latest stable version
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")   // coroutines + Flow support
    ksp("androidx.room:room-compiler:$roomVersion")

    // ViewModel + collecting state in Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Icons.Default.Delete (used in Step 5). Version comes from the Compose BOM.
    implementation("androidx.compose.material:material-icons-core")   
}
```

Click **Sync Now**.

> **Teaching note:** `room-runtime` is the library, `room-ktx` adds coroutine/Flow support, and `room-compiler` is the code generator. Forgetting the `ksp(...)` line is the most common setup error: the app compiles but crashes with *"Cannot find implementation for AppDatabase"*.

---

## 4. Step 1: The Entity (the table)

Create a new package `data` and a file `Task.kt`:

```kotlin
package com.example.roomtasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

What each part does:

- `@Entity(tableName = "tasks")`: this class becomes a table named `tasks`. Without `tableName`, Room uses the class name.
- `@PrimaryKey(autoGenerate = true)`: every row needs a unique id. With `autoGenerate`, SQLite assigns it. That is why `id` has the default value `0`: it means "not saved yet, please generate one".
- Every property becomes a column. Room supports `Int`, `Long`, `String`, `Boolean`, `Double`, `ByteArray` and similar types out of the box. (Dates, enums, lists need a `TypeConverter`, which is an extension topic.)

---

## 5. Step 2: The DAO (the operations)

Create `TaskDao.kt` in the same package:

```kotlin
package com.example.roomtasks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // Reads: return a Flow so the UI is notified of every change.
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Task>>

    // Writes: suspend functions, Room runs them off the main thread.
    @Insert
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE isDone = 1")
    suspend fun deleteCompleted()
}
```

Things to explain to students:

1. **It is an interface.** We never write the body. Room generates the implementation.
2. **`@Insert`, `@Update`, `@Delete`** need no SQL. Room figures it out from the parameter type. `@Update` and `@Delete` match rows by primary key.
3. **`@Query`** is for anything custom. The SQL is verified at compile time. Try changing `tasks` to `taskz` and see the build fail.
4. **Why `Flow` for reads and `suspend` for writes?**
    - A `Flow<List<Task>>` is *observable*: Room re-runs the query and emits a new list whenever the `tasks` table changes.
    - `suspend` functions run on a background thread automatically. Room **forbids** database access on the main thread (it throws an exception), so you cannot freeze the UI by mistake.

---

## 6. Step 3: The Database class

Create `AppDatabase.kt`:

```kotlin
package com.example.roomtasks.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Task::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tasks.db"
                ).build().also { INSTANCE = it }
            }
    }
}
```

Key points:

- `entities = [...]` lists every table. If you add a new `@Entity` and forget to list it here, it will not exist.
- `version = 1`: the schema version. We will change it when we alter the tables (see Section 12).
- `abstract fun taskDao()`: Room implements this and gives you the DAO.
- **Singleton pattern.** A `RoomDatabase` instance is heavy, and you should have only **one per app**. `@Volatile` + `synchronized` guarantees that even if two threads ask at the same time, only one is created.
- `context.applicationContext` avoids leaking an Activity.
- `"tasks.db"` is the file name stored in the app's private storage.

> **Build now.** Press **Build → Make Project**. If everything is right, Room generates `AppDatabase_Impl` and `TaskDao_Impl` (you can find them under `app/build/generated/ksp/`). Showing students these generated files demystifies Room: it is just ordinary SQLite code someone else wrote for you.

---

## 7. Step 4: The ViewModel

The ViewModel is the bridge between the DAO and Compose. Create `TaskViewModel.kt`:

```kotlin
package com.example.roomtasks.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.roomtasks.data.AppDatabase
import com.example.roomtasks.data.Task
import com.example.roomtasks.data.TaskDao
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
```

Explain:

- **`stateIn`** converts the cold `Flow` from Room into a hot `StateFlow`, which always has a current value (ideal for Compose).
- **`WhileSubscribed(5_000)`**: keep the database subscription alive for 5 seconds after the UI stops listening. This survives screen rotation without re-querying, and stops work when the app goes to the background.
- **`viewModelScope.launch`**: we call `suspend` DAO functions from a coroutine. Room moves the actual work to a background thread.
- **The `Factory`**: a ViewModel with constructor parameters needs a factory. Here it creates the database and passes the DAO in. (In bigger apps you would replace this with Hilt, but the concept is the same.)
- Notice what is **missing**: there is no code like `tasks = dao.getAll()` after an insert. The Flow takes care of it.

---

## 8. Step 5: The Compose UI

Create `TaskScreen.kt`:

```kotlin
package com.example.roomtasks.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomtasks.data.Task

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory)) {

    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var newTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Tasks") },
                actions = {
                    TextButton(onClick = viewModel::clearCompleted) {
                        Text("Clear done")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            // Input row
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("New task") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    viewModel.addTask(newTitle)
                    newTitle = ""
                }) { Text("Add") }
            }

            Spacer(Modifier.height(16.dp))

            if (tasks.isEmpty()) {
                Text("No tasks yet. Add your first one!")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { viewModel.toggleDone(task) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete task")
            }
        }
    }
}
```

Points worth highlighting for students who already know Compose:

- `collectAsStateWithLifecycle()` turns the `StateFlow` into Compose `State`. It stops collecting when the app is in the background.
- The UI **only reads state and sends events**. It never touches the database. This is *state hoisting* and *unidirectional data flow* applied to a real data source.
- `key = { it.id }` in `items` lets Compose track each row by its database id, so animations and recomposition are efficient.
- The `newTitle` text field state is *UI state* (stays in the composable). The list of tasks is *data state* (comes from the database).

Note: `Icons.Default.Delete` needs the `material-icons-core` artifact, which the default template normally includes. If the icon is not found, add `implementation("androidx.compose.material:material-icons-core")`.

---

## 9. Step 6: Connect it in `MainActivity`

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoomTasksTheme {   // the theme name generated by your project
                TaskScreen()
            }
        }
    }
}
```

Run the app on an emulator or device.

---

## 10. Test it (the "aha" moment)

Have students do this checklist:

1. Add three tasks.
2. Tick one task and delete another. The list updates instantly.
3. **Rotate the device.** Data is still there.
4. **Force-stop the app** (long-press the icon → App info → Force stop) and reopen it. **The data is still there.** That is persistence.
5. Compare with a version using only `remember { mutableStateListOf() }`: everything disappears at step 4.

### Inspect the database with Database Inspector

1. Run the app from Android Studio.
2. Open **View → Tool Windows → App Inspection**.
3. Select the **Database Inspector** tab and choose `tasks.db`.
4. Double-click the `tasks` table to see rows. Enable **Live updates** and add a task in the app: you will see the row appear in real time. You can even edit a value in the inspector and watch the Compose UI react.

This is a great demonstration of the Flow → StateFlow → Compose chain.

---

## 11. Recap: the complete data flow

1. User taps **Add** → UI calls `viewModel.addTask(title)`.
2. ViewModel launches a coroutine → `dao.insert(...)` writes to SQLite on a background thread.
3. Room detects the `tasks` table changed → the `Flow` from `observeAll()` emits the new list.
4. `stateIn` updates the `StateFlow` → `collectAsStateWithLifecycle` updates the Compose state.
5. Compose recomposes the `LazyColumn`.

Nobody ever called "refresh". That is the power of observable queries.

---

## 12. Changing the schema: migrations

Suppose we want a `priority` column. If you just add `val priority: Int = 0` to `Task`, the app will crash on devices that already have the old database, with an error about the schema hash not matching. Room refuses to guess.

You have two options.

**During development only** (you do not care about the data):

```kotlin
Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "tasks.db")
    .fallbackToDestructiveMigration(dropAllTables = true)
    .build()
```

This wipes the database on a version change. Never ship this if users have real data.

**The correct way** (a migration): bump the version and describe the change.

```kotlin
@Database(entities = [Task::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() { /* ... */ }

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
    }
}

// ...
Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "tasks.db")
    .addMigrations(MIGRATION_1_2)
    .build()
```

The exported schema JSON files in `app/schemas/` (from the `ksp { arg(...) }` setting) are the history of every version. Commit them to Git.

---

## 13. Common mistakes (and their symptoms)

| Symptom | Likely cause |
| --- | --- |
| `Cannot find implementation for AppDatabase` | Missing `ksp("...room-compiler...")` or the KSP plugin |
| `Cannot access database on the main thread` | Calling a non-suspend, non-Flow DAO function from the UI thread |
| App crashes after changing an entity | Changed the schema without bumping `version` / adding a migration |
| List never updates | DAO returns `List<Task>` instead of `Flow<List<Task>>` (a one-time snapshot) |
| `@Update` / `@Delete` does nothing | The object's `id` is `0` or does not exist in the table |
| Two different database instances, inconsistent data | Calling `Room.databaseBuilder` multiple times instead of using a singleton |
| KSP version error at sync | KSP version does not match the Kotlin version |

---

## 14. Exercises for students

**Level 1: Practice**

1. Add a `@Query` that returns only tasks where `isDone = 0`, and show a counter *"3 pending"* in the top bar.
2. Show the creation date under each task title, formatted with `SimpleDateFormat` or `java.time`.

**Level 2: Extend**

3. Add a `priority` field (Low / Medium / High) with a proper migration from version 1 to 2.
4. Add a search `TextField` and a DAO query using `WHERE title LIKE '%' || :text || '%'`. Combine it with `flatMapLatest` in the ViewModel.

**Level 3: Architecture**

5. Introduce a `TaskRepository` class between the ViewModel and the DAO.
6. Add a second entity `Category` and a one-to-many relation (`@Relation` with an `@Embedded` data class).
7. Replace the manual factory with Hilt.

---

## 15. Suggested project structure

```
com.example.roomtasks
├── MainActivity.kt
├── data
│   ├── Task.kt            (Entity)
│   ├── TaskDao.kt         (DAO)
│   └── AppDatabase.kt     (Database)
└── ui
    ├── TaskViewModel.kt
    └── TaskScreen.kt
```

---

## 16. Key takeaways

- Room = **Entity** (table) + **DAO** (operations) + **Database** (holder).
- Return **`Flow`** from queries to get automatic UI updates; use **`suspend`** for writes.
- Keep **one** database instance and access it through a **ViewModel**, never directly from composables.
- Room checks your SQL **at compile time**.
- When the schema changes, **bump the version and write a migration**.

*Official documentation: [developer.android.com/training/data-storage/room](https://developer.android.com/training/data-storage/room). Always check for the latest Room and KSP versions before starting a new project.*
