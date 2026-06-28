# Compose Lifecycle

DAY 5-7: Jetpack Compose Lifecycle

Compose uses a state-driven lifecycle tied to composition, not Android component lifecycle.
Key concepts: DisposableEffect, LaunchedEffect, rememberSaveable, SideEffect

````kotlin
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
````

## 1. Disposable Effect (With Cleanup)

Used for side effects that require cleanup.
Runs when composition starts and when keys change.
onDispose runs when leaving composition or before re-running effect.

````kotlin
@Composable
fun NetworkStatusListener(onStatusChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    
    DisposableEffect(context) { // Key = context - re-runs if context changes
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onStatusChanged(true)
            }
            override fun onLost(network: Network) {
                onStatusChanged(false)
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        // CLEANUP - runs when leaving composition
        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}
````

## 2. Launched Effect (Coroutine-Based)

Used for side effects in a coroutine scope.
Automatically cancelled when leaving composition.
Key change = restart effect.

````kotlin
@Composable
fun AutoRefreshTimer(intervalMs: Long, onRefresh: () -> Unit) {
    // Unit = run once, never restart
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(intervalMs)
            onRefresh()
        }
    }
}
````

Runs when userId changes

````kotlin
@Composable
fun UserProfile(userId: String, viewModel: UserViewModel) {
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }
    
    // UI implementation
}
````

Multiple keys - runs when any changes

````kotlin
@Composable
fun Dashboard(userId: String, refreshKey: Int) {
    LaunchedEffect(userId, refreshKey) {
        // Runs when userId OR refreshKey changes
    }
}
````

## 3. Remember Saveable (Survives Process Death)

Similar to remember but survives configuration changes AND process death.
Uses Bundle mechanism - only savable types or custom Saver.

````kotlin
@Composable
fun SearchScreen() {
    // Survives process death
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var scrollPosition by rememberSaveable { mutableStateOf(0) }
}
````

Custom Saver for complex objects

````kotlin
data class User(val id: String, val name: String, val email: String)

val UserSaver = Saver<User, List<Any>>(
    save = { listOf(it.id, it.name, it.email) },
    restore = { restored ->
        User(
            id = restored[0] as String,
            name = restored[1] as String,
            email = restored[2] as String
        )
    }
)

@Composable
fun UserEditor() {
    var user by rememberSaveable(stateSaver = UserSaver) {
        mutableStateOf(User("1", "", ""))
    }
}
````

## 4. Side Effect (No Cleanup Needed)

Called on every successful composition (not during initial composition).
Used for publishing Compose state to non-Compose code.

````kotlin
@Composable
fun AnalyticsScreen(screenName: String) {
    SideEffect {
        // Called after every recomposition
        Analytics.trackScreenView(screenName)
    }
}
````

## 5. Derived State Of (Computed State)

Memoizes expensive calculations. Only recalculates when inputs change.

````kotlin
@Composable
fun ItemList(items: List<Item>, filter: String) {
    // Only recomputes when items or filter changes
    val filteredItems by remember(items, filter) {
        derivedStateOf {
            items.filter { it.name.contains(filter, ignoreCase = true) }
        }
    }
}
````

## 6. Effect Handler Comparison

┌─────────────────────┬──────────────────┬──────────────────┬─────────────────┐
│ Effect              │ Runs When        │ Cleanup          │ Use Case        │
├─────────────────────┼──────────────────┼──────────────────┼─────────────────┤
│ LaunchedEffect      │ Key change       │ Auto-cancels     │ API calls,      │
│                     │                  │ coroutine scope  │ one-shot ops    │
├─────────────────────┼──────────────────┼──────────────────┼─────────────────┤
│ DisposableEffect    │ Key change       │ onDispose block  │ Listeners,      │
│                     │                  │                  │ callbacks       │
├─────────────────────┼──────────────────┼──────────────────┼─────────────────┤
│ SideEffect          │ Every recomp     │ None             │ Analytics,      │
│                     │                  │                  │ external sync   │
├─────────────────────┼──────────────────┼──────────────────┼─────────────────┤
│ remember            │ Once (per comp)  │ None             │ Cache values    │
├─────────────────────┼──────────────────┼──────────────────┼─────────────────┤
│ rememberSaveable    │ Once (per comp)  │ None             │ Survives        │
│                     │                  │                  │ process death   │
└─────────────────────┴──────────────────┴──────────────────┴─────────────────┘

## 7. Viewmodel In Compose

ViewModel survives configuration changes automatically.
Scope follows ViewModelStoreOwner (Activity, Fragment, nav graph).

Basic usage

````kotlin
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel() // androidx.lifecycle.viewmodel.compose
) {
    val uiState by viewModel.uiState.collectAsState()
    // UI
}
````

With Hilt

````kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel() // Scoped to nav destination
) {
    val settings by viewModel.settings.collectAsState()
}
````

Different scopes

````kotlin
@Composable
fun NestedNavigation() {
    // Activity-scoped
    val activityViewModel: ActivityViewModel = viewModel()
    
    // Nav graph scoped (backStackEntry)
    val navController = rememberNavController()
    val parentEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry("parent_route")
    }
    val parentViewModel: ParentViewModel = viewModel(parentEntry)
}
````

## 8. Compose Lifecycle Flow

COMPOSITION LIFECYCLE:

1. COMPOSITION (initial)
├─ remember/rememberSaveable initialized
├─ DisposableEffect enters (no onDispose yet)
├─ LaunchedEffect launches coroutine
└─ SideEffect NOT called (only on recompositions)

2. RECOMPOSITION (state change)
├─ remember values retained
├─ DisposableEffect: if keys changed → onDispose + re-enter
├─ LaunchedEffect: if keys changed → cancel + relaunch
└─ SideEffect called

3. DISPOSAL (leaving composition)
├─ remember values cleared
├─ DisposableEffect: onDispose called
└─ LaunchedEffect: coroutine cancelled

CONFIGURATION CHANGE (Activity recreated):
├─ remember cleared
├─ rememberSaveable restored from Bundle
├─ ViewModel retained (survives)
└─ DisposableEffect/LaunchedEffect: restart if keys changed

## 9. Practical Android Patterns

Pattern: Lifecycle-aware data loading

````kotlin
@Composable
fun DataLoader(userId: String) {
    var data by remember { mutableStateOf<Data?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(userId) {
        isLoading = true
        error = null
        try {
            data = repository.fetchData(userId)
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }
    
    // UI uses data, isLoading, error
}
````

Pattern: Permission handling with cleanup

````kotlin
@Composable
fun CameraPreview() {
    val context = LocalContext.current
    
    DisposableEffect(Unit) {
        val cameraController = LifecycleCameraController(context)
        // Setup camera...
        
        onDispose {
            cameraController.unbind()
        }
    }
}
````

Pattern: Polling with proper cleanup

````kotlin
@Composable
fun PollingIndicator(pollInterval: Long, onPoll: () -> Unit) {
    var isActive by remember { mutableStateOf(true) }
    
    LaunchedEffect(isActive) {
        if (!isActive) return@LaunchedEffect
        
        while (isActive && isActive) { // isActive from coroutine scope
            onPoll()
            delay(pollInterval)
        }
    }
}
````

Pattern: Back handler

````kotlin
@Composable
fun BackAwareScreen(onBack: () -> Boolean) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    
    DisposableEffect(backDispatcher) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!onBack()) {
                    isEnabled = false
                    backDispatcher?.onBackPressed()
                }
            }
        }
        backDispatcher?.addCallback(callback)
        
        onDispose {
            callback.remove()
        }
    }
}
````

## Interview Questions

Q: When would you use DisposableEffect vs LaunchedEffect?
A: DisposableEffect for resources needing cleanup (listeners, callbacks, subscriptions).
LaunchedEffect for coroutine-based operations that auto-cancel (API calls, timers).

Q: How does rememberSaveable differ from ViewModel for state management?
A: rememberSaveable survives process death but is scoped to composition.
ViewModel survives config changes and process death (with SavedStateHandle).
ViewModel is better for business logic; rememberSaveable for UI state.

Q: What happens to LaunchedEffect during configuration change?
A: Coroutine is cancelled when leaving composition, relaunched when recomposed.
Use rememberUpdatedState if you need access to latest values without restart.

Q: Why is SideEffect not called during initial composition?
A: It's designed to publish Compose state to non-Compose code after successful composition.
During initial composition, there may be nothing meaningful to publish yet.

Stub classes for compilation

````kotlin
data class Item(val name: String)
class UserViewModel { fun loadUser(id: String) {} }
class ProfileViewModel { val uiState: kotlinx.coroutines.flow.StateFlow<String> by lazy { kotlinx.coroutines.flow.MutableStateFlow("") } }
class SettingsViewModel { val settings: kotlinx.coroutines.flow.StateFlow<String> by lazy { kotlinx.coroutines.flow.MutableStateFlow("") } }
class ActivityViewModel
class ParentViewModel
class Data
class LifecycleCameraController(context: Context) { fun unbind() {} }
object Analytics { fun trackScreenView(name: String) {} }
abstract class OnBackPressedCallback(enabled: Boolean) {
    var isEnabled: Boolean = enabled
    abstract fun handleOnBackPressed()
    fun remove() {}
}
class OnBackPressedDispatcher {
    fun addCallback(callback: OnBackPressedCallback) {}
    fun onBackPressed() {}
}
interface OnBackPressedDispatcherOwner { val onBackPressedDispatcher: OnBackPressedDispatcher }
object LocalOnBackPressedDispatcherOwner { val current: OnBackPressedDispatcherOwner? = null }
object repository { suspend fun fetchData(id: String): Data { return Data() } }
class Saver<T, S> {
    companion object {
        fun <T, S> create(save: (T) -> S, restore: (S) -> T): Saver<T, S> = Saver()
    }
}
fun <T : Any> rememberNavController(): T { TODO() }
fun <T> MutableStateFlow(value: T): kotlinx.coroutines.flow.MutableStateFlow<T> { TODO() }
````
