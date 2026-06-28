/**
 * ============================================================================
 * DAY 3-4: ANDROID COMPONENT LIFECYCLES
 * ============================================================================
 * Topic 3: LifecycleOwner, LifecycleObserver, and Lifecycle-Aware Components
 * ============================================================================
 */

/**
 * LIFECYCLE ARCHITECTURE COMPONENTS
 * =================================
 *
 * Core Interfaces:
 * - LifecycleOwner: Has a Lifecycle (Activity, Fragment)
 * - LifecycleObserver: Observes Lifecycle events
 * - Lifecycle: State machine for component lifecycle
 *
 * Benefits:
 * - Self-contained components manage their own lifecycle
 * - No manual lifecycle callbacks in Activity/Fragment
 * - Memory leak prevention through automatic cleanup
 */

// ============================================================================
// BASIC LIFECYCLE OBSERVER
// ============================================================================

/**
 * Simple LifecycleObserver that logs all lifecycle events
 */
class LoggingLifecycleObserver : LifecycleEventObserver {

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> println("📱 ON_CREATE")
            Lifecycle.Event.ON_START -> println("📱 ON_START")
            Lifecycle.Event.ON_RESUME -> println("📱 ON_RESUME")
            Lifecycle.Event.ON_PAUSE -> println("📱 ON_PAUSE")
            Lifecycle.Event.ON_STOP -> println("📱 ON_STOP")
            Lifecycle.Event.ON_DESTROY -> println("📱 ON_DESTROY")
            Lifecycle.Event.ON_ANY -> println("📱 ON_ANY")
        }
    }
}

/**
 * Using @OnLifecycleEvent annotation (deprecated, use LifecycleEventObserver)
 */
class AnnotatedLifecycleObserver : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        println("Started!")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        println("Stopped!")
    }
}

// ============================================================================
// PRACTICAL LIFECYCLE-AWARE COMPONENTS
// ============================================================================

/**
 * Lifecycle-Aware Location Manager
 * Automatically starts/stops location updates based on lifecycle
 */
class LifecycleAwareLocationManager(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : DefaultLifecycleObserver {

    private var locationCallback: LocationCallback? = null

    /**
     * Call this to start receiving location updates
     * Actual updates only start when lifecycle reaches STARTED
     */
    fun startLocationTracking(callback: LocationCallback) {
        this.locationCallback = callback
        // Updates will start in onStart()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // Start location updates when Activity/Fragment is visible
        locationCallback?.let { callback ->
            try {
                fusedLocationClient.requestLocationUpdates(
                    createLocationRequest(),
                    callback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                // Handle permission not granted
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // Stop location updates when not visible (saves battery!)
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        // Clean up
        locationCallback = null
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            10000L
        ).build()
    }
}

/**
 * Lifecycle-Aware Timer
 * Automatically pauses/resumes based on lifecycle
 */
class LifecycleAwareTimer(
    private val onTick: (Long) -> Unit
) : DefaultLifecycleObserver {

    private var countDownTimer: CountDownTimer? = null
    private var remainingTime: Long = 60000 // 1 minute
    private var isRunning = false

    fun start(millisInFuture: Long = 60000) {
        remainingTime = millisInFuture
        createTimer()
    }

    private fun createTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                onTick(millisUntilFinished / 1000)
            }

            override fun onFinish() {
                isRunning = false
            }
        }.start()
        isRunning = true
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        // Pause timer when not active
        countDownTimer?.cancel()
        isRunning = false
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        // Resume timer when active again
        if (!isRunning && remainingTime > 0) {
            createTimer()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        // Clean up
        countDownTimer?.cancel()
        countDownTimer = null
    }
}

/**
 * Lifecycle-Aware Media Player
 */
class LifecycleAwareMediaPlayer(
    private val context: Context
) : DefaultLifecycleObserver {

    private var mediaPlayer: MediaPlayer? = null
    private var currentPosition = 0

    fun prepare(url: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            prepareAsync()
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                player.seekTo(currentPosition)
                player.start()
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                currentPosition = player.currentPosition
                player.pause()
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // Optionally stop completely
        // mediaPlayer?.stop()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

// ============================================================================
// CUSTOM LIFECYCLE OWNER
// ============================================================================

/**
 * Creating a custom LifecycleOwner for non-Activity/Fragment components
 */
class CustomLifecycleService : Service(), LifecycleOwner {

    private val dispatcher = ServiceLifecycleDispatcher(this)

    override fun getLifecycle(): Lifecycle = dispatcher.lifecycle

    override fun onCreate() {
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()

        // Register lifecycle-aware components
        lifecycle.addObserver(MyServiceObserver())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dispatcher.onServicePreSuperOnStart()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        dispatcher.onServicePreSuperOnBind()
        return null
    }

    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }
}

class MyServiceObserver : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        println("Service created")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        println("Service destroyed")
    }
}

// ============================================================================
// LIFECYCLE-AWARE COROUTINES
// ============================================================================

/**
 * Using lifecycleScope for automatic coroutine management
 */
class CoroutineLifecycleExample : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * lifecycleScope
         * ==============
         * - Bound to LifecycleOwner's lifecycle
         * - Coroutines automatically cancelled when lifecycle destroyed
         * - Uses Dispatchers.Main by default
         */

        // Launch coroutine tied to lifecycle
        lifecycleScope.launch {
            // This coroutine is automatically cancelled in onDestroy()
            val data = withContext(Dispatchers.IO) {
                fetchDataFromNetwork()
            }
            updateUi(data)
        }

        /**
         * repeatOnLifecycle
         * =================
         * Collects Flow only when lifecycle is in specific state
         * Automatically stops collecting in other states
         */
        lifecycleScope.launch {
            // Only collect when lifecycle is at least STARTED
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }

        /**
         * flowWithLifecycle
         * ================
         * Alternative to repeatOnLifecycle
         * Similar behavior but different use cases
         */
        lifecycleScope.launch {
            viewModel.events
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { event ->
                    handleEvent(event)
                }
        }
    }

    // ------------------------------------------------------------------------
    // COROUTINE DISPATCHERS IN ANDROID
    // ------------------------------------------------------------------------

    fun demonstrateDispatchers() {
        lifecycleScope.launch {
            // Main dispatcher - UI thread
            val uiData = withContext(Dispatchers.Main) {
                updateUi("Main thread")
            }

            // IO dispatcher - Network, database, file operations
            val networkData = withContext(Dispatchers.IO) {
                fetchFromNetwork()
            }

            // Default dispatcher - CPU intensive work
            val processedData = withContext(Dispatchers.Default) {
                heavyComputation(networkData)
            }

            // Unconfined - Starts in caller thread, resumes in suspended thread
            // Generally avoid using
            withContext(Dispatchers.Unconfined) {
                // Uncertain threading behavior
            }
        }
    }

    private suspend fun fetchDataFromNetwork(): String = ""
    private fun updateUi(data: String) {}
    private suspend fun fetchFromNetwork(): String = ""
    private fun heavyComputation(data: String): String = ""
    private fun handleEvent(event: Any) {}
}

// ============================================================================
// LIFECYCLE STATES AND STATE AWARENESS
// ============================================================================

/**
 * Understanding Lifecycle.State
 */
fun demonstrateLifecycleStates(lifecycle: Lifecycle) {
    when (lifecycle.currentState) {
        Lifecycle.State.DESTROYED -> {
            // Component destroyed, nothing can be done
        }
        Lifecycle.State.INITIALIZED -> {
            // Initial state, before onCreate()
        }
        Lifecycle.State.CREATED -> {
            // After onCreate(), before onStart()
            // OR after onStop(), before onDestroy()
        }
        Lifecycle.State.STARTED -> {
            // After onStart(), before onResume()
            // OR after onPause(), before onStop()
        }
        Lifecycle.State.RESUMED -> {
            // After onResume(), user actively interacting
        }
    }

    // Check if lifecycle is at least in a certain state
    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        // Safe to interact with user
    }
}

// ============================================================================
// PRACTICAL LIFECYCLE PATTERNS
// ============================================================================

/**
 * Pattern: Lifecycle-Aware Resource Management
 */
class ResourceManager : DefaultLifecycleObserver {

    private val resources = mutableListOf<Closeable>()

    fun addResource(resource: Closeable) {
        resources.add(resource)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        resources.forEach { it.close() }
        resources.clear()
    }
}

/**
 * Pattern: Lifecycle-Aware Data Loading
 */
abstract class LifecycleAwareDataLoader<T> : DefaultLifecycleObserver {

    private var data: T? = null
    private val callbacks = mutableListOf<DataCallback<T>>()

    abstract suspend fun loadData(): T

    fun registerCallback(callback: DataCallback<T>, lifecycle: Lifecycle) {
        callbacks.add(callback)
        lifecycle.addObserver(this)

        // Deliver cached data if available
        data?.let { callback.onDataLoaded(it) }
    }

    override fun onStart(owner: LifecycleOwner) {
        // Refresh data when becoming visible
        owner.lifecycleScope.launch {
            data = loadData()
            callbacks.forEach { it.onDataLoaded(data!!) }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        callbacks.clear()
    }
}

interface DataCallback<T> {
    fun onDataLoaded(data: T)
}

/**
 * Pattern: Lifecycle-Aware Broadcast Receiver
 */
class LifecycleAwareBroadcastReceiver(
    private val context: Context,
    private val filter: IntentFilter
) : DefaultLifecycleObserver {

    private var receiver: BroadcastReceiver? = null

    override fun onStart(owner: LifecycleOwner) {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Handle broadcast
            }
        }.also {
            ContextCompat.registerReceiver(
                context,
                it,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        receiver?.let {
            context.unregisterReceiver(it)
            receiver = null
        }
    }
}
