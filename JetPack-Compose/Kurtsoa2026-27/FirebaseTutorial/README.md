# Jetpack Compose + Cloud Firestore: Step-by-Step Tutorial

Claude: https://claude.ai/share/beec9bce-d3b0-47e2-b852-da2a484ed877

A hands-on guide for students who already know Jetpack Compose but have never used Firebase.

## What we will build

A real-time **To-Do list** app. Tasks are stored in the cloud (Cloud Firestore), and the UI updates automatically whenever the data changes, even if you change it from another device or from the Firebase console.

You will learn how to:

- Create a Firebase project and connect it to an Android app
- Add the Firebase dependencies with Gradle
- Read data in real time and expose it to Compose
- Create, update and delete documents
- Protect your data with security rules

**Prerequisites:** Android Studio (recent version), a Google account, basic Kotlin, Compose and ViewModel knowledge.

## Step 0: Which Firebase database?

Firebase offers two databases:

|  | Cloud Firestore | Realtime Database |
| --- | --- | --- |
| Data model | Collections of documents | One big JSON tree |
| Queries | Rich (filter, order, combine) | Limited |
| Offline support | Yes | Yes |
| Recommended for new apps | **Yes** | Only for very simple or very high-frequency data |

In this tutorial we use **Cloud Firestore**.

## Step 1: Create the Firebase project

1. Go to <https://console.firebase.google.com> and sign in with your Google account.
2. Click **Create a project** and give it a name (for example `compose-todo`).
3. You can disable Google Analytics for this tutorial. Click **Create project**.

## Step 2: Create the Android project

1. In Android Studio: **New Project > Empty Activity** (the Compose template).
2. Name it `ComposeTodo`. Choose a package name such as `com.example.composetodo`.
3. **Remember the package name exactly.** Firebase uses it to identify your app.

## Step 3: Register the app in Firebase

1. In the Firebase console, click the **Android icon** ("Add app") on the project overview page.
2. Enter the **package name** from Step 2. A nickname is optional. A SHA-1 key is not needed for Firestore.
3. Click **Register app**.
4. Download **google-services.json**.
5. In Android Studio, switch the Project view to **Project** (not *Android*) and copy the file into the `app/` folder, next to `app/build.gradle.kts`.

> This file contains your project identifiers and API keys. It is how the app knows which Firebase project to talk to. Without it the app will crash on start.

## Step 4: Configure Gradle

### 4.1 Project-level `build.gradle.kts` (root)

Add the Google Services plugin (declared, but not applied here):

```kotlin
plugins {
    // ... existing plugins
    id("com.google.gms.google-services") version "4.5.0" apply false
}
```

### 4.2 App-level `app/build.gradle.kts`

Apply the plugin and add the dependencies:

```kotlin
plugins {
    // ... existing plugins
    id("com.google.gms.google-services")
}

dependencies {
    // Firebase: the BoM manages the versions of all Firebase libraries
    implementation(platform("com.google.firebase:firebase-bom:35.0.0"))
    implementation("com.google.firebase:firebase-firestore")

    // Lets us use .await() on Firebase tasks inside coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // ViewModel + collectAsStateWithLifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Icons.Default.Delete (used in Step 5). Version comes from the Compose BOM.
    implementation("androidx.compose.material:material-icons-core")

    // ... existing dependencies
}
```

> **Tip:** the version numbers above are examples. Check the Firebase release notes for the latest BoM version. Because we use the **BoM** (Bill of Materials), the Firebase libraries themselves have no version number.

Click **Sync Now**.

### 4.3 Internet permission

In `AndroidManifest.xml`, before `<application>`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Step 5: Create the Firestore database

1. In the Firebase console go to **Build > Firestore Database > Create database**.
2. Choose a location close to your users (for example `eur3` for Europe).
3. Choose **Start in test mode** (open access for 30 days). We will secure it in Step 11.
4. Click **Create**.

You do **not** need to create the collection manually. Firestore creates it when the first document is written.

### Firestore in one minute

- A **collection** is a list of documents (like a table). Ours will be called `todos`.
- A **document** is a set of key/value fields (like a row or a JSON object), with a unique ID.
- Documents can be mapped directly to Kotlin data classes.

## Step 6: The data model

Create `TodoItem.kt`:

```kotlin
package com.example.composetodo

import com.google.firebase.firestore.DocumentId

data class TodoItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

Important points:

- **All properties need default values.** Firestore creates the object through an empty constructor and then fills in the fields.
- `@DocumentId` tells Firestore to put the document's ID in this property (it is not stored as a field).
- We named it `TodoItem` (not `Task`) to avoid a name clash with Firebase's own `Task` class.

## Step 7: The repository

The repository is the only class that talks to Firebase. Create `TodoRepository.kt`:

```kotlin
package com.example.composetodo

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class TodoRepository {

    private val collection = Firebase.firestore.collection("todos")

    // READ: a Flow that emits a new list every time the data changes
    fun observeItems(): Flow<List<TodoItem>> = callbackFlow {
        val listener = collection
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects<TodoItem>() ?: emptyList())
            }
        // Called when nobody collects the Flow anymore: stop listening
        awaitClose { listener.remove() }
    }

    // CREATE
    fun add(title: String) {
        collection.add(TodoItem(title = title))
    }

    // UPDATE
    fun setDone(id: String, done: Boolean) {
        collection.document(id).update("done", done)
    }

    // DELETE
    fun delete(id: String) {
        collection.document(id).delete()
    }
}
```

What is happening here:

- `addSnapshotListener` registers a **real-time listener**. Firestore calls it once with the current data and again after every change.
- `callbackFlow` converts this callback-based API into a Kotlin `Flow`, which fits perfectly with Compose and ViewModels.
- `awaitClose { listener.remove() }` is essential. Without it the listener would keep running forever (a memory and billing leak).
- Writes (`add`, `update`, `delete`) are applied **locally first**, so the UI updates instantly, even offline. Firestore syncs them to the server when possible. That is why we do not need to wait for results here.

## Step 8: The ViewModel

Create `TodoViewModel.kt`:

```kotlin
package com.example.composetodo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

class TodoViewModel(
    private val repository: TodoRepository = TodoRepository()
) : ViewModel() {

    val items: StateFlow<List<TodoItem>> = repository.observeItems()
        .catch { emit(emptyList()) }   // e.g. permission denied
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun add(title: String) = repository.add(title)
    fun setDone(item: TodoItem, done: Boolean) = repository.setDone(item.id, done)
    fun delete(item: TodoItem) = repository.delete(item.id)
}
```

`stateIn` turns the cold `Flow` into a `StateFlow`. With `WhileSubscribed(5_000)` the Firestore listener is active only while the screen is visible (plus 5 seconds to survive screen rotations).

## Step 9: The UI in Compose

Create `TodoScreen.kt`:

```kotlin
package com.example.composetodo

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(viewModel: TodoViewModel = viewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var newTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My To-Dos") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
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
                    if (newTitle.isNotBlank()) {
                        viewModel.add(newTitle.trim())
                        newTitle = ""
                    }
                }) { Text("Add") }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn {
                items(items, key = { it.id }) { item ->
                    TodoRow(
                        item = item,
                        onCheckedChange = { viewModel.setDone(item, it) },
                        onDelete = { viewModel.delete(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun TodoRow(
    item: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.done, onCheckedChange = onCheckedChange)
        Text(
            text = item.title,
            modifier = Modifier.weight(1f),
            textDecoration = if (item.done) TextDecoration.LineThrough else null
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}
```

Notice that the UI has **no Firebase code at all**. It only observes a `StateFlow` and calls ViewModel functions. This is why we separated the layers.

Now show the screen in `MainActivity.kt`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TodoScreen()
            }
        }
    }
}
```

(Use the theme generated by your project, for example `ComposeTodoTheme`, if you prefer.)

## Step 10: Run and test

1. Run the app on an emulator or a real device **with internet access**.
2. Add a few tasks. Check and delete some.
3. Open the Firebase console, **Firestore Database > Data**. You should see the `todos` collection with your documents.
4. **Real-time experiment:** keep the app visible, and in the console edit a document's `title` or `done` field. The app updates by itself without any refresh code.
5. **Offline experiment:** enable airplane mode, add a task (it appears immediately), then disable airplane mode and watch it appear in the console.

## Step 11: Security rules (very important)

Test mode lets **anyone** read and write your database, and it expires after 30 days. Never publish an app like that.

In the console go to **Firestore Database > Rules**. A minimal rule that only allows signed-in users:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /todos/{todoId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

This requires **Firebase Authentication**, which is the natural next lesson. Until then, keep test mode for class exercises, and never store personal data.

## Common problems

| Symptom | Likely cause and fix |
| --- | --- |
| App crashes at start: *Default FirebaseApp is not initialized* | `google-services.json` missing or in the wrong folder, or the `google-services` plugin is not applied in `app/build.gradle.kts`. |
| `No matching client found for package name` | The package name in `google-services.json` differs from your `applicationId`. |
| List always empty and Logcat shows `PERMISSION_DENIED` | Rules expired or are too strict. Check the Rules tab. |
| Fields come back empty or default | Data class property names differ from the field names in Firestore, or properties have no default values. |
| Nothing changes after editing in the console | The listener is not active (screen not visible) or the device has no internet. |
| Gradle sync fails on Firebase libraries | Check that the BoM is declared with `platform(...)` and that Google's Maven repository is available (it is by default in new projects). |

## Exercises for students

1. Add a **loading state**: show a `CircularProgressIndicator` until the first data arrives.
2. Add an **edit** feature: tap a task to change its title (`update("title", ...)`).
3. Add a **filter** (All / Pending / Done) using `whereEqualTo("done", false)`. Firestore may ask you to create an index; follow the link in Logcat.
4. Show a message when the list is empty.
5. **Challenge:** add Firebase Authentication and store each user's tasks in `users/{uid}/todos`, then update the security rules so users only access their own data.

## Appendix: Using Realtime Database instead

The architecture (Repository, ViewModel, Compose UI) stays identical. Only the repository changes:

- Dependency: `com.google.firebase:firebase-database`
- Reference: `Firebase.database.getReference("todos")`
- Listener: `addValueEventListener(...)` instead of `addSnapshotListener`
- You must create the database in the console (Build > Realtime Database) and, in test mode, set its rules accordingly.

## Summary

1. Create a Firebase project and register the app.
2. Add `google-services.json`, the plugin and the BoM dependencies.
3. Create the Firestore database.
4. Model data with a data class that has default values.
5. Wrap Firebase in a repository that exposes a `Flow`.
6. Collect it as state in the ViewModel and Compose.
7. Secure the database with rules before publishing.
