# Kotlin Coroutines: Complete Study Guide

From basic concepts to advanced optimizations.

---

## Table of Contents

1. [Phase 1: Foundations](#phase-1-foundations)
2. [Phase 2: Core Building Blocks](#phase-2-core-building-blocks)
3. [Phase 3: Intermediate Concepts](#phase-3-intermediate-concepts)
4. [Phase 4: Kotlin Flow](#phase-4-kotlin-flow)
5. [Phase 5: Advanced Topics](#phase-5-advanced-topics)
  - [5.1 Coroutines vs Threads: Performance Comparison](#51-coroutines-vs-threads-performance-comparison)
  - [5.2 Coroutine Internals - Deep Dive](#52-coroutine-internals---deep-dive)
  - [5.3 Memory Layout and GC Impact](#53-memory-layout-and-gc-impact)
  - [5.4 Event Loop and Dispatcher Internals](#54-event-loop-and-dispatcher-internals)
  - [5.5 Exception Handling and Cancellation Propagation](#55-exception-handling-and-cancellation-propagation)
  - [5.6 Select Expression](#56-select-expression)
  - [5.7 Exception Handling in Android Architecture](#57-exception-handling-in-android-architecture)
  - [5.8 Channels](#58-channels-communication-between-coroutines)
6. [Phase 6: Advanced Optimizations](#phase-6-advanced-optimizations)
7. [Phase 7: Common Pitfalls](#phase-7-common-pitfalls)
8. [Recommended Learning Path](#recommended-learning-path)

---

## Phase 1: Foundations

### 1.1 What Are Coroutines?

**Coroutines** are lightweight threads that allow you to write asynchronous, non-blocking code in a sequential style.

- They are not threads - they can suspend and resume without blocking the underlying thread
- **Key benefit**: Simplify async programming by eliminating callback hell while maintaining efficiency

```kotlin
// Instead of callbacks:
fetchUser(id) { user ->
    fetchOrders(user) { orders ->
        showOrders(orders)
    }
}

// Coroutines give you sequential-looking code:
val user = fetchUser(id)      // suspends, doesn't block
val orders = fetchOrders(user) // suspends, doesn't block
showOrders(orders)
```

### 1.2 Core Concepts to Master


| Concept                | Description                                         | Key Functions                      |
| ---------------------- | --------------------------------------------------- | ---------------------------------- |
| **Suspend Functions**  | Functions that can pause execution and resume later | `suspend fun`                      |
| **Coroutine Builders** | Ways to create coroutines                           | `launch`, `async`, `runBlocking`   |
| **CoroutineScope**     | Lifecycle boundary for coroutines                   | `lifecycleScope`, `viewModelScope` |
| **Job**                | Handle to a coroutine that can be canceled          | `job.cancel()`, `job.join()`       |
| **Deferred**           | A Job that returns a result                         | `async { }.await()`                |


### 1.3 Your First Coroutine

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {  // Creates a scope and blocks main thread
    launch {               // Launches new coroutine
        delay(1000L)       // Suspends for 1 second (non-blocking)
        println("World!")
    }
    println("Hello,")      // Main coroutine continues immediately
    delay(2000L)           // Wait for launch to complete
}
// Output: Hello, [1s delay] World!
```

**Key observation**: `delay` is a suspend function - it suspends the coroutine without blocking the thread.

---

## Phase 2: Core Building Blocks

### 2.1 Coroutine Builders

#### `launch` vs `async` vs `runBlocking`

```kotlin
// launch - fire and forget, returns Job
val job = launch {
    doSomething()
    println("Done")
}
job.join() // Wait for completion

// async - launches and returns Deferred (future result)
val deferred = async {
    computeExpensiveValue()
}
val result = deferred.await() // Get the result (suspends until ready)

// runBlocking - blocks current thread, mainly for testing/main
runBlocking {
    // Use this when you need to bridge sync and async code
}
```

### 2.2 Structured Concurrency

Always use **structured concurrency** - coroutines are launched in a scope and the scope controls their lifecycle.

```kotlin
// Bad - GlobalScope is discouraged
GlobalScope.launch {
    // Runs forever, not tied to any lifecycle
}

// Good - Scoped to ViewModel lifecycle
class MyViewModel : ViewModel() {
    fun fetchData() {
        viewModelScope.launch {
            val data = repository.getData()
            _uiState.value = data
        }  // Automatically cancelled when ViewModel clears
    }
}

// Good - Custom scope with proper dispatcher
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
```

### 2.3 Dispatchers (Where Coroutines Run)


| Dispatcher               | Use Case                                                           |
| ------------------------ | ------------------------------------------------------------------ |
| `Dispatchers.Main`       | UI updates on Android main thread                                  |
| `Dispatchers.IO`         | Network, database, file operations                                 |
| `Dispatchers.Default`    | CPU-intensive work (sorting, parsing)                              |
| `Dispatchers.Unconfined` | Starts in caller thread, continues in resumed thread (rarely used) |


```kotlin
launch(Dispatchers.IO) {
    val data = fetchFromNetwork()  // Network call on IO thread pool
    withContext(Dispatchers.Main) {
        updateUI(data)               // Back to main thread for UI
    }
}
```

---

## Phase 3: Intermediate Concepts

### 3.1 Async/Await Patterns

```kotlin
// Sequential (slow - 2 seconds total)
suspend fun fetchTwoUsers(): Pair<User, User> {
    val user1 = fetchUser("1")  // 1s
    val user2 = fetchUser("2")  // 1s
    return user1 to user2
}

// Concurrent (fast - 1 second total)
suspend fun fetchTwoUsers(): Pair<User, User> = coroutineScope {
    val user1 = async { fetchUser("1") }  // Start immediately
    val user2 = async { fetchUser("2") }  // Start immediately
    user1.await() to user2.await()         // Wait for both
}

// Error handling with async
coroutineScope {
    val deferred1 = async { fetchUser("1") }
    val deferred2 = async { fetchUser("2") }
    
    try {
        val user1 = deferred1.await()
        val user2 = deferred2.await()
    } catch (e: Exception) {
        // Both coroutines fail if one fails (by default)
    }
}

// WHY BOTH FAIL: Coroutine Job Hierarchy and Exception Propagation
//
// By default, coroutines created in a scope use regular Job (not SupervisorJob).
// The Job hierarchy works like this:
//
// coroutineScope {                    // Parent Job
//     async { }  // deferred1  ─────┬── Child 1 (sibling relationship)
//     async { }  // deferred2  ─────┘   Child 2
// }
//
// When Child 1 fails:
// 1. Exception propagates UP to Parent Job
// 2. Parent Job enters "Cancelling" state
// 3. Parent cancels ALL other children (Child 2 gets CancellationException)
// 4. Parent completes exceptionally
// 5. Parent's parent also gets cancelled (propagates up the entire tree)
//
// This is "structured concurrency" - failures cascade to siblings via parent
//
// SOLUTION 1: Use supervisorScope (wraps children in SupervisorJob)
supervisorScope {
    val deferred1 = async { fetchUser("1") }
    val deferred2 = async { fetchUser("2") }
    
    val user1 = try {
        deferred1.await()
    } catch (e: Exception) {
        null  // Handle individually
    }
    
    val user2 = try {
        deferred2.await()
    } catch (e: Exception) {
        null  // Other coroutine unaffected
    }
}
//
// SOLUTION 2: Wrap each async in its own try-catch before await
// (Still cancels sibling, but you get result from successful one)
coroutineScope {
    val deferred1 = async {
        try { fetchUser("1") } catch (e: Exception) { null }
    }
    val deferred2 = async {
        try { fetchUser("2") } catch (e: Exception) { null }
    }
    
    // Both will complete (one may be null), no cascade cancellation
    val user1 = deferred1.await()
    val user2 = deferred2.await()
}
//
// SOLUTION 3: CoroutineExceptionHandler (for "fire and forget" coroutines)
val handler = CoroutineExceptionHandler { _, exception ->
    println("Handled: $exception")
}

supervisorScope {
    launch(handler) { task1() }  // Fails independently
    launch(handler) { task2() }  // Continues running
}
```

### 3.2 CoroutineContext Explained

`CoroutineContext` is a collection of elements that define coroutine behavior:

```kotlin
// CoroutineContext = Job + Dispatcher + CoroutineName + ... 

launch(
    Dispatchers.IO +                    // Where to run
    CoroutineName("NetworkRequest") +   // Debug name
    CoroutineExceptionHandler { _, e ->   // Error handling
        logError(e)
    }
) {
    // Coroutine body
}

// Context can be inherited and overridden
val parentScope = CoroutineScope(Dispatchers.Main)
parentScope.launch(Dispatchers.IO) {  // Overrides parent's dispatcher
    // Runs on IO
}
```

### 3.3 Exception Handling

```kotlin
// SupervisorJob - Child failures don't affect siblings
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

scope.launch {
    launch { task1() }  // If this fails...
    launch { task2() }  // ...this still runs
}

// try-catch with suspend functions
launch {
    try {
        riskyOperation()
    } catch (e: IOException) {
        // Handle error
    }
}

// CoroutineExceptionHandler - for unhandled exceptions
val handler = CoroutineExceptionHandler { _, exception ->
    println("Caught $exception")
}

GlobalScope.launch(handler) {
    throw RuntimeException("Oops")
}
```

---

## Phase 4: Kotlin Flow

### 4.1 What is Flow?

Flow is a cold stream that emits values asynchronously - like a suspendable Sequence.

```kotlin
// Cold stream - starts emitting when collected
val userFlow: Flow<User> = flow {
    emit(fetchUser("1"))   // suspending emit
    delay(1000)
    emit(fetchUser("2"))
}

// Collect (terminal operator)
userFlow.collect { user ->
    println(user)  // Called for each emission
}

// Flow operators (lazy, suspending)
userFlow
    .filter { it.isActive }
    .map { it.name.uppercase() }
    .catch { e -> emit("Error: $e") }  // Error handling
    .collect { println(it) }
```

### 4.2 Flow Builders & Operators

```kotlin
// Builders
flow { /* emit manually */ }
flowOf(1, 2, 3)
(1..5).asFlow()
callbackFlow { /* convert callbacks */ }

// Intermediate operators (return new Flow)
.map { transform(it) }
.filter { predicate(it) }
.transform {  // Most flexible
    emit(it * 2)
    if (it > 5) emit(it * 3)
}
.buffer(10)  // Conflate emissions
.debounce(300)  // Wait for pause in emissions

// Terminal operators (suspend, trigger collection)
.collect { }
.first()
.reduce { acc, value -> acc + value }
.fold(initial) { acc, value -> acc + value }
.toList()
```

### 4.3 StateFlow & SharedFlow (Hot Streams)

```kotlin
// StateFlow - always has a value, only emits on change
private val _uiState = MutableStateFlow(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// Update state
_uiState.value = UiState.Success(data)
_uiState.update { it.copy(isLoading = false) }  // Atomic update

// SharedFlow - broadcasts events to multiple collectors
private val _events = MutableSharedFlow<UiEvent>()
val events: SharedFlow<UiEvent> = _events.asSharedFlow()

// Configure replay/cache
MutableSharedFlow(
    replay = 0,           // Don't replay to new collectors
    extraBufferCapacity = 1  // Buffer 1 emission
)
```

### 4.4 Flow in Android Architecture

```kotlin
class UserRepository @Inject constructor(
    private val api: UserApi,
    private val db: UserDao
) {
    // Cold flow from database
    fun getUsers(): Flow<List<User>> = db.getAllUsers()
    
    // Hot flow for one-time events
    private val _refreshTrigger = MutableSharedFlow<Unit>()
    
    // Combined flow
    val userStream: Flow<List<User>> = _refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest { api.fetchUsers() }  // Cancel previous if new arrives
        .catch { e -> emit(emptyList()) }
        .flowOn(Dispatchers.IO)
}
```

---

## Phase 5: Advanced Topics

### 5.1 Coroutines vs Threads: Performance Comparison

#### Why Coroutines Are Faster

**1. Thread Context Switching Overhead**


| Operation       | Thread                        | Coroutine         |
| --------------- | ----------------------------- | ----------------- |
| Creation        | ~~1,000,000 ns (~~1ms)        | ~100 ns           |
| Context Switch  | ~1,000-2,000 ns               | ~10-100 ns        |
| Memory Overhead | ~1MB stack per thread         | ~KB per coroutine |
| Max Concurrent  | ~1,000-10,000 (limited by OS) | 100,000+ easily   |


**2. Thread Stack vs Coroutine Heap Allocation**

```kotlin
// Threads allocate 1MB+ stack upfront
val thread = Thread {
    // Pre-allocated 1MB stack regardless of usage
}.apply { start() }

// Coroutines allocate tiny continuations on heap
launch {
    // Minimal initial allocation
    delay(1000)  // Suspends - frees the thread
    // Resumes with minimal state restoration
}
```

**3. JVM Thread Memory Model**

Every Java/Kotlin thread requires:

- **Thread Stack**: 1MB default (512KB-2MB configurable)
- **Kernel Thread**: OS-managed resource
- **Thread Local Storage**: Additional memory
- **GC Roots**: More objects to scan during GC

**Coroutine Memory Model**:

- **Continuation Object**: ~100-200 bytes per suspension point
- **No Kernel Resource**: Runs on shared thread pool
- **Heap Allocated**: Managed by JVM GC efficiently

#### Real-World Performance Numbers

```kotlin
// Benchmark: Launch 100,000 concurrent operations

// Using Threads (WILL CRASH with OutOfMemoryError)
val threads = List(100_000) {
    Thread {
        Thread.sleep(1000)
        println(it)
    }.apply { start() }
}
threads.forEach { it.join() }
// Result: JVM crashes around 4,000-8,000 threads

// Using Coroutines (COMPLETES SUCCESSFULLY)
runBlocking {
    val jobs = List(100_000) {
        launch {
            delay(1000)
            println(it)
        }
    }
    jobs.joinAll()
}
// Result: Completes in ~1 second using ~10 threads
```

#### Throughput Comparison

```
Threads (10,000 concurrent connections):
- Memory: 10,000 × 1MB = 10GB RAM just for stacks
- CPU: Constant context switching between threads
- Result: System thrashing, poor performance

Coroutines (10,000 concurrent connections):
- Memory: 10,000 × ~200 bytes = ~2MB for continuations
- CPU: Event-driven, work-stealing dispatchers
- Result: Smooth performance on modest hardware
```

---

### 5.2 Coroutine Internals - Deep Dive

#### 1. Continuation Passing Style (CPS) Transformation

When Kotlin compiler sees a `suspend` function, it transforms it using **Continuation Passing Style**:

**Original Source Code:**

```kotlin
suspend fun fetchAndProcess(userId: String): Result {
    val user = fetchUser(userId)      // Suspension Point 0
    val orders = fetchOrders(user.id) // Suspension Point 1
    return processOrders(orders)      // Suspension Point 2
}
```

**Compiler-Generated State Machine:**

```kotlin
// Simplified conceptual transformation
fun fetchAndProcess(userId: String, continuation: Continuation<Result>): Any {
    
    // The continuation tracks execution state
    class FetchAndProcessContinuation(
        val userId: String,
        val continuation: Continuation<Result>
    ) : Continuation<Any?> {
        
        var state: Int = 0
        var user: User? = null  // Local variable preservation
        var orders: Orders? = null
        
        override fun resumeWith(result: Result<Any?>) {
            when (state) {
                0 -> {
                    // After fetchUser completes
                    user = result.getOrThrow() as User
                    state = 1
                    // Continue to next suspension point
                    fetchAndProcess(userId, this)
                }
                1 -> {
                    // After fetchOrders completes
                    orders = result.getOrThrow() as Orders
                    state = 2
                    fetchAndProcess(userId, this)
                }
                2 -> {
                    // Completion
                    continuation.resume(processOrders(orders!!))
                }
            }
        }
    }
    
    // Resume or start
    val cont = continuation as? FetchAndProcessContinuation 
        ?: FetchAndProcessContinuation(userId, continuation)
    
    return when (cont.state) {
        0 -> fetchUser(userId, cont)
        1 -> fetchOrders(cont.user!!.id, cont)
        2 -> {
            cont.state = 3
            processOrders(cont.orders!!)
        }
        else -> throw IllegalStateException()
    }
}
```

#### 2. The COROUTINE_SUSPENDED Mechanism

```kotlin
// This sentinel value indicates "I'm suspending, will resume later"
val COROUTINE_SUSPENDED: Any = Any()

// Suspend function contract:
// Returns COROUTINE_SUSPENDED if it suspended
// Returns actual result immediately if no suspension occurred

suspend fun <T> suspendCancellableCoroutine(
    block: (CancellableContinuation<T>) -> Unit
): T {
    val continuation = CancellableContinuationImpl(...)
    block(continuation)
    
    return if (continuation.isCompleted) {
        continuation.getResult()  // Completed synchronously
    } else {
        COROUTINE_SUSPENDED         // Will resume asynchronously
    }
}
```

#### 3. How Suspension Actually Works

**Step-by-Step Execution Flow:**

```kotlin
launch(Dispatchers.Main) {
    println("Before suspension")     // Runs on Main thread
    val result = networkCall()      // Suspend function
    println("After suspension: $result")  // May resume on different thread!
}
```

**Execution Timeline:**

```
Time ─────────────────────────────────────────►

Main Thread:  [println][networkCall.start]───►[Other Work]───►[resume]───►[println]
                                              │                              ▲
                                              │                              │
                                              ▼                              │
Network Thread:                                [Actual HTTP Request]────────────┘
                                              (Blocking operation happens here)

Coroutine State: RUNNING → SUSPENDED → RUNNING
```

**Detailed Mechanism:**

1. **Initial Call**: Coroutine starts executing on a thread
2. **Encounter suspend function**: Function returns `COROUTINE_SUSPENDED`
3. **Save State**: Local variables and continuation stored in heap object
4. **Release Thread**: Current thread returns to dispatcher pool
5. **Async Operation**: Network/database operation runs on appropriate thread
6. **Completion Callback**: When operation completes, schedules continuation
7. **Resume**: Dispatcher assigns continuation to available thread
8. **Restore State**: Local variables restored, execution continues

#### 4. The Dispatcher Thread Pool Architecture

```kotlin
// Dispatchers.Default - Work Stealing Dispatcher
// ┌─────────────────────────────────────────────────────────┐
// │  Thread Pool (CPU cores count)                          │
// │  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐ │
// │  │ T1  │  │ T2  │  │ T3  │  │ T4  │  │ T5  │  │ T6  │ │ (8 cores = 8 threads)
// │  └──┬──┘  └──┬──┘  └──┬──┘  └──┬──┘  └──┬──┘  └──┬──┘ │
// │     │        │        │        │        │        │     │
// │     └────────┴────────┴────────┴────────┴────────┘     │
// │                    Work Queue (Shared)                   │
// │  ┌─────────────────────────────────────────────────┐     │
// │  │ Continuation1 │ Continuation2 │ Continuation3   │     │
// │  └─────────────────────────────────────────────────┘     │
// └─────────────────────────────────────────────────────────┘
//
// Work Stealing: Idle threads steal work from busy threads' local queues

// Dispatchers.IO - Cached Thread Pool
// ┌─────────────────────────────────────────────────────────┐
// │  Elastic Thread Pool (starts at 0, max 64 or more)       │
// │                                                         │
// │  Threads created on-demand for blocking operations      │
// │  Idle threads terminated after 60 seconds               │
// │                                                         │
// │  Optimal for: Network, Database, File I/O               │
// │  (These threads spend most time WAITING, not computing)   │
// └─────────────────────────────────────────────────────────┘
```

#### 5. Continuation Implementation Details

```kotlin
// Actual Kotlin compiler-generated code (simplified)

// Interface every suspend function receives
interface Continuation<in T> {
    val context: CoroutineContext
    fun resumeWith(result: Result<T>)
}

// DispatchedContinuation wraps a continuation for thread dispatching
class DispatchedContinuation<T>(
    val dispatcher: CoroutineDispatcher,
    val continuation: Continuation<T>
) : Continuation<T> {
    
    override fun resumeWith(result: Result<T>) {
        // Schedule resume on the dispatcher's thread
        dispatcher.dispatch(context, Runnable {
            continuation.resumeWith(result)
        })
    }
}

// Actual state machine implementation uses labels
suspend fun example(value: Int): String {
    // Compiler generates:
    // var label = 0
    // var result: Any?
    
    // while (true) {
    //     when (label) {
    //         0 -> { /* initial state */ }
    //         1 -> { /* after first suspension */ }
    //         // ...
    //     }
    // }
}
```

#### 6. Call Stack vs Continuation Stack

**Traditional Thread Call Stack:**

```
Thread Stack (grows downward)
┌─────────────────────────┐
│ suspend fun example()   │ ← SP (growing)
│   local vars            │
├─────────────────────────┤
│ suspend fun fetchUser() │
│   local vars            │
├─────────────────────────┤
│ retrofit.enqueue()      │
├─────────────────────────┤
│ okhttp.Call.execute()   │
├─────────────────────────┤
│ native socket read()    │ ← 1MB stack consumed
└─────────────────────────┘
```

**Coroutine Continuation Chain (Heap Allocated):**

```
Heap Objects (minimal allocation)
┌─────────────────────────┐
│ Continuation: example() │
│   label = 2             │
│   local1 = "value"      │
│   next = ───────────────┼──┐
└─────────────────────────┘    │
                               ▼
┌─────────────────────────┐
│ Continuation: fetchUser() │
│   label = 1             │
│   userId = "123"        │
│   next = ───────────────┼──┐
└─────────────────────────┘    │
                               ▼
┌─────────────────────────┐
│ Continuation: launch    │
│   job = JobImpl         │
│   next = null           │ ← Root
└─────────────────────────┘

Total: ~300 bytes on heap vs 1MB+ stack
```

---

### 5.3 Memory Layout and GC Impact

#### Thread Memory Footprint

```kotlin
// Each thread on 64-bit JVM:
// - Thread object: ~240 bytes
// - ThreadLocalMap: ~100 bytes minimum
// - Stack: 1MB (default) = 1,048,576 bytes
// - OS kernel thread structures: ~16KB
// Total per thread: ~1.1MB

// 10,000 threads = 11GB RAM (impossible on most systems)
```

#### Coroutine Memory Footprint

```kotlin
// Each suspended coroutine:
// - Continuation object: ~100-200 bytes
// - Local variables captured: varies
// - Job object: ~100 bytes
// Total per suspended coroutine: ~300-500 bytes typical

// 10,000 suspended coroutines = ~4MB RAM (trivial)

// Even 1,000,000 coroutines = ~400MB (manageable)
```

#### Garbage Collection Benefits

```
Threads and GC:
- Every thread stack is a GC root
- GC must scan 1MB × threadCount constantly
- Long GC pauses with many threads

Coroutines and GC:
- Continuations are regular heap objects
- Collected like any other object
- Young generation collection handles most
- Minimal GC impact
```

---

### 5.4 Event Loop and Dispatcher Internals

#### How Dispatchers.Main Works (Android)

```kotlin
// Looper-based event loop integration
object HandlerDispatcher : CoroutineDispatcher() {
    private val handler = Handler(Looper.getMainLooper())
    
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        // Post to Android's main looper message queue
        handler.post(block)
    }
    
    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        // Optimization: don't dispatch if already on main thread
        return Looper.myLooper() != Looper.getMainLooper()
    }
}

// When coroutine resumes:
// 1. Native network callback arrives on background thread
// 2. Continuation.resumeWith(result) called
// 3. DispatchedContinuation sees Main dispatcher
// 4. Posts Runnable to Handler
// 5. Looper dequeues message, runs on main thread
// 6. State machine continues execution
```

#### Work-Stealing Algorithm (Dispatchers.Default)

```kotlin
// ForkJoinPool-inspired work distribution

class CoroutineScheduler : Executor {
    private val workers = Array(cpuCount) { Worker() }
    private val globalQueue = GlobalQueue()
    
    fun dispatch(task: Task) {
        // Try to submit to local worker queue first
        val worker = currentWorker()
        if (worker != null && worker.submitLocal(task)) {
            return
        }
        // Fall back to global queue
        globalQueue.add(task)
    }
    
    inner class Worker : Thread() {
        private val localQueue = ArrayDeque<Task>()
        
        fun run() {
            while (isActive) {
                val task = localQueue.poll() 
                    ?: stealWork() 
                    ?: globalQueue.poll()
                    ?: park()
                
                task.run()
            }
        }
        
        fun stealWork(): Task? {
            // Randomly try stealing from other workers' queues
            repeat(3) {
                val victim = workers.random()
                val stolen = victim.localQueue.removeLastOrNull()
                if (stolen != null) return stolen
            }
            return null
        }
    }
}
```

---

### 5.5 Exception Handling and Cancellation Propagation

#### Why Both Coroutines Fail When Using async/await

**The Core Problem: Regular Job vs SupervisorJob**

When you use `coroutineScope { }` or `launch { }`, coroutines are created with a regular `Job`. In a regular Job hierarchy, exceptions propagate bidirectionally:

```
Exception Propagation Flow:
┌─────────────────────────────────────────────────────────────┐
│  Child 1 fails with Exception                               │
│       │                                                     │
│       ▼ (exception propagates UP)                         │
│  Parent Job enters "Cancelling" state                       │
│       │                                                     │
│       ▼ (cancellation propagates DOWN)                      │
│  Parent cancels all other children (Child 2, Child 3...)      │
│       │                                                     │
│       ▼                                                     │
│  Parent completes exceptionally                              │
│       │                                                     │
│       ▼                                                     │
│  Exception thrown to coroutineScope caller                  │
└─────────────────────────────────────────────────────────────┘
```

**Detailed Example with async/await:**

```kotlin
// SCENARIO: Both fail because of regular Job hierarchy
suspend fun fetchUsers(): Pair<User, User> = coroutineScope {
    // Parent Job created by coroutineScope
    
    val deferred1 = async {        // Child Job 1
        println("Child 1 starting")
        delay(100)
        throw IOException("Network error")  // FAILS HERE
    }
    
    val deferred2 = async {        // Child Job 2
        println("Child 2 starting")
        delay(500)                          // Still running when Child 1 fails
        fetchUser("2")                      // This never completes!
    }
    
    // What happens:
    // 1. Child 1 throws IOException
    // 2. Exception propagates to Parent Job
    // 3. Parent immediately cancels Child 2 (it gets CancellationException)
    // 4. Parent fails with the original exception
    // 5. coroutineScope re-throws IOException
    
    try {
        val user1 = deferred1.await()  // Throws IOException
        val user2 = deferred2.await()    // Also throws (Child 2 was cancelled)
        user1 to user2
    } catch (e: IOException) {
        // Catches the IOException from Child 1
        // But Child 2 was ALREADY cancelled - we lost its result
        throw e
    }
}
```

**The Job State Machine:**

```kotlin
// Job lifecycle states:
// New → Active → (Completing | Cancelling) → Completed
//                    │           │
//                    ▼           ▼
//              (waits for     (cancels all
//               children)      children)

class JobImpl : Job {
    private val children = CopyOnWriteArrayList<ChildJob>()
    private val state = AtomicReference<State>(State.New)
    
    // When a child fails:
    fun childFailed(child: ChildJob, exception: Throwable) {
        // Only the first exception matters for parent failure
        if (state.compareAndSet(State.Active, State.Cancelling)) {
            // Store the failure cause
            this.exception = exception
            
            // CRITICAL: Cancel ALL other children
            children.filter { it !== child }.forEach { sibling ->
                sibling.cancel(CancellationException("Parent cancelled"))
            }
        }
    }
    
    fun cancel(cause: CancellationException) {
        if (state.compareAndSet(State.Active, State.Cancelling)) {
            // Propagate to all children
            children.forEach { it.cancel(cause) }
        }
    }
    
    fun onChildCompleted(child: ChildJob) {
        children.remove(child)
        // If all children done and we're cancelling, complete exceptionally
        if (children.isEmpty() && state.get() == State.Cancelling) {
            state.set(State.Completed)
            continuation.resumeWithException(exception!!)
        }
    }
}
```

**Solutions to Handle Independent Failures:**

```kotlin
// SOLUTION 1: Use supervisorScope (creates SupervisorJob)
// SupervisorJob treats child failures independently

suspend fun fetchUsersIndependent(): Pair<User?, User?> = supervisorScope {
    // Parent uses SupervisorJob instead of regular Job
    
    val deferred1 = async {
        try {
            fetchUser("1")  // May fail
        } catch (e: Exception) {
            null  // Handle failure locally
        }
    }
    
    val deferred2 = async {
        try {
            fetchUser("2")  // Runs to completion even if deferred1 fails
        } catch (e: Exception) {
            null
        }
    }
    
    // Both await() calls return independently
    deferred1.await() to deferred2.await()
}

// How SupervisorJob differs:
class SupervisorJobImpl : Job {
    override fun childFailed(child: ChildJob, exception: Throwable) {
        // KEY DIFFERENCE: Don't cancel siblings!
        // Just mark this child as failed, keep parent Active
        child.handleFailure(exception)
        // Parent stays Active, other children continue running
    }
}

// SOLUTION 2: Wrap exception handling INSIDE async
// This prevents exception from reaching parent

suspend fun fetchUsersWrapped(): Pair<User?, User?> = coroutineScope {
    val deferred1 = async {
        runCatching { fetchUser("1") }.getOrNull()  // Catches inside
    }
    
    val deferred2 = async {
        runCatching { fetchUser("2") }.getOrNull()  // Independent
    }
    
    deferred1.await() to deferred2.await()
}

// SOLUTION 3: Use CoroutineExceptionHandler for "fire and forget"
// Only works with launch, not async (async exceptions go to await())

val handler = CoroutineExceptionHandler { _, exception ->
    logError(exception)
}

supervisorScope {
    launch(handler) { 
        taskThatMightFail()  // Exception handled by handler, not propagated
    }
    launch(handler) { 
        anotherTask()      // Continues even if first fails
    }
}
```

**Visual Comparison: Job vs SupervisorJob:**

```
Regular Job (coroutineScope):
┌─────────────────────────────────────────────────────────┐
│ Parent Job (Active)                                      │
│     ├── Child 1: async { throw Ex() }                   │
│     │       │                                            │
│     │       ▼                                            │
│     │   Exception! ──► Parent enters Cancelling         │
│     │       │                                            │
│     │       ▼                                            │
│     └── Child 2: async { ... }  ◄── Cancelled!        │
│                  (gets CancellationException)              │
│                                                           │
│ Result: Both fail, exception thrown                      │
└─────────────────────────────────────────────────────────┘

SupervisorJob (supervisorScope):
┌─────────────────────────────────────────────────────────┐
│ Parent Job (Active) ───────── stays Active ───────────►    │
│     ├── Child 1: async { throw Ex() }                   │
│     │       │                                            │
│     │       ▼                                            │
│     │   Exception! ──► Child 1 fails independently      │
│     │       │                                            │
│     │       X (does NOT affect siblings)                 │
│     └── Child 2: async { ... }  ◄── Continues!          │
│                  (completes normally)                    │
│                                                           │
│ Result: Child 1 fails, Child 2 succeeds                  │
└─────────────────────────────────────────────────────────┘
```

**Key Takeaways:**


| Aspect             | Regular Job                | SupervisorJob                        |
| ------------------ | -------------------------- | ------------------------------------ |
| Created by         | `coroutineScope`, `launch` | `supervisorScope`, `SupervisorJob()` |
| Child failure      | Cancels all siblings       | Affects only failing child           |
| Use case           | Atomic operations          | Independent parallel tasks           |
| async/await        | Both await() calls throw   | Each await() independent             |
| Exception handling | Catch at scope level       | Catch per child or use runCatching   |


**When to Use Which:**

```kotlin
// Use coroutineScope (regular Job) when:
// - All tasks must succeed or the operation fails
// - You want automatic cleanup of all tasks on any failure
coroutineScope {
    val user = async { fetchUser() }      // All must succeed
    val orders = async { fetchOrders() }  // for transaction
    process(user.await(), orders.await())
}

// Use supervisorScope when:
// - Tasks are independent
// - Partial success is acceptable
// - You want individual error handling
supervisorScope {
    val settings = async { fetchSettings() }  // Optional
    val profile = async { fetchProfile() }    // Critical
    
    display(
        settings = settings.await(),  // May be null
        profile = profile.await()      // Required
    )
}
```

---

### 5.6 Select Expression (Experimental)

Choose between multiple suspending operations:

```kotlin
select<Unit> {
    channelA.onReceive { value ->
        println("From A: $value")
    }
    channelB.onReceive { value ->
        println("From B: $value")
    }
    onTimeout(1000) {
        println("Timeout")
    }
}
```

### 5.7 Exception Handling in Android Architecture

#### Android Exception Handling Strategy Overview

In Android apps, exception handling spans multiple layers. Each layer has different responsibilities:

```
┌─────────────────────────────────────────────────────────────┐
│                    EXCEPTION FLOW IN ANDROID                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   UI Layer (Fragment/Activity)                               │
│   ├─ Observe StateFlow (no exceptions here)                  │
│   ├─ Handle one-time events via SharedFlow/Channel           │
│   └─ Show Snackbar/Toast/Dialog                              │
│          ↑                                                   │
│   ViewModel Layer                                            │
│   ├─ Catch exceptions from repository                        │
│   ├─ Transform to UI state (Error vs Loading vs Success)       │
│   └─ Expose via StateFlow/SharedFlow                         │
│          ↑                                                   │
│   Repository/UseCase Layer                                   │
│   ├─ Catch data layer exceptions                             │
│   ├─ Transform to domain errors                                │
│   └─ Retry logic, caching                                    │
│          ↑                                                   │
│   Data Layer (Retrofit/Room/Remote)                          │
│   ├─ IOException, HttpException, SQLiteException               │
│   └─ Raw exceptions from libraries                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

#### 1. ViewModel Exception Handling

**Pattern A: Exception to UI State (Recommended for Screens)**

```kotlin
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // UI State sealed class
    sealed class UiState {
        object Loading : UiState()
        data class Success(val user: User) : UiState()
        data class Error(val message: String, val retry: () -> Unit) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val user = userRepository.getUser(userId)
                _uiState.value = UiState.Success(user)
            } catch (e: IOException) {
                // Network error - no internet
                _uiState.value = UiState.Error(
                    message = "No internet connection",
                    retry = { loadUser(userId) }
                )
            } catch (e: HttpException) {
                // HTTP error - 404, 500, etc.
                val message = when (e.code()) {
                    404 -> "User not found"
                    401 -> "Session expired. Please login again"
                    500 -> "Server error. Please try later"
                    else -> "Something went wrong"
                }
                _uiState.value = UiState.Error(message) { loadUser(userId) }
            } catch (e: Exception) {
                // Unexpected error
                _uiState.value = UiState.Error(
                    message = "Unexpected error: ${e.message}",
                    retry = { loadUser(userId) }
                )
            }
        }
    }
}
```

**Pattern B: Global CoroutineExceptionHandler (Last Resort)**

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: Repository,
    private val crashReporter: CrashReporter
) : ViewModel() {

    // Handle uncaught exceptions in viewModelScope
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        // This only catches exceptions not handled by try-catch
        // Useful for "fire and forget" operations
        crashReporter.report(throwable)
        
        // Update error state
        _uiState.value = UiState.Error("Unexpected error occurred")
    }

    fun performBackgroundTask() {
        // Exceptions in this launch go to exceptionHandler
        viewModelScope.launch(exceptionHandler) {
            repository.backgroundOperation()
        }
    }
}
```

#### 2. One-Time Events vs State (Error Events)

**Problem**: StateFlow keeps last value - error would persist across rotations

```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // STATE: Survives rotation, holds current data
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // EVENTS: One-time, consumed once (Snackbar, Toast, Navigation)
    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val result = authRepository.login(email, password)
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                _events.emit(LoginEvent.ShowSuccess("Welcome back!"))
                _events.emit(LoginEvent.NavigateToHome)
            } catch (e: InvalidCredentialsException) {
                // Update state for inline error (survives rotation)
                _uiState.update { 
                    it.copy(isLoading = false, error = "Invalid credentials") 
                }
            } catch (e: IOException) {
                // One-time event for Snackbar (not persisted)
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(LoginEvent.ShowError("No internet connection"))
            }
        }
    }
}

// In Fragment/Activity
class LoginFragment : Fragment() {
    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Collect state (updates UI, survives rotation)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.isVisible = state.isLoading
                binding.errorText.text = state.error
                binding.errorText.isVisible = state.error != null
            }
        }
        
        // Collect events (one-time actions)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is LoginEvent.ShowError -> {
                        Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
                    }
                    is LoginEvent.ShowSuccess -> {
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }
                    is LoginEvent.NavigateToHome -> {
                        findNavController().navigate(R.id.action_login_to_home)
                    }
                }
            }
        }
    }
}
```

#### 3. Repository Layer Exception Handling

```kotlin
class UserRepository @Inject constructor(
    private val api: UserApi,
    private val dao: UserDao,
    private val connectivityManager: ConnectivityManager
) {
    // Wrap low-level exceptions in domain exceptions
    suspend fun getUser(userId: String): User {
        // Check connectivity first
        if (!connectivityManager.isNetworkAvailable()) {
            throw NoConnectivityException()
        }
        
        return try {
            // Try network first
            val response = api.getUser(userId)
            
            if (response.isSuccessful) {
                response.body()?.also { user ->
                    // Cache to database
                    dao.insertUser(user)
                } ?: throw EmptyResponseException()
            } else {
                throw HttpException(response)
            }
        } catch (e: IOException) {
            // Network failure - try cache
            dao.getUser(userId)?.let { cachedUser ->
                return cachedUser // Return stale data
            } ?: throw NoConnectivityException("No network and no cached data")
        }
    }

    // Retry pattern with exponential backoff
    suspend fun getUserWithRetry(userId: String, retries: Int = 3): User {
        var lastException: Exception? = null
        
        repeat(retries) { attempt ->
            try {
                return getUser(userId)
            } catch (e: IOException) {
                lastException = e
                if (attempt < retries - 1) {
                    delay(1000L * (attempt + 1)) // 1s, 2s, 3s
                }
            }
        }
        
        throw lastException ?: Exception("Unknown error")
    }
}

// Domain-specific exceptions
class NoConnectivityException : IOException("No internet connection")
class EmptyResponseException : Exception("Server returned empty response")
class SessionExpiredException : Exception("Session expired")
```

#### 4. Global Exception Handling in Application

```kotlin
class MyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        setupGlobalExceptionHandling()
    }
    
    private fun setupGlobalExceptionHandling() {
        // Handler for uncaught exceptions in any coroutine
        val handler = CoroutineExceptionHandler { context, throwable ->
            // Log to Firebase/Crashlytics
            FirebaseCrashlytics.getInstance().recordException(throwable)
            
            // Log details
            Log.e("GlobalCoroutineError", "Error in $context", throwable)
            
            // Optionally show global error notification
            // or restart critical components
        }
        
        // Install into main scope if needed
        // Note: This is rarely needed as ViewModels should handle their own
    }
}
```

#### 5. Activity/Fragment Exception Handling

```kotlin
class MainActivity : AppCompatActivity() {
    
    // Safe collection that respects lifecycle
    private fun <T> Flow<T>.collectWithLifecycle(
        action: suspend (T) -> Unit
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect { action(it) }
            }
        }
    }
    
    // Safe launch with error handling
    private fun safeLaunch(
        errorHandler: (Exception) -> Unit = { /* default */ },
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return lifecycleScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                // Normal cancellation - don't handle
                throw e
            } catch (e: Exception) {
                errorHandler(e)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Example usage
        safeLaunch(
            errorHandler = { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            // Some UI operation that might fail
            val result = withContext(Dispatchers.IO) {
                riskyOperation()
            }
            updateUI(result)
        }
    }
}
```

#### 6. Common Android Exception Handling Patterns

**Pattern: Result Wrapper (Alternative to try-catch)**

```kotlin
// Domain result type
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// Extension for safe calling
suspend fun <T> safeCall(
    emitLoading: suspend () -> Unit = {},
    block: suspend () -> T
): Result<T> {
    emitLoading()
    return try {
        Result.Success(block())
    } catch (e: CancellationException) {
        throw e // Re-throw cancellation
    } catch (e: Exception) {
        Result.Error(e)
    }
}

// Usage in ViewModel
fun loadData() {
    viewModelScope.launch {
        safeCall(
            emitLoading = { _uiState.value = Result.Loading }
        ) {
            repository.fetchData()
        }.let { result ->
            _uiState.value = result
        }
    }
}
```

**Pattern: Flow Error Handling**

```kotlin
class NewsRepository @Inject constructor(private val api: NewsApi) {
    
    fun getNewsStream(): Flow<List<News>> = flow {
        // Emit loading state
        emit(emptyList())
        
        // Fetch from network
        val news = api.getLatestNews()
        emit(news)
    }
    .retry(3) { cause ->
        // Retry on IOException only
        if (cause is IOException) {
            delay(1000)
            true // Retry
        } else {
            false // Don't retry other errors
        }
    }
    .catch { e ->
        // Emit empty list on error (graceful degradation)
        emit(emptyList())
    }
    .flowOn(Dispatchers.IO)
}

// In ViewModel
val newsFlow: StateFlow<List<News>> = repository.getNewsStream()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
```

#### 7. Testing Exception Handling

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: UserViewModel
    private lateinit var repository: FakeUserRepository
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeUserRepository()
        viewModel = UserViewModel(repository)
    }
    
    @Test
    fun `loadUser emits error state on network failure`() = runTest {
        // Given
        repository.setShouldThrow(IOException("Network error"))
        
        // When
        viewModel.loadUser("123")
        advanceUntilIdle()
        
        // Then
        val states = viewModel.uiState.value
        assertTrue(states is UiState.Error)
        assertEquals("No internet connection", (states as UiState.Error).message)
    }
    
    @Test
    fun `loadUser emits error state on 404`() = runTest {
        // Given
        val errorResponse = Response.error<User>(
            404,
            "Not found".toResponseBody()
        )
        repository.setResponse(errorResponse)
        
        // When
        viewModel.loadUser("123")
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.uiState.value is UiState.Error)
    }
}
```

---

### 5.8 Channels (Communication between Coroutines)

```kotlin
// Buffered channel
val channel = Channel<Int>(capacity = 10)

// Producer-Consumer pattern
val producer = GlobalScope.launch {
    for (i in 1..5) {
        channel.send(i * i)  // Suspends if buffer full
    }
    channel.close()
}

val consumer = GlobalScope.launch {
    for (value in channel) {  // Receives until closed
        println(value)
    }
}
```

---

## Phase 6: Advanced Optimizations

### 6.1 Performance Best Practices

#### 1. Use `withContext` for dispatcher switching, not `async/await`

```kotlin
// Bad - creates unnecessary coroutine
suspend fun parseData(data: String): ParsedData = 
    async(Dispatchers.Default) { heavyParsing(data) }.await()

// Good - suspends and resumes efficiently
suspend fun parseData(data: String): ParsedData = 
    withContext(Dispatchers.Default) { heavyParsing(data) }
```

#### 2. Prefer `Dispatchers.Default` for CPU work, `IO` for blocking

```kotlin
// Default uses thread pool size = CPU cores
// IO uses 64 threads (or more) for blocking operations

// CPU-intensive: sorting, parsing, calculations
withContext(Dispatchers.Default) { processImages() }

// Blocking: network, database, files  
withContext(Dispatchers.IO) { readFromDatabase() }
```

#### 3. Avoid unnecessary dispatcher switching

```kotlin
// Bad - multiple switches
withContext(Dispatchers.IO) {
    val data = readFile()
    withContext(Dispatchers.Default) {
        val processed = process(data)
        withContext(Dispatchers.IO) {
            writeFile(processed)
        }
    }
}

// Better - batch work per dispatcher
val data = withContext(Dispatchers.IO) { readFile() }
val processed = withContext(Dispatchers.Default) { process(data) }
withContext(Dispatchers.IO) { writeFile(processed) }
```

#### 4. Use `SupervisorJob` for independent child failures

```kotlin
// Use when children should fail independently
val scope = CoroutineScope(SupervisorJob())

scope.launch {
    launch { 
        throw Exception("Child 1 fails")
    }
    launch {
        delay(100)
        println("Child 2 still runs")  // With SupervisorJob, this prints
    }
}
```

### 6.2 Memory Optimizations

#### 1. Use `buffer()` for backpressure handling

```kotlin
flow {
    emit(1)
    emit(2)  // Suspends until previous collect finishes
}
.collect { 
    delay(100)  // Slow consumer blocks producer
}

// Better - add buffer
flow { ... }
.buffer(10)     // Producer can emit up to 10 ahead
.collect { ... }
```

#### 2. Use `conflate()` for latest-value scenarios

```kotlin
// UI updates - only care about latest value
locationUpdates
    .conflate()  // Skip intermediate values if collector slow
    .collect { updateMap(it) }
```

#### 3. Careful with `shareIn` and `stateIn` scope

```kotlin
// Bad - never completes, leaks memory
val sharedFlow = sourceFlow.shareIn(GlobalScope, SharingStarted.Eagerly)

// Good - scoped to ViewModel lifecycle
val sharedFlow = sourceFlow.shareIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),  // Stop when no collectors
    replay = 1
)
```

### 6.3 Cancellation Optimization

```kotlin
// Check cancellation cooperatively
suspend fun longRunningTask() {
    for (i in 0..1000) {
        ensureActive()  // Check if cancelled, throw CancellationException
        doWork(i)
    }
}

// Or use yield() to give other coroutines a chance
suspend fun longRunningTask() {
    for (i in 0..1000) {
        doWork(i)
        yield()  // Check cancellation + dispatch
    }
}

// Non-cancellable blocks (cleanup)
suspend fun cleanup() {
    withContext(NonCancellable) {
        closeResources()
    }
}
```

### 6.4 Testing Coroutines

```kotlin
// Use TestDispatcher for instant execution
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }
    
    @Test
    fun testLoadingState() = runTest(testDispatcher) {
        val viewModel = MyViewModel()
        viewModel.loadData()
        
        assertEquals(Loading, viewModel.state.value)
        advanceTimeBy(1000)  // Fast-forward delay
        assertEquals(Success, viewModel.state.value)
    }
}
```

---

## Phase 7: Common Pitfalls


| Pitfall                           | Solution                                                |
| --------------------------------- | ------------------------------------------------------- |
| **Blocking in coroutines**        | Use `Dispatchers.IO` or make the call suspendable       |
| **Memory leaks with GlobalScope** | Always use lifecycle-bound scopes                       |
| **Exception swallowed**           | Use `CoroutineExceptionHandler` or `try-catch` properly |
| **Losing flow emissions**         | Use `SharingStarted.WhileSubscribed()`                  |
| **Race conditions**               | Use `Mutex` or `Actor` for shared state                 |
| **Blocking main thread**          | Never call `runBlocking` on main in production          |


---

## Recommended Learning Path

1. **Week 1**: Complete [Kotlin Coroutines Codelab](https://developer.android.com/courses/pathways/android-coroutines)
2. **Week 2**: Practice with `suspend`, `launch`, `async`, `withContext`
3. **Week 3**: Master Flow with codelabs and small projects
4. **Week 4**: Study internals via [Roman Elizarov's blog](https://blog.jetbrains.com/kotlin/)
5. **Ongoing**: Read [Coroutines Design Document (KEEP)](https://github.com/Kotlin/KEEP/blob/master/proposals/coroutines.md)

---

## Quick Reference Card

### Coroutine Builders

- `launch` - Fire and forget, returns `Job`
- `async` - Returns `Deferred<T>` for result
- `runBlocking` - Blocks thread, bridges sync/async

### Key Functions

- `delay(ms)` - Non-blocking sleep
- `withContext(dispatcher)` - Switch context
- `coroutineScope { }` - Create scope for structured concurrency
- `supervisorScope { }` - Children fail independently

### Flow Operators

- `.map { }` - Transform
- `.filter { }` - Filter emissions
- `.catch { }` - Handle errors
- `.flowOn(dispatcher)` - Change upstream dispatcher
- `.buffer(n)` - Add backpressure buffer
- `.debounce(ms)` - Debounce emissions

### Dispatchers

- `Dispatchers.Main` - Android UI thread
- `Dispatchers.IO` - Network/database (64 threads)
- `Dispatchers.Default` - CPU work (CPU cores count)

---

*Happy learning! Practice with real projects to solidify these concepts.*