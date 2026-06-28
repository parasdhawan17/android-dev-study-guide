# Activity Lifecycle

DAY 3-4: ANDROID COMPONENT LIFECYCLES
Topic 1: Activity Lifecycle Deep Dive

ACTIVITY LIFECYCLE OVERVIEW

The Activity lifecycle consists of 6 main states:

                   onCreate()
                      ↓
                  CREATED
                      ↓
                   onStart()
                      ↓
                  STARTED
                      ↓
                   onResume()
                      ↓
                  RESUMED (Active - user can interact)
                      ↓
                   onPause()
                      ↓
                  STARTED
                      ↓
                   onStop()
                      ↓
                  CREATED
                      ↓
                   onDestroy()
                      ↓
                 DESTROYED

KEY STATE FLOW:
1. Activity launched → onCreate() → onStart() → onResume()
2. User navigates away → onPause() → onStop()
3. User returns → onRestart() → onStart() → onResume()
4. Activity finished → onPause() → onStop() → onDestroy()

````kotlin
class ActivityLifecycleDemo : AppCompatActivity() {
````

## Oncreate() - Activity Initialization

````kotlin
    // Called ONCE when Activity is first created
    // Good for: UI setup, ViewModel initialization, restoring saved state
    // BAD for: Starting services, heavy operations (blocks UI thread)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * SAVED INSTANCE STATE
         * =====================
         * savedInstanceState is Bundle containing data from onSaveInstanceState()
         * - NULL on fresh start
         * - NON-NULL when Activity recreated (config change, process death)
         */
        if (savedInstanceState != null) {
            val restoredData = savedInstanceState.getString("key_data")
            // Restore UI state
        }

        setContentView(R.layout.activity_demo)

        // Initialize ViewModel here
        viewModel = ViewModelProvider(this)[DemoViewModel::class.java]

        // Setup UI components
        setupViews()

        // Register lifecycle observers
        lifecycle.addObserver(MyLifecycleObserver())

        println("🔵 onCreate: Activity created")
    }
````

## Onstart() - Activity Becoming Visible

````kotlin
    // Activity enters STARTED state
    // Good for: Refreshing data, starting animations, location updates
    // Called after onCreate() or onRestart()
    override fun onStart() {
        super.onStart()

        // Register broadcast receivers
        // Start location updates
        // Refresh UI data

        println("🟢 onStart: Activity visible but not focused")
    }
````

## Onresume() - Activity Active And Focused

````kotlin
    // Activity enters RESUMED state - user can interact
    // Good for: Start camera preview, resume animations, acquire resources
    // Called after onStart() or when returning from paused state
    override fun onResume() {
        super.onResume()

        // Resume camera preview
        // Resume video playback
        // Re-acquire wake locks or sensors

        println("🟡 onResume: Activity active - user can interact")
    }
````

## Onpause() - Activity Losing Focus

````kotlin
    // Activity leaving RESUMED state
    // Good for: Stop animations, release camera, save lightweight state
    // Called when: Dialog appears, split screen, user leaves
    // IMPORTANT: Must execute quickly - next Activity won't resume until this completes!
    override fun onPause() {
        super.onPause()

        // Pause video playback
        // Stop camera preview (but don't release)
        // Stop unnecessary animations
        // Save UI state (cursor position, form data)

        println("🟠 onPause: Activity no longer focused")
    }
````

## Onstop() - Activity No Longer Visible

````kotlin
    // Activity enters CREATED state (not visible)
    // Good for: Heavy cleanup, stop location updates, save data to database
    // Called when: Activity completely covered, user navigates away
    override fun onStop() {
        super.onStop()

        // Stop location updates
        // Unregister broadcast receivers
        // Save persistent data to database
        // Stop heavy background operations

        println("🔴 onStop: Activity no longer visible")
    }
````

## Onrestart() - Activity Restarting

````kotlin
    // Called ONLY when Activity was stopped and is starting again
    // Rarely overridden - most logic goes in onStart()
    override fun onRestart() {
        super.onRestart()
        println("🔄 onRestart: Activity restarting after being stopped")
    }
````

## Ondestroy() - Activity Destroyed

````kotlin
    // Final cleanup - Activity being destroyed
    // Good for: Release all resources, close database connections
    // Called when: User presses back, finish() called, system kills app
    // Note: May not be called if system kills process!
    override fun onDestroy() {
        super.onDestroy()

        // Release all resources
        // Close database connections
        // Cancel all coroutines/background tasks

        println("⚫ onDestroy: Activity destroyed")
    }
````

## Onsaveinstancestate() - Save State Before Death

````kotlin
    // Called when system might kill Activity (config change, low memory)
    // Called AFTER onStop() (in API 28+, before in older versions)
    // Use for: Saving transient UI state (scroll position, form data)
    // Don't use for: Persistent data (use database/SharedPreferences)
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save UI state
        outState.putString("key_data", userInput)
        outState.putInt("key_scroll_position", scrollY)

        println("💾 onSaveInstanceState: State saved")
    }
````

## Onrestoreinstancestate() - Restore Saved State

````kotlin
    // Called after onStart() when Activity recreated from saved state
    // Alternative: Check savedInstanceState in onCreate()
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val restoredData = savedInstanceState.getString("key_data")
        // Restore UI state

        println("📂 onRestoreInstanceState: State restored")
    }
}
````

LIFECYCLE CALLBACK COMPARISON TABLE

Callback          | Called When                          | Use Case
------------------|--------------------------------------|-------------------------------------------
onCreate()        | Activity first created               | UI setup, ViewModel init, restore state
onStart()         | Activity becoming visible            | Refresh data, start location updates
onResume()        | Activity active &amp; focused            | Resume camera, animations, user input
onPause()         | Activity losing focus                | Pause camera, stop animations (QUICK!)
onStop()          | Activity no longer visible           | Heavy cleanup, save to database
onRestart()       | Activity restarting                  | Rarely used (use onStart())
onDestroy()       | Activity being destroyed             | Final cleanup, release resources
onSaveInstanceState() | Before potential destruction     | Save transient UI state
onRestoreInstanceState() | After recreation with state   | Restore UI state

CONFIGURATION CHANGES

Configuration changes (rotation, language, theme) destroy and recreate Activity

ORIGINAL Activity:
  onPause() → onStop() → onSaveInstanceState() → onDestroy()

NEW Activity:
  onCreate(savedInstanceState) → onStart() → onRestoreInstanceState() → onResume()

HANDLING OPTIONS:
1. Let system destroy/recreate (default) - use ViewModel + SavedStateHandle
2. Handle manually: android:configChanges="orientation|screenSize"
3. Use ViewModel to survive config changes (recommended)

PROCESS DEATH SCENARIO

When system kills app due to low memory:

1. App in background → System kills process
2. User returns → Process recreated
3. Activity stack restored with saved state

onSaveInstanceState() IS called (before onStop or after)
onDestroy() may NOT be called

SOLUTION: ViewModel + SavedStateHandle
