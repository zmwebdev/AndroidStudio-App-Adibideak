# Navigation in Jetpack Compose: Step-by-Step Tutorial

Claude: https://claude.ai/share/52f56fa3-0b5f-4a83-94ac-e3765cdcaee6


A hands-on introduction for students who already know Compose basics (composables, state, state hoisting) but have never used Navigation.

## What you will build

A small app with three screens:

- **Home**: a list of items.
- **Detail**: shows one item, receiving its `id` as an argument.
- **Settings**: a simple screen reachable from the bottom bar.

By the end you will know how to declare routes, build a `NavHost`, navigate forward and back, pass arguments, manage the back stack, add a bottom navigation bar, and group screens into nested graphs.

## Step 0. The big idea

Compose has no Activities-per-screen and no Fragments. Your whole app usually lives in **one Activity** and "screens" are just composables. Navigation Compose gives you the machinery to switch between them. Learn these four words and everything else follows:

| Concept | What it is |
| --- | --- |
| **Route** | A name (here, a Kotlin type) that identifies a destination. |
| **NavHost** | A composable that shows the destination matching the current route. It is the "container" for your screens. |
| **NavController** | The object that performs navigation (`navigate`, `popBackStack`...). It owns the **back stack**. |
| **Back stack** | The list of screens visited. Pressing Back removes the top one. |

Mental model: *the NavController holds a stack of routes; the NavHost draws whichever route is on top.*

## Step 1. Create the project and add dependencies

Create a new project with the **Empty Activity** template (it already uses Compose). We will use classic **string routes**: each screen is identified by a plain text name such as `"home"`. You only need one extra dependency.

In the app module's `build.gradle.kts`, add the Navigation Compose dependency. Look up the latest stable version on the [AndroidX Navigation releases page](https://developer.android.com/jetpack/androidx/releases/navigation) and use it as `nav_version`:

```kotlin
dependencies {
    // ...existing dependencies
    val nav_version = "<latest-stable-version>"
    implementation("androidx.navigation:navigation-compose:$nav_version")
}
```

That single dependency is all you need. Kotlin Serialization is only required for the type-safe routes API, which we are not using.

Sync Gradle. If it succeeds, you are ready.

## Step 2. Define the routes

Create `Routes.kt`. A route is just a **string**. Instead of scattering strings around the code (one typo would crash the app at runtime), keep them as constants in one place.

```kotlin
object Routes {
    const val HOME = "home"
    const val DETAIL = "detail/{itemId}"   // {itemId} is a placeholder for an argument
    const val SETTINGS = "settings"

    // Builds the real route for a given item
    fun detail(itemId: Int) = "detail/$itemId"
}
```

`detail/{itemId}` is a **pattern**: the NavHost uses it to recognise the screen and extract the argument. `Routes.detail(5)` produces `"detail/5"`, the concrete route you pass to `navigate`. Think of it like a web URL.

## Step 3. Create the screens (without any navigation code)

A golden rule: **screens must not know about the NavController.** Instead they receive lambdas (state hoisting, which you already know). This keeps them reusable, previewable and testable.

```kotlin
@Composable
fun HomeScreen(
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(20) { id ->
            ListItem(
                headlineContent = { Text("Item $id") },
                modifier = Modifier.clickable { onItemClick(id) }
            )
        }
    }
}

@Composable
fun DetailScreen(
    itemId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Details for item $itemId", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Settings")
    }
}
```

Notice that none of these import anything from `androidx.navigation`.

## Step 4. Create the NavController and the NavHost

Create `AppNavigation.kt`. This is where all the wiring lives.

```kotlin
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onItemClick = { id -> navController.navigate(Routes.detail(id)) }
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("itemId") { type = NavType.IntType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getInt("itemId") ?: 0
            DetailScreen(
                itemId = itemId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
    }
}
```

Let us read it line by line:

1. `rememberNavController()` creates the controller and keeps it across recompositions (and configuration changes).
2. `NavHost(...)` needs the controller and a **start destination** (a route string): the first screen shown.
3. `composable(Routes.HOME) { ... }` says: "when the current route is `home`, show this content".
4. `navController.navigate(Routes.detail(id))` pushes a new screen, for example `detail/5`, on the back stack.
5. `navArgument("itemId") { type = NavType.IntType }` declares the type of the `{itemId}` placeholder, and `backStackEntry.arguments?.getInt("itemId")` reads its value.
6. `navController.popBackStack()` removes the top screen, same as the system Back button.

## Step 4b. Call it from MainActivity

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyAppTheme {
                Scaffold { innerPadding ->
                    AppNavigation(Modifier.padding(innerPadding))
                }
            }
        }
    }
}
```

Run the app. Tap an item: the Detail screen appears. Press the system Back button or your own Back button: you return to Home. You have just used the back stack.

## Step 5. Understand the back stack

Trace what happens when you tap item 3 and then press Back:

```
Start:                [home]
navigate("detail/3")  [home, detail/3]   <- Detail is shown
popBackStack()        [home]             <- Home is shown again
```

Useful functions:

| Call | Effect |
| --- | --- |
| `navigate(route)` | Push a destination. |
| `popBackStack()` | Pop the top destination. Returns `false` if there was nothing to pop. |
| `navigateUp()` | Like Back, but is meant for the top-bar "Up" arrow. In simple apps they behave the same. |
| `navigate(route) { popUpTo(...) }` | Push a destination and first clear part of the stack. |

## Step 6. Controlling the back stack with navigate options

Sometimes you do not want the old screen to remain. Typical example: after a **login** screen, the user should not be able to go Back to it.

```kotlin
// Assumes the app has a login screen with the route Routes.LOGIN
navController.navigate(Routes.HOME) {
    popUpTo(Routes.LOGIN) { inclusive = true }   // remove "login" (and everything above it)
}
```

- `popUpTo(X)` removes screens until it reaches X.
- `inclusive = true` also removes X itself.
- `launchSingleTop = true` avoids stacking a second copy of a screen if it is already on top (for example, a user double-tapping a button).

## Step 7. Add a bottom navigation bar

A bottom bar switches between **top-level** destinations (Home and Settings). We need to know the current route to highlight the selected tab.

First, describe the tabs:

```kotlin
data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Routes.HOME, Icons.Default.Home),
    BottomNavItem("Settings", Routes.SETTINGS, Icons.Default.Settings)
)
```

Then move the `NavController` **up** so both the bar and the `NavHost` share it. Update `AppNavigation.kt`:

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.name) },
                        label = { Text(item.name) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(onItemClick = { id -> navController.navigate(Routes.detail(id)) })
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("itemId") { type = NavType.IntType })
            ) { entry ->
                val itemId = entry.arguments?.getInt("itemId") ?: 0
                DetailScreen(itemId, onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}
```

Required imports:

```kotlin
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
```

In `MainActivity`, remove the outer `Scaffold` and call `AppNavigation()` directly, since the Scaffold now lives inside it.

What the new pieces do:

- `currentBackStackEntryAsState()` turns the current destination into Compose **state**, so the bar recomposes whenever you navigate.
- `destination?.route` is the route string of the visible screen. Comparing it with each tab's route tells us which tab is selected. On the Detail screen no tab is selected, because its route (`detail/{itemId}`) is not a tab. Exercise 4 hides the bar there.
- `popUpTo(startDestination)` + `launchSingleTop` keep the stack from growing every time the user taps tabs.
- `saveState` / `restoreState` remember each tab's state (for example, scroll position) when you switch away and back.

## Step 8. Nested navigation graphs

So far every destination sits directly inside one `NavHost`. Real apps have groups of screens that belong together: a login flow (Login, Register, Forgot password), an onboarding, a checkout wizard. A **nested navigation graph** groups those screens under a single route. The group then behaves like one unit: you can enter it, leave it, and clear it from the back stack in one step.

In Compose you create one with the `navigation()` function inside the `NavHost` builder. It looks like `composable()`, but it holds several screens, and it needs a `startDestination` (the first screen of the group) and a `route` (the name of the group).

First, add the new routes to `Routes`:

```kotlin
object Routes {
    // ...existing routes
    const val AUTH = "auth"          // the nested graph itself
    const val LOGIN = "login"        // screens inside it
    const val REGISTER = "register"
}
```

Then declare the graph. To keep `AppNavigation` tidy, put it in an extension function. `LoginScreen` and `RegisterScreen` are simple screens like the ones from Step 3, taking lambdas for their events:

```kotlin
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation

fun NavGraphBuilder.authGraph(navController: NavController) {
    navigation(startDestination = Routes.LOGIN, route = Routes.AUTH) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onRegisterClick = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(onDone = { navController.popBackStack() })
        }
    }
}
```

Finally, call it from the host:

```kotlin
NavHost(navController, startDestination = Routes.AUTH) {
    authGraph(navController)
    composable(Routes.HOME) { /* ... */ }
    // ...other destinations
}
```

Points to understand:

1. Navigating to `Routes.AUTH` takes you to the graph's **start destination** (`login`). The graph is not a screen itself.
2. `popUpTo(Routes.AUTH) { inclusive = true }` removes the whole graph from the back stack. After logging in, pressing Back leaves the app instead of returning to Login.
3. Screens inside a graph navigate to each other by their own routes, exactly as before.
4. Graphs can be nested inside other graphs.

Bonus: screens in the same graph can share one ViewModel by scoping it to the graph's back stack entry:

```kotlin
composable(Routes.REGISTER) { backStackEntry ->
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry(Routes.AUTH)
    }
    val authViewModel: AuthViewModel = viewModel(parentEntry)
    // ...
}
```

This is handy for multi-step forms: every step reads and writes the same state, and the ViewModel is cleared when the graph leaves the back stack.

## Step 9. Use arguments in a ViewModel (optional, recommended)

In real apps the Detail screen would load data. A `ViewModel` can read the route arguments directly from `SavedStateHandle`:

```kotlin
class DetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val itemId: Int = checkNotNull(savedStateHandle["itemId"])
    // load the item using itemId...
}
```

Then in the destination:

```kotlin
composable(
    route = Routes.DETAIL,
    arguments = listOf(navArgument("itemId") { type = NavType.IntType })
) {
    val viewModel: DetailViewModel = viewModel()
    DetailScreen(viewModel.itemId, onBack = { navController.popBackStack() })
}
```

The `viewModel()` function comes from `androidx.lifecycle:lifecycle-viewmodel-compose`. The ViewModel is scoped to the back stack entry, so it is created when the screen is pushed and cleared when it is popped.

## Common mistakes

1. **Passing the NavController into every screen.** Pass lambdas instead. It makes previews and testing trivial.
2. **Passing big objects as arguments.** Send an **id** and load the data in the destination. Routes should be small.
3. **Typos in route strings.** `"detail/5"` and `"details/5"` are different routes, and the app crashes at runtime. Always use the constants in `Routes`.
4. **Forgetting to declare the argument.** If the route has `{itemId}`, the `composable` needs the matching `navArgument("itemId")` with the right type.
5. **Creating the NavController inside a screen** that recomposes. Create it once, high in the tree.
6. **Calling `navigate()` during composition.** Navigate only from event callbacks (clicks) or from `LaunchedEffect`.
7. **Stacking duplicate screens** when a button is tapped twice. Use `launchSingleTop = true`.

## Exercises

1. Add a top app bar to the Detail screen with an **Up arrow** that calls `navigateUp()`.
2. Add a Profile screen with the route `profile/{name}/{age}` and pass two arguments at once (a `String` and an `Int`).
3. Build the **login flow** from Step 8 and make sure Back from Home does **not** return to Login.
4. Hide the bottom bar when the current destination is the Detail screen.
5. Add a third tab and verify that each tab remembers its scroll position.

## Quick reference

```kotlin
// Declare
const val HOME = "home"
const val DETAIL = "detail/{itemId}"
fun detail(id: Int) = "detail/$id"

// Host
NavHost(navController, startDestination = HOME) {
    composable(HOME) { /* ... */ }
    composable(
        route = DETAIL,
        arguments = listOf(navArgument("itemId") { type = NavType.IntType })
    ) { entry -> val id = entry.arguments?.getInt("itemId") }
    navigation(startDestination = LOGIN, route = AUTH) { /* nested graph */ }
}

// Navigate
navController.navigate(detail(5))
navController.popBackStack()
navController.navigate(HOME) { popUpTo(AUTH) { inclusive = true } }
```

Further reading: the official guide "Navigation with Compose" at developer.android.com.
