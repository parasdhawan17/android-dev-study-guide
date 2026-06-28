# Background Execution

DAY 5-7: Background Execution &amp; WorkManager

Android background execution limits (API 26+) and recommended patterns.

````kotlin
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.*
import androidx.work.PeriodicWorkRequest
import java.util.concurrent.TimeUnit
````

## 1. Background Execution Limits (Android 8+)

WHAT CHANGED IN OREO (API 26):

Before:
- Apps could start background services freely
- Implicit broadcasts delivered to manifest-registered receivers
- Background location always available

After:
- Background apps cannot start services (IllegalStateException)
- Most implicit broadcasts blocked
- Background location throttled
- Wake locks limited

What is "Background"?
┌─────────────────────────────────────────────────────────────────────┐
│ BACKGROUND APP =                                                      │
│   • No visible Activity                                               │
│   • No foreground Service                                             │
│   • Not in foreground by system (notification listener, etc.)          │
└─────────────────────────────────────────────────────────────────────┘

What STILL WORKS:
✓ Foreground Services (with notification)
✓ WorkManager (deferred work)
✓ AlarmManager exact alarms (with permission API 31+)
✓ High-priority FCM notifications
✓ JobScheduler (API 21+)

## 2. Workmanager (Recommended For Deferrable Work)

WorkManager features:
- Guaranteed execution (even if app closed or device restarted)
- Constraint-based scheduling (network, charging, battery)
- Chaining work together
- Observing work state
- Testable

Simple one-time work

````kotlin
class SimpleSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            syncData()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun syncData() {
        // Sync logic here
    }
}
````

Work with retry logic

````kotlin
class UploadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    companion object {
        const val KEY_INPUT_FILE = "input_file"
        const val KEY_OUTPUT_URL = "output_url"
        const val MAX_RETRIES = 3
    }

    override suspend fun doWork(): Result {
        val inputFile = inputData.getString(KEY_INPUT_FILE)
            ?: return Result.failure(workDataOf("error" to "No input file"))

        return try {
            val url = uploadFile(inputFile)
            Result.success(workDataOf(KEY_OUTPUT_URL to url))
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) {
                // Retry with exponential backoff (configured at enqueue time)
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to e.message))
            }
        }
    }

    private suspend fun uploadFile(file: String): String {
        // Upload logic
        return "https://example.com/uploaded/$file"
    }
}
````

## 3. Workmanager Constraints

````kotlin
fun enqueueConstrainedWork(context: Context) {
    // Define constraints
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)  // WiFi or cellular
        // .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only
        .setRequiresCharging(true)                       // Device charging
        .setRequiresBatteryNotLow(true)                   // Battery above threshold
        // .setRequiresStorageNotLow(true)                // Storage not low
        // .setRequiresDeviceIdle(true)                   // Device idle (API 23+)
        .build()

    // Create work request
    val syncWork = OneTimeWorkRequestBuilder<UploadWorker>()
        .setConstraints(constraints)
        .setInputData(workDataOf("input_file" to "document.pdf"))
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            WorkRequest.MIN_BACKOFF_MILLIS,
            TimeUnit.MILLISECONDS
        )
        .addTag("upload")
        .build()

    // Enqueue work
    WorkManager.getInstance(context).enqueue(syncWork)
}
````

## 4. Unique Work (Prevent Duplicates)

````kotlin
fun enqueueUniqueWork(context: Context) {
    val syncWork = OneTimeWorkRequestBuilder<SimpleSyncWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "daily_sync",                           // Unique name
        ExistingWorkPolicy.KEEP,                // Policy: KEEP, REPLACE, APPEND
        syncWork
    )

    /*
    ExistingWorkPolicy options:
    - KEEP: Keep existing, ignore new work
    - REPLACE: Cancel existing, enqueue new
    - APPEND: Add to end of existing work chain
    - APPEND_OR_REPLACE: Append, or start new if existing is finished
    */
}
````

## 5. Periodic Work

````kotlin
fun schedulePeriodicWork(context: Context) {
    // Minimum interval: 15 minutes (enforced by system)
    val dailyCleanup = PeriodicWorkRequestBuilder<CleanupWorker>(
        15, TimeUnit.MINUTES
    )
        .setConstraints(
            Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .setRequiresCharging(true)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "daily_cleanup",
        ExistingPeriodicWorkPolicy.KEEP,
        dailyCleanup
    )

    /*
    ExistingPeriodicWorkPolicy:
    - KEEP: Keep existing periodic work
    - UPDATE: Update existing work (flex interval)
    */
}

class CleanupWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // Clean up old cache files
        return Result.success()
    }
}
````

## 6. Work Chaining

````kotlin
fun chainWork(context: Context) {
    // Define individual work units
    val compressWork = OneTimeWorkRequestBuilder<CompressWorker>()
        .addTag("compress")
        .build()

    val filterWork = OneTimeWorkRequestBuilder<FilterWorker>()
        .addTag("filter")
        .build()

    val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .addTag("upload")
        .build()

    val notifyWork = OneTimeWorkRequestBuilder<NotificationWorker>()
        .addTag("notify")
        .build()

    // Chain: compress → filter → upload → notify
    WorkManager.getInstance(context)
        .beginUniqueWork(
            "image_processing",
            ExistingWorkPolicy.REPLACE,
            compressWork
        )
        .then(filterWork)
        .then(uploadWork)
        .then(notifyWork)
        .enqueue()
}
````

Parallel then sequential

````kotlin
fun parallelThenSequential(context: Context) {
    val syncA = OneTimeWorkRequestBuilder<SimpleSyncWorker>().build()
    val syncB = OneTimeWorkRequestBuilder<SimpleSyncWorker>().build()
    val merge = OneTimeWorkRequestBuilder<MergeWorker>().build()

    // syncA and syncB run in parallel, then merge runs
    WorkManager.getInstance(context)
        .beginWith(listOf(syncA, syncB))
        .then(merge)
        .enqueue()
}

class CompressWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()
}

class FilterWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()
}

class NotificationWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        showNotification("Upload Complete", "Your files have been uploaded.")
        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        // Notification logic
    }
}

class MergeWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()
}
````

## 7. Observing Work State

In ViewModel or Activity:

class UploadViewModel(application: Application) : AndroidViewModel(application) {

fun uploadFile(fileUri: String): LiveData&lt;WorkInfo&gt; {
val uploadWork = OneTimeWorkRequestBuilder&lt;UploadWorker&gt;()
.setInputData(workDataOf("file" to fileUri))
.build()

WorkManager.getInstance(getApplication()).enqueue(uploadWork)

return WorkManager.getInstance(getApplication())
.getWorkInfoByIdLiveData(uploadWork.id)
}
}

// In Compose:
@Composable
fun UploadProgress(workId: UUID) {
val workInfo by WorkManager.getInstance(LocalContext.current)
.getWorkInfoByIdLiveData(workId)
.observeAsState()

when (workInfo?.state) {
WorkInfo.State.ENQUEUED -&gt; Text("Waiting...")
WorkInfo.State.RUNNING -&gt; CircularProgressIndicator()
WorkInfo.State.SUCCEEDED -&gt; Text("Complete!")
WorkInfo.State.FAILED -&gt; Text("Failed")
WorkInfo.State.BLOCKED -&gt; Text("Waiting for constraints...")
WorkInfo.State.CANCELLED -&gt; Text("Cancelled")
null -&gt; Text("Not started")
}
}

## 8. Foreground Services

Use for: Immediate execution that user is actively aware of
Examples: Music playback, active navigation, file download in progress

Foreground Service for long-running immediate work

````kotlin
class MusicPlaybackService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "music_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("Playing: Track Name")

        // API 31+ requires foregroundServiceType in manifest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start playback
        startPlayback()

        return START_STICKY // Restart if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Player")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    private fun startPlayback() {
        // Playback logic
    }

    private fun stopPlayback() {
        // Stop logic
    }
}
````

Data Sync Foreground Service (for immediate sync)

````kotlin
class DataSyncService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "sync_channel")
            .setContentTitle("Syncing Data")
            .setContentText("Uploading changes...")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setProgress(100, 0, false)
            .build()

        startForeground(2, notification)

        // Do sync work...

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
````

## 9. Workmanager vs Foreground Service Decision Tree

┌─────────────────────────────────────┐
│   Does the work need to run NOW?    │
└──────────────┬──────────────────────┘
│
YES ───────────┴─────────── NO
│                              │
┌───────────▼──────────┐     ┌───────────▼──────────┐
│ Is user actively       │     │  Can work be deferred?  │
│ aware of the work?     │     │  (hours/days later)     │
└───────────┬───────────┘     └───────────┬──────────┘
│                              │
YES ─────────┴───────── NO         YES ─────┴───── NO
│                      │           │                │
┌────────▼────────┐  ┌──────────▼────────┐  │         ┌────▼────┐
│ FOREGROUND       │  │ Can work wait a    │  │         │ Not     │
│ SERVICE          │  │ few minutes?       │  │         │ possible│
│ (notification    │  └──────────┬─────────┘  │         │ on Oreo+│
│  required)       │             │           │         └─────────┘
└─────────────────┘   YES ──────┴───── NO    │
│                │     │
┌──────────▼────────┐  ┌────▼────┐│
│ WORKMANAGER with   │  │ AlarmManager│
│ expedited work     │  │ (exact time)│
│ (API 31+)          │  │ or FCM      │
└───────────────────┘  └───────────┘
│ for high    │
│ priority    │
└─────────────┘

## 10. Android 12+ (Api 31+) Service Changes

New requirements for Foreground Services:

1. Must declare foregroundServiceType in manifest:

&lt;service
android:name=".MusicPlaybackService"
android:foregroundServiceType="mediaPlayback"
android:exported="false" /&gt;

Types available:
- camera, connectedDevice, dataSync, location, mediaPlayback
- mediaProjection, microphone, phoneCall, remoteMessaging
- shortService, specialUse, systemExempted

2. Restrictions on starting from background:
- Cannot start foreground service from background except:
User interaction (notification tap, broadcast receiver)
High-priority FCM notification
Phone call, alarm, etc.

3. ShortService type: For quick tasks (&lt;3 minutes)
- No notification required initially
- Must stop within 3 minutes
- Automatically stopped if timeout

4. WorkManager expedited work:
- Uses foreground service internally
- No notification needed (system handles it)

Expedited Work (API 31+)

````kotlin
fun expeditedWork(context: Context) {
    val urgentWork = OneTimeWorkRequestBuilder<SimpleSyncWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        // Policy: DROP_WORK_REQUEST if out of quota, or
        //         RUN_AS_NON_EXPEDITED_WORK_REQUEST to run normally
        .build()

    WorkManager.getInstance(context).enqueue(urgentWork)
}
````

## 11. Common Patterns

Pattern: Sync with retry and notification

````kotlin
class RetryableSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("sync", Context.MODE_PRIVATE)
        var attempts = prefs.getInt("attempts", 0)

        return try {
            performSync()
            prefs.edit().putInt("attempts", 0).apply()
            Result.success()
        } catch (e: Exception) {
            attempts++
            prefs.edit().putInt("attempts", attempts).apply()

            if (attempts >= 5) {
                // Too many attempts - show error notification
                showErrorNotification("Sync failed after 5 attempts")
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    private suspend fun performSync() {
        // Sync logic
    }

    private fun showErrorNotification(message: String) {
        // Show notification
    }
}
````

Pattern: Upload with progress

````kotlin
class ProgressUploadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val file = inputData.getString("file") ?: return Result.failure()

        setForeground(
            ForegroundInfo(
                1,
                createProgressNotification(0)
            )
        )

        // Simulate chunked upload with progress updates
        for (progress in 0..100 step 10) {
            setProgress(workDataOf("progress" to progress))

            setForeground(
                ForegroundInfo(
                    1,
                    createProgressNotification(progress)
                )
            )

            kotlinx.coroutines.delay(500)
        }

        return Result.success()
    }

    private fun createProgressNotification(progress: Int): Notification {
        return NotificationCompat.Builder(applicationContext, "upload_channel")
            .setContentTitle("Uploading File")
            .setContentText("$progress% complete")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
    }
}
````

## 12. Testing Workmanager

// Enable testing init provider in AndroidManifest.xml:
&lt;provider
android:name="androidx.work.impl.WorkManagerInitializer"
android:authorities="${applicationId}.workmanager-init"
tools:node="remove" /&gt;

// In Application.onCreate():
val config = Configuration.Builder()
.setMinimumLoggingLevel(android.util.Log.DEBUG)
.setExecutor(SynchronousExecutor()) // For tests
.build()

WorkManager.initialize(this, config)

// Test:
@Test
fun testUploadWorker() {
val worker = TestListenableWorkerBuilder&lt;UploadWorker&gt;(context)
.setInputData(workDataOf("file" to "test.txt"))
.build()

runBlocking {
val result = worker.doWork()
assertThat(result).isEqualTo(Result.success())
}
}

## Interview Questions

Q: When would you use WorkManager vs Foreground Service?
A: WorkManager for deferrable, guaranteed work (sync, upload, cleanup).
Foreground Service for immediate, user-aware work (playback, navigation).

Q: How does WorkManager guarantee execution?
A: Uses JobScheduler (API 21+), AlarmManager + BroadcastReceiver (API &lt; 21),
or Firebase JobDispatcher (with Play Services). Falls back appropriately.

Q: What's the minimum interval for PeriodicWorkRequest?
A: 15 minutes. System enforces this to prevent battery drain.

Q: How do you handle work chaining with conditional logic?
A: Use beginUniqueWork() with ExistingWorkPolicy, then chain with .then().
Worker outputs can be inputs to next worker via setInputMerger().

Q: What's new with Android 12 (API 31) for background execution?
A: Foreground services need foregroundServiceType declaration.
Background start restrictions tightened.
Expedited WorkManager jobs available (use foreground service internally).

Q: How do you observe work progress?
A: WorkManager.getWorkInfoByIdLiveData() returns WorkInfo with state.
Can also use setProgress() from worker for intermediate updates.

Stub classes for compilation

````kotlin
annotation class ExperimentalWorkerApi
class ForegroundInfo(
    val notificationId: Int,
    val notification: Notification
)
class OutOfQuotaPolicy {
    companion object {
        const val RUN_AS_NON_EXPEDITED_WORK_REQUEST = 1
    }
}
fun <T : CoroutineWorker> OneTimeWorkRequestBuilder(workerClass: Class<T>): OneTimeWorkRequestBuilder<T> { TODO() }
inline fun <reified T : CoroutineWorker> OneTimeWorkRequestBuilder(): OneTimeWorkRequestBuilder<T> = OneTimeWorkRequestBuilder(T::class.java)
class OneTimeWorkRequestBuilder<T> {
    fun setConstraints(constraints: Constraints) = this
    fun setInputData(data: Data) = this
    fun setBackoffCriteria(policy: BackoffPolicy, duration: Long, unit: TimeUnit) = this
    fun addTag(tag: String) = this
    fun setExpedited(policy: Int) = this
    fun build(): WorkRequest { TODO() }
}
class PeriodicWorkRequestBuilder<T> {
    constructor(repeatInterval: Long, unit: TimeUnit)
    fun setConstraints(constraints: Constraints) = this
    fun build(): WorkRequest { TODO() }
}
class WorkRequest
class Constraints {
    class Builder {
        fun setRequiredNetworkType(type: NetworkType) = this
        fun setRequiresCharging(requires: Boolean) = this
        fun setRequiresBatteryNotLow(requires: Boolean) = this
        fun setRequiresDeviceIdle(requires: Boolean) = this
        fun build(): Constraints { TODO() }
    }
}
enum class NetworkType { CONNECTED, UNMETERED }
enum class BackoffPolicy { EXPONENTIAL }
object WorkRequest {
    const val MIN_BACKOFF_MILLIS = 10000L
}
fun workDataOf(vararg pairs: Pair<String, Any>): Data { TODO() }
class Data
class WorkManager {
    companion object {
        fun getInstance(context: Context): WorkManager { TODO() }
    }
    fun enqueue(work: WorkRequest) {}
    fun enqueueUniqueWork(name: String, policy: ExistingWorkPolicy, work: WorkRequest) {}
    fun enqueueUniquePeriodicWork(name: String, policy: ExistingPeriodicWorkPolicy, work: WorkRequest) {}
    fun beginUniqueWork(name: String, policy: ExistingWorkPolicy, work: WorkRequest): WorkContinuation { TODO() }
}
class WorkContinuation {
    fun then(work: WorkRequest): WorkContinuation { TODO() }
    fun enqueue() {}
}
enum class ExistingWorkPolicy { KEEP, REPLACE, APPEND, APPEND_OR_REPLACE }
enum class ExistingPeriodicWorkPolicy { KEEP, UPDATE }
abstract class CoroutineWorker(context: Context, params: WorkerParameters) {
    val runAttemptCount: Int = 0
    val inputData: Data = Data()
    suspend fun setForeground(foregroundInfo: ForegroundInfo) {}
    suspend fun setProgress(data: Data) {}
    abstract suspend fun doWork(): Result
}
class WorkerParameters
object Result {
    fun success(): Result { TODO() }
    fun success(outputData: Data): Result { TODO() }
    fun failure(): Result { TODO() }
    fun failure(outputData: Data): Result { TODO() }
    fun retry(): Result { TODO() }
}
class SynchronousExecutor
class Configuration {
    class Builder {
        fun setMinimumLoggingLevel(level: Int) = this
        fun setExecutor(executor: SynchronousExecutor) = this
        fun build(): Configuration { TODO() }
    }
}
````
