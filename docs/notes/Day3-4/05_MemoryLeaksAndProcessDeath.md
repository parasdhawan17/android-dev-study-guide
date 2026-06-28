# Memory Leaks And Process Death

DAY 3-4: ANDROID COMPONENT LIFECYCLES
Topic 5: Memory Leaks, Context Handling, and Process Death

MEMORY LEAKS IN ANDROID

A memory leak occurs when an object that is no longer needed
is still referenced, preventing garbage collection.

In Android, the most common cause is holding references to
Activity/Fragment contexts longer than their lifecycle.

## Common Memory Leak Scenarios And Fixes

SCENARIO 1: Anonymous Inner Class holding Activity reference

SCENARIO 1: Anonymous Inner Class Memory Leak (Handler &amp; Runnable)

THE PROBLEM:
When you create an anonymous inner class (object : Runnable { }), it implicitly
holds a reference to the outer class (the Activity). Even after the Activity is
destroyed, the Runnable keeps it alive until the Runnable executes.

MEMORY REFERENCE CHAIN (Leaky Scenario):

Handler (Main Looper)
│
├─ Message Queue
│      │
│      └─ Message (delayed 60s)
│             │
│             └─ Runnable ──► Outer Activity (MemoryLeakScenario1)
│                                  │
│                                  ├─ Views
│                                  ├─ Resources
│                                  └─ (Entire Activity hierarchy)
│
[User leaves Activity, onDestroy() called]
│
▼
GC tries to collect Activity... BUT!
│
▼
Runnable still in MessageQueue ──► Runnable holds Activity ref
│                                  │
▼                                  ▼
Activity CANNOT be garbage collected!!!
(Memory Leak for 60+ seconds)

WHY ANONYMOUS INNER CLASSES CAUSE LEAKS:

In Java/Kotlin, non-static inner classes (including anonymous ones) implicitly
hold a reference to their enclosing class. This is compiler-generated:

// What you write:
object : Runnable { override fun run() { updateUI() } }

// What compiler generates (conceptually):
class AnonymousRunnable$1(val this$0: MemoryLeakScenario1) : Runnable {
override fun run() {
this$0.updateUI()  // Implicit reference!
}
}

STATIC vs NON-STATIC INNER CLASS:

┌─────────────────────────────────────────────────────────────┐
│  NON-STATIC (Anonymous/Inner)         STATIC (Nested)         │
│  ─────────────────────────          ───────────────         │
│  class Outer {                      class Outer {            │
│      val data = "hello"                 class Nested {       │
│      inner class Inner {  ❌            // No outer ref ✅  │
│          fun print() =                  fun print() =        │
│              println(data)              // Can't access      │
│      }                                  // outer.data        │
│  }                                  }                       │
│                                                               │
│  Implicit reference to Outer        No reference to Outer  │
│  → CAN cause memory leaks           → SAFE for memory       │
└─────────────────────────────────────────────────────────────┘

WEAKREFERENCE EXPLAINED:

A WeakReference holds a reference to an object WITHOUT preventing it
from being garbage collected. When GC runs, if an object is only
reachable through WeakReferences, it gets collected.

Strong Reference:    obj ──► Object (GC cannot collect)
Weak Reference:      obj ──► Object (GC CAN collect)
↓
weakRef.get() returns null after GC

Normal lifecycle:
1. Activity alive ──► weakRef.get() returns Activity
2. Activity destroyed ──► only weakRef points to it
3. GC runs ──► Activity collected, weakRef.get() returns null

````kotlin
class MemoryLeakScenario1 : AppCompatActivity() {

    // ❌ WRONG: Anonymous Runnable holds implicit reference to Activity
    private val leakyHandler = Handler(Looper.getMainLooper())

    fun wrongWay() {
        /**
         * THE LEAK EXPLAINED:
         * - Handler posts message to Main Looper's MessageQueue
         * - Message contains reference to the Runnable
         * - Runnable (anonymous inner class) holds implicit ref to Activity
         * - MessageQueue lives for app lifetime
         * - Even if Activity finishes, Message keeps Runnable alive
         * - Runnable keeps Activity alive
         * - LEAK until Runnable executes (60 seconds later)
         */
        leakyHandler.postDelayed(object : Runnable {
            override fun run() {
                // IMPLICIT: this@MemoryLeakScenario1.updateUI()
                // The 'this@' is hidden but REAL - it's the Activity reference!
                updateUI()
            }
        }, 60000) // 1 minute delay

        /**
         * LEAK MAGNITUDE:
         * - Not just the Activity leaks
         * - Entire View hierarchy leaks (all views, bitmaps, drawables)
         * - Resources and Context references leak
         * - Can be MEGABYTES of leaked memory!
         */
    }

    // ✅ CORRECT: Static nested class + WeakReference
    private val safeHandler = Handler(Looper.getMainLooper())

    fun rightWay() {
        /**
         * THE SAFE APPROACH:
         * 1. SafeRunnable is a static nested class (no outer reference)
         * 2. WeakReference allows Activity to be GC'd
         * 3. Check if Activity still exists before using it
         * 4. Remove callbacks in onDestroy() as extra safety
         */
        safeHandler.postDelayed(SafeRunnable(this), 60000)
    }

    /**
     * WHY 'private class' IS SAFE:
     * - Static nested class (no implicit outer reference)
     * - Must explicitly pass reference via constructor
     * - Using WeakReference instead of strong reference
     * - Activity can be garbage collected normally
     */
    private class SafeRunnable(activity: MemoryLeakScenario1) : Runnable {
        // WeakReference doesn't prevent garbage collection
        private val weakActivity = WeakReference(activity)

        override fun run() {
            // weakActivity.get() returns:
            // - The Activity (if still alive)
            // - null (if Activity was garbage collected)
            weakActivity.get()?.let { activity ->
                // CRITICAL: Check if Activity is finishing/destroyed
                // Even with WeakReference, check lifecycle state!
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.updateUI()
                } else {
                    // Activity is dying, skip the update
                    println("Activity destroyed, skipping UI update")
                }
            } ?: run {
                // Activity was garbage collected
                println("Activity was GC'd, nothing to update")
            }
        }
    }

    /**
     * ALTERNATIVE SOLUTIONS (Choose based on your use case):
     * ======================================================
     */

    // Solution 2: Use Lifecycle-aware components (RECOMMENDED for modern apps)
    fun lifecycleAwareWay() {
        lifecycleScope.launch {
            delay(60000) // 60 seconds
            // Automatically cancelled if Activity destroyed!
            if (isActive) {
                updateUI()
            }
        }
    }

    // Solution 3: ViewModel + coroutines (For business logic that survives rotation)
    fun viewModelWay(viewModel: MyViewModel) {
        viewModel.scheduleUpdate {
            // Callback only executes if Activity still observes
            updateUI()
        }
    }

    // Solution 4: RxJava with lifecycle binding
    fun rxJavaWay(disposables: CompositeDisposable) {
        Observable.timer(60, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { updateUI() }
            .addTo(disposables) // Clear in onDestroy
    }

    private fun updateUI() {
        // Update UI with delayed result
        println("UI Updated!")
    }

    override fun onDestroy() {
        super.onDestroy()
        /**
         * DEFENSE IN DEPTH:
         * Even with safe code, ALWAYS clean up in onDestroy()
         * - Catches any missed callbacks
         * - Prevents unnecessary work
         * - Good defensive programming
         */

        // ✅ Cancel pending callbacks (null removes ALL)
        leakyHandler.removeCallbacksAndMessages(null)
        safeHandler.removeCallbacksAndMessages(null)

        // If using RxJava
        // disposables.clear()

        // If using coroutines with custom scope
        // customScope.cancel()
    }
}
````

TIMELINE OF A MEMORY LEAK:

0s: User opens Activity
└─ Handler.postDelayed(Runnable, 60000ms)
└─ Runnable enters MessageQueue

5s: User presses back (Activity finishing)
└─ onPause() → onStop() → onDestroy()
└─ Activity marked for destruction
└─ BUT: Runnable still references Activity!

5s-60s: GC tries to collect Activity
└─ Runnable in MessageQueue → Activity reachable
└─ GC: "Cannot collect, still referenced!"
└─ LEAK ACTIVE: Activity + Views + Resources held in memory

60s: Runnable executes
└─ Runs updateUI() on dead Activity
└─ CRASH? Or silently fails?
└─ MessageQueue releases Runnable

60s+: GC collects Activity
└─ Finally! But memory was wasted for 55 seconds

IF USER OPENS/CLOSES ACTIVITY MULTIPLE TIMES:
Each instance leaks until its Runnable executes
Memory usage keeps growing
Eventually: OutOfMemoryError!

SCENARIO 2: Listener/Callback not unregistered

````kotlin
class MemoryLeakScenario2 : AppCompatActivity() {

    private lateinit var locationManager: LocationManager

    // ❌ WRONG: Not unregistering listener
    fun wrongWay() {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,
            10f,
            locationListener  // Holds reference to Activity
        )
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Implicit reference to outer Activity
        }
    }

    // ✅ CORRECT: Unregister in onStop() or onDestroy()
    override fun onStart() {
        super.onStart()
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,
            10f,
            safeLocationListener,
            Looper.getMainLooper()
        )
    }

    override fun onStop() {
        super.onStop()
        // CRITICAL: Remove listener to prevent leak
        locationManager.removeUpdates(safeLocationListener)
    }

    private val safeLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Handle location update
        }
    }
}
````

SCENARIO 3: Singleton holding Activity Context

````kotlin
object SingletonManager {

    // ❌ WRONG: Singleton holds Activity context
    private var activityContext: Context? = null

    fun wrongInit(context: Context) {
        // Never hold Activity reference in singleton!
        activityContext = context  // This is an Activity reference!
    }

    // ✅ CORRECT: Hold Application context only
    private var appContext: Context? = null

    fun rightInit(context: Context) {
        // Use application context - lives as long as app
        appContext = context.applicationContext
    }
}
````

SCENARIO 4: AsyncTask holding Activity reference
(AsyncTask is deprecated but this pattern applies to any async work)

````kotlin
class MemoryLeakScenario4 : AppCompatActivity() {

    // ❌ WRONG: Non-static AsyncTask holds Activity reference
    inner class LeakyAsyncTask : AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg params: Void?): String {
            Thread.sleep(10000) // 10 seconds
            return "Done"
        }

        override fun onPostExecute(result: String?) {
            // Implicit reference to outer Activity
            textView.text = result  // textView is from Activity
        }
    }

    // ✅ CORRECT: Static AsyncTask with WeakReference
    private class SafeAsyncTask(activity: MemoryLeakScenario4) : AsyncTask<Void, Void, String>() {
        private val weakActivity = WeakReference(activity)

        override fun doInBackground(vararg params: Void?): String {
            Thread.sleep(10000)
            return "Done"
        }

        override fun onPostExecute(result: String?) {
            weakActivity.get()?.let { activity ->
                if (!activity.isDestroyed) {
                    activity.textView.text = result
                }
            }
        }
    }

    lateinit var textView: TextView
}
````

SCENARIO 5: Fragment holding View reference after onDestroyView

````kotlin
class MemoryLeakScenario5 : Fragment() {

    // ❌ WRONG: Holding view reference after onDestroyView
    private var wrongBinding: FragmentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        wrongBinding = FragmentBinding.inflate(inflater, container, false)
        return wrongBinding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Forgot to clear binding! Memory leak!
    }

    // ✅ CORRECT: Proper ViewBinding pattern
    private var _binding: FragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateViewCorrect(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyViewCorrect() {
        super.onDestroyView()
        // CRITICAL: Clear the binding reference
        _binding = null
    }
}
````

Mock binding class

````kotlin
class FragmentBinding {
    val root: View = View(null)
    companion object {
        fun inflate(inflater: LayoutInflater, container: ViewGroup?, attach: Boolean): FragmentBinding {
            return FragmentBinding()
        }
    }
}
````

SCENARIO 6: Coroutine not scoped properly

````kotlin
class MemoryLeakScenario6 : AppCompatActivity() {

    // ❌ WRONG: GlobalScope lives forever
    fun wrongCoroutine() {
        GlobalScope.launch {
            delay(30000) // 30 seconds
            // If Activity destroyed, this coroutine keeps running
            // and may try to access destroyed Activity
            updateUI()
        }
    }

    // ❌ WRONG: Using lifecycleScope but not checking state
    fun wrongCoroutine2() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                fetchData()
            }
            // May crash if Activity destroyed during fetch
            updateUI(result)
        }
    }

    // ✅ CORRECT: Using lifecycleScope with proper state checking
    fun rightCoroutine() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                fetchData()
            }
            // Check if still active before updating UI
            if (isActive && !isFinishing && !isDestroyed) {
                updateUI(result)
            }
        }
    }

    // ✅ CORRECT: Using repeatOnLifecycle for Flow collection
    fun rightFlowCollection() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collect { data ->
                    // Automatically cancels when lifecycle stops
                    updateUI(data)
                }
            }
        }
    }

    private suspend fun fetchData(): String = ""
    private fun updateUI(data: String = "") {}
    private lateinit var viewModel: MyViewModel
}
````

## Context Usage Guide

WHEN TO USE WHICH CONTEXT?

Activity Context (@Activity):
- Starting Activity (startActivity)
- Creating dialogs (AlertDialog)
- Inflating layouts that need themes
- Accessing Activity-specific resources

Application Context (getApplicationContext()):
- Singleton initialization
- Long-lived objects (repositories, databases)
- Toast messages
- System services that outlive Activity
- SharedPreferences (usually)

Base Context:
- Wrapping with ContextWrapper for theming

````kotlin
class ContextUsageGuide {

    fun demonstrateContextUsage(activity: AppCompatActivity) {
        val activityContext: Context = activity
        val appContext: Context = activity.applicationContext

        // ✅ Activity Context - for UI-related operations
        val intent = Intent(activityContext, TargetActivity::class.java)
        activityContext.startActivity(intent)

        val dialog = AlertDialog.Builder(activityContext)
            .setTitle("Title")
            .create()

        // ✅ Application Context - for long-lived objects
        val database = Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "database"
        ).build()

        // Show Toast (can use either, app context preferred)
        Toast.makeText(appContext, "Message", Toast.LENGTH_SHORT).show()

        // System Service (usually app context)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
}
````

## Process Deep Restoration

SAVED STATE vs VIEWMODEL vs DATABASE

SavedStateHandle (Bundle):
- Survives: Configuration changes, process death
- Use for: UI state (scroll position, form input, current tab)
- Limitations: ~1MB limit, must be Parcelable/Serializable
- Speed: Fast (in-memory after restore)

ViewModel:
- Survives: Configuration changes
- Does NOT survive: Process death
- Use for: In-memory data, temporary calculations

Database/SharedPreferences:
- Survives: Everything (persistent storage)
- Use for: User data, settings, cached content
- Speed: Slower (disk I/O)

````kotlin
class ProcessDeathExample : AppCompatActivity() {

    private lateinit var viewModel: ProcessAwareViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if this is a fresh start or restoration
        if (savedInstanceState == null) {
            println("🆕 Fresh start - no previous state")
        } else {
            println("🔄 Restoration - process died or config change")
        }

        viewModel = ViewModelProvider(this)[ProcessAwareViewModel::class.java]

        // Observe state - works for both config changes AND process death
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: ProcessUiState) {
        // Restore scroll position from saved state
        // Restore form input
        // Restore selected tab
    }
}

class ProcessAwareViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_FORM_DATA = "form_data"
        private const val KEY_SELECTED_TAB = "selected_tab"
        private const val KEY_SCROLL_POSITION = "scroll_position"
    }

    private val _uiState = MutableStateFlow(
        ProcessUiState(
            formData = savedStateHandle[KEY_FORM_DATA] ?: "",
            selectedTab = savedStateHandle[KEY_SELECTED_TAB] ?: 0,
            scrollPosition = savedStateHandle[KEY_SCROLL_POSITION] ?: 0
        )
    )
    val uiState: StateFlow<ProcessUiState> = _uiState.asStateFlow()

    fun updateFormData(data: String) {
        _uiState.update { it.copy(formData = data) }
        // Persist immediately - survives process death
        savedStateHandle[KEY_FORM_DATA] = data
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
        savedStateHandle[KEY_SELECTED_TAB] = tabIndex
    }

    fun updateScrollPosition(position: Int) {
        _uiState.update { it.copy(scrollPosition = position) }
        savedStateHandle[KEY_SCROLL_POSITION] = position
    }
}

data class ProcessUiState(
    val formData: String = "",
    val selectedTab: Int = 0,
    val scrollPosition: Int = 0
)
````

## Detecting Memory Leaks

Tools for detecting memory leaks:

1. LeakCanary (Library)
- Automatically detects and reports leaks
- Shows leak trace
- Easy integration

2. Android Studio Profiler
- Heap dump analysis
- Memory allocation tracking
- Find retained objects

3. StrictMode (Built-in)
- Detects accidental disk/network operations on main thread
- Can detect some lifecycle violations

````kotlin
class MemoryLeakDetection {

    fun setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )
        }
    }
}
````

Mock classes for compilation

````kotlin
class LocationManager {
    fun requestLocationUpdates(
        provider: String,
        minTime: Long,
        minDistance: Float,
        listener: LocationListener,
        looper: Looper? = null
    ) {}
    fun removeUpdates(listener: LocationListener) {}
}
interface LocationListener {
    fun onLocationChanged(location: Location)
}
class Location
class Looper {
    companion object {
        fun getMainLooper(): Looper = Looper()
    }
}
class Handler(looper: Looper) {
    fun postDelayed(r: Runnable, delay: Long) {}
    fun removeCallbacksAndMessages(token: Any?) {}
}
open class AsyncTask<Params, Progress, Result> {
    open fun doInBackground(vararg params: Params?): Result { throw NotImplementedError() }
    open fun onPostExecute(result: Result?) {}
    fun execute(vararg params: Params?): AsyncTask<Params, Progress, Result> { return this }
}
class BuildConfig {
    companion object {
        const val DEBUG = true
    }
}
class StrictMode {
    class VmPolicy
    class VmPolicyBuilder {
        fun detectLeakedClosableObjects(): VmPolicyBuilder = this
        fun detectLeakedRegistrationObjects(): VmPolicyBuilder = this
        fun detectLeakedSqlLiteObjects(): VmPolicyBuilder = this
        fun penaltyLog(): VmPolicyBuilder = this
        fun penaltyDeath(): VmPolicyBuilder = this
        fun build(): VmPolicy = VmPolicy()
    }
    companion object {
        fun setVmPolicy(policy: VmPolicy) {}
    }
}
class Room {
    companion object {
        fun databaseBuilder(context: Context, klass: Class<*>, name: String): Any {
            return Any()
        }
    }
}
class AppDatabase
class AppCompatActivity : ComponentActivity()
open class ComponentActivity {
    fun getSystemService(name: String): Any = Any()
}
class Intent(context: Context, cls: Class<*>) {
    fun putExtras(bundle: Bundle?): Intent { return this }
}
class Bundle
class AlertDialog {
    class Builder(context: Context) {
        fun setTitle(title: String): Builder = this
        fun create(): Any = Any()
    }
}
class Toast {
    companion object {
        fun makeText(context: Context, text: String, duration: Int): Toast = Toast()
        const val LENGTH_SHORT = 0
    }
}
class View(context: Context?)
class TextView
class LayoutInflater {
    fun inflate(resource: Int, root: ViewGroup?, attachToRoot: Boolean): View {
        return View(null)
    }
}
open class ViewGroup(context: Context?) : View(context)
class Inflater : LayoutInflater()
class Fragment
class TargetActivity : AppCompatActivity()
class Context
class AlarmManager
class MyViewModel : ViewModel() {
    val data: kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.emptyFlow()
}
abstract class ViewModel {
    protected val viewModelScope: kotlinx.coroutines.CoroutineScope
        get() = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
    open fun onCleared() {}
}
class GlobalScope {
    companion object {
        fun launch(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit): kotlinx.coroutines.Job {
            throw NotImplementedError()
        }
    }
}
class WeakReference<T>(referent: T) {
    fun get(): T? = null
}
class TargetActivity
````
