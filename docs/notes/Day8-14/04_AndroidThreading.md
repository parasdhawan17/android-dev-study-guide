# Android Threading

DAY 8-14: Android Threading, Handler, Looper, MessageQueue

Goal: Connect coroutine knowledge to Android's underlying main-thread model.

## 1. Main Thread Rule

Android UI toolkit is not thread-safe.
All UI mutations must happen on the main thread.

Why?
- Views keep mutable state without locks for performance.
- The main Looper serializes UI events, drawing, input, and callbacks.

````kotlin
fun updateTextSafely(activity: Activity, textView: TextView, text: String) {
    activity.runOnUiThread {
        textView.text = text
    }
}
````

## 2. Looper, Messagequeue, Handler

Looper:
- Runs an event loop on a thread.
- Main thread has a Looper automatically.

MessageQueue:
- Queue of Messages/Runnables to process.

Handler:
- Posts work to a thread's MessageQueue.

Mental model:
Handler.post { ... } -&gt; MessageQueue -&gt; Looper picks it up -&gt; executes on target thread.

````kotlin
class HandlerExample {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun postToMainThread() {
        mainHandler.post {
            // Runs on main thread
        }
    }

    fun postDelayedWork() {
        mainHandler.postDelayed({
            // Runs after delay, if not removed
        }, 1000)
    }

    fun cleanup() {
        mainHandler.removeCallbacksAndMessages(null)
    }
}
````

## 3. Handlerthread

HandlerThread is a thread with a Looper.
Useful before coroutines for background serial work.
Still appears in platform APIs, camera/media code, and legacy apps.

````kotlin
class LegacySerialWorker {
    private val thread = HandlerThread("serial-worker").apply { start() }
    private val handler = Handler(thread.looper)

    fun enqueue(task: () -> Unit) {
        handler.post(task)
    }

    fun shutdown() {
        thread.quitSafely()
    }
}
````

## 4. Executors And Thread Pools

Executors manage thread pools.
Coroutines often replace direct Executor usage in app code, but many Java APIs still require them.

FixedThreadPool:
- Bounded number of threads.

SingleThreadExecutor:
- Serial background work.

CachedThreadPool:
- Expands as needed; can grow too much if abused.

````kotlin
class ExecutorExample {
    private val executor = Executors.newFixedThreadPool(4)

    fun loadInBackground(callback: (String) -> Unit) {
        executor.execute {
            val result = blockingLoad()
            Handler(Looper.getMainLooper()).post {
                callback(result)
            }
        }
    }

    fun shutdown() = executor.shutdown()
}
````

## 5. ANR Prevention

ANR = Application Not Responding.
Common causes:
- Blocking main thread with network/file/database work.
- Long locks on main thread.
- Slow BroadcastReceiver.
- Deadlock involving main thread.

Rules:
- Keep main-thread work under a few milliseconds where possible.
- Move IO to Dispatchers.IO.
- Move CPU-heavy parsing/sorting to Dispatchers.Default.
- Use tracing/profiling instead of guessing.

````kotlin
class SafeRepository(private val dao: Dao, private val api: Api) {
    suspend fun refresh(): List<Item> = withContext(Dispatchers.IO) {
        val items = api.fetchItems()
        dao.insert(items)
        items
    }
}
````

## Interview Questions

Q: What is the relationship between Handler and Looper?
A: A Handler posts work into a MessageQueue associated with a Looper. The Looper runs on a
thread and executes queued messages one at a time.

Q: Why can blocking the main thread cause ANR?
A: The main thread processes input, lifecycle callbacks, rendering, and broadcasts. If it is
blocked, the system cannot deliver events and shows ANR after timeout.

Q: Are coroutines a replacement for threads?
A: Coroutines are an abstraction over asynchronous work. They still run on threads selected by
dispatchers, but they suspend instead of occupying a thread while waiting.

Stubs

````kotlin
class Activity { fun runOnUiThread(block: () -> Unit) = block() }
class TextView { var text: String = "" }
class Handler(val looper: Looper) { fun post(block: () -> Unit) {}; fun postDelayed(block: () -> Unit, delay: Long) {}; fun removeCallbacksAndMessages(token: Any?) {} }
class Looper { companion object { fun getMainLooper() = Looper() } }
class HandlerThread(val name: String) { val looper = Looper(); fun start() {}; fun quitSafely() {} }
object Executors { fun newFixedThreadPool(size: Int) = Executor() }
class Executor { fun execute(block: () -> Unit) = block(); fun shutdown() {} }
fun blockingLoad() = "data"
data class Item(val id: String)
interface Dao { fun insert(items: List<Item>) }
interface Api { fun fetchItems(): List<Item> }
````
