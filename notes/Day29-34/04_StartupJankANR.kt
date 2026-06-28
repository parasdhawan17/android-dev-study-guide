/**
 * DAY 29-34: Startup Time, Jank, and ANR Prevention
 */

// ============ 1. APP STARTUP ============
/*
Startup types:
- Cold start: process not running.
- Warm start: process alive, Activity recreated.
- Hot start: Activity already in memory.

Cold start cost includes:
- Process creation.
- Application.onCreate().
- Dependency initialization.
- First Activity creation.
- First frame rendering.
*/

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Keep this lightweight.
        // Lazy initialize analytics, database, SDKs if possible.
    }
}

// ============ 2. IMPROVING STARTUP ============
/*
Techniques:
- Defer non-critical SDK initialization.
- Avoid disk/network on main thread.
- Use AndroidX Startup carefully.
- Show first meaningful frame quickly.
- Precompute only when it actually helps.
*/

// ============ 3. JANK ============
/*
Jank = missed frames / uneven frame pacing.
Causes:
- Main-thread blocking.
- Heavy recomposition/layout.
- Slow image decoding.
- Too much work in RecyclerView bind.
- GC pauses from excessive allocation.
*/

// ============ 4. ANR ============
/*
Common ANR thresholds:
- Input dispatch: about 5 seconds.
- BroadcastReceiver: about 10 seconds.
- Service start/foreground timing can also ANR.

Prevent by:
- Never do network/disk on main thread.
- Keep BroadcastReceiver quick; delegate to WorkManager.
- Avoid locks that block main thread.
- Use StrictMode during development.
*/

fun enableStrictModeForDebug() {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .build()
    )
}

// ============ INTERVIEW QUESTIONS ============
/*
Q: How would you investigate app startup slowness?
A: Measure cold/warm startup, inspect Application.onCreate, use System Trace/Startup profiler,
   defer non-critical initialization, and validate first-frame improvements.

Q: How do you debug jank?
A: Use Profile GPU Rendering, Layout Inspector recomposition counts, CPU profiler/System Trace,
   then reduce main-thread, layout, bind, or allocation work.
*/

open class Application { open fun onCreate() {} }
object StrictMode { fun setThreadPolicy(policy: ThreadPolicy) {}; class ThreadPolicy { class Builder { fun detectDiskReads()=this; fun detectDiskWrites()=this; fun detectNetwork()=this; fun penaltyLog()=this; fun build()=ThreadPolicy() } } }
