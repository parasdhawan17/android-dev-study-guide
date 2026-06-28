# Day 5-7: Jetpack Compose, Process Death & Background Execution - Quick Reference

## Compose Lifecycle Effects

### Effect Handlers

| Effect | Runs When | Cleanup | Use Case |
|--------|-----------|---------|----------|
| `LaunchedEffect` | Key changes | Auto-cancel coroutine | API calls, one-shot operations |
| `DisposableEffect` | Key changes | `onDispose` block | Listeners, callbacks, resources |
| `SideEffect` | Every recomposition | None | Analytics, external sync |
| `rememberSaveable` | Once (restored) | None | Survives process death |

### DisposableEffect Pattern
```kotlin
@Composable
fun NetworkListener(onStatus: (Boolean) -> Unit) {
    val context = LocalContext.current
    
    DisposableEffect(context) {
        val callback = NetworkCallback { onStatus(it) }
        connectivityManager.register(callback)
        
        onDispose {
            connectivityManager.unregister(callback) // Cleanup!
        }
    }
}
```

### LaunchedEffect Pattern
```kotlin
// Run once
LaunchedEffect(Unit) { loadData() }

// Run when key changes
LaunchedEffect(userId) { loadUser(userId) }

// Multiple keys
LaunchedEffect(userId, refreshKey) { load() }
```

### rememberSaveable
```kotlin
// Simple values
var text by rememberSaveable { mutableStateOf("") }

// Complex objects with custom Saver
var user by rememberSaveable(stateSaver = UserSaver) {
    mutableStateOf(User.default())
}

// Custom Saver
data class User(val id: String, val name: String)
val UserSaver = Saver<User, List<Any>>(
    save = { listOf(it.id, it.name) },
    restore = { User(it[0] as String, it[1] as String) }
)
```

---

## ViewModel in Compose

```kotlin
// Activity/Fragment scope
val viewModel: MyViewModel = viewModel()

// Hilt
val viewModel: MyViewModel = hiltViewModel()

// Nav graph scope
val parentEntry = remember(backStackEntry) {
    navController.getBackStackEntry("route")
}
val viewModel: MyViewModel = viewModel(parentEntry)
```

---

## Compose vs View System

| Aspect | View System | Jetpack Compose |
|--------|-------------|-----------------|
| UI Definition | XML + findViewById | Kotlin functions |
| State Updates | Manual (setText, etc.) | Automatic recomposition |
| Custom Components | Extend View | Composable function |
| Preview | XML editor | @Preview annotation |
| Performance | View inflation | Compiler optimizations |
| Threading | UI thread required | Composition any thread |

### Interoperability
```kotlin
// Compose inside View
findViewById<ComposeView>(R.id.compose_view).setContent {
    MyComposable()
}

// View inside Compose
AndroidView(
    factory = { context -> CustomView(context) },
    update = { view -> view.update() }
)
```

---

## Process Death

### What Happens
```
1. App backgrounded
2. System kills process (low memory)
3. NO lifecycle callbacks called!
4. User returns via Recents
5. Activity recreated with savedInstanceState
```

### What Saves vs Refetch

| Save to Bundle | Re-fetch from Source |
|---------------|---------------------|
| User input (form, search) | Large lists |
| Scroll position | Network responses |
| Selected tab/filter | Images/Bitmaps |
| Item IDs | Complex objects |

### SavedStateHandle
```kotlin
class MyViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Property delegation
    var searchQuery: String
        get() = savedStateHandle.get<String>("query") ?: ""
        set(value) { savedStateHandle["query"] = value }
    
    // StateFlow
    val queryFlow: StateFlow<String> = 
        savedStateHandle.getStateFlow("query", "")
}

// With Hilt
@HiltViewModel
class MyViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: Repository
) : ViewModel()
```

---

## Background Execution

### Decision Tree
```
Need to run NOW?
├── YES → Is user actively aware?
│         ├── YES → Foreground Service (notification required)
│         └── NO  → Can wait minutes?
│                 ├── YES → WorkManager (expedited, API 31+)
│                 └── NO  → AlarmManager (exact time)
└── NO  → Can be deferred hours/days?
          ├── YES → WorkManager (periodic/one-time)
          └── NO  → Not possible on Oreo+
```

### WorkManager
```kotlin
// Simple work
val work = OneTimeWorkRequestBuilder<SyncWorker>()
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(true)
            .build()
    )
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        WorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS
    )
    .build()

WorkManager.getInstance(context).enqueue(work)

// Unique work (prevent duplicates)
WorkManager.getInstance(context).enqueueUniqueWork(
    "daily_sync",
    ExistingWorkPolicy.KEEP,
    syncWork
)

// Periodic work (min interval: 15 minutes)
val periodicWork = PeriodicWorkRequestBuilder<CleanupWorker>(
    15, TimeUnit.MINUTES
).build()

// Chaining
WorkManager.getInstance(context)
    .beginWith(compressWork)
    .then(uploadWork)
    .then(notifyWork)
    .enqueue()
```

### Foreground Service
```kotlin
class MusicService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        
        // API 31+ requires foregroundServiceType in manifest
        startForeground(NOTIFICATION_ID, notification)
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
```

---

## Quick Interview Checklist

| Topic | Key Points |
|-------|------------|
| **LaunchedEffect** | Key-based coroutine launch, auto-cancels |
| **DisposableEffect** | Has onDispose cleanup block |
| **rememberSaveable** | Survives config changes + process death |
| **Process Death** | Silent kill, no callbacks, use onSaveInstanceState |
| **SavedStateHandle** | Auto-saves to Bundle, survives process death |
| **WorkManager** | Deferrable, guaranteed, constraint-based |
| **Foreground Service** | Immediate, user-aware, notification required |
| **Compose vs View** | Declarative vs imperative, recomposition vs manual |

---

## Common Interview Questions

### Compose Lifecycle
**Q: LaunchedEffect vs DisposableEffect?**
A: LaunchedEffect for coroutine-based work (auto-cancels). DisposableEffect for resources needing cleanup (has onDispose).

**Q: What survives process death in Compose?**
A: rememberSaveable (not remember). ViewModel with SavedStateHandle.

### Process Death
**Q: What lifecycle callbacks during process death?**
A: None! Process killed silently. onSaveInstanceState called earlier when backgrounding.

**Q: Difference between ViewModel and onSaveInstanceState?**
A: ViewModel survives config changes but NOT process death (unless using SavedStateHandle). onSaveInstanceState/SavedStateHandle survives both.

### Background Execution
**Q: WorkManager vs Foreground Service?**
A: WorkManager for deferrable work (sync, upload). Foreground Service for immediate user-aware work (playback, navigation).

**Q: Minimum periodic work interval?**
A: 15 minutes. System-enforced to prevent battery drain.

---

## Testing Process Death

```bash
# Enable "Don't keep activities" in Developer Options
# OR use adb:

# 1. Start app, enter data
# 2. Background app
adb shell am kill com.your.package
# 3. Return via Recents
# 4. Verify state restored
```
