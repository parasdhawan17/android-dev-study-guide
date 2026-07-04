# Thread Safety And Synchronization

DAY 8-14: Thread Safety and Synchronization

Goal: Understand race conditions, shared mutable state, and how Android/Kotlin avoids them.

## 1. Race Conditions

A race condition happens when correctness depends on timing between threads/coroutines.

Example: counter++ is not atomic.
It is roughly:
1. read counter
2. add 1
3. write counter
Two threads can both read 0 and both write 1.

````kotlin
class UnsafeCounter {
    var count = 0
    fun increment() {
        count++ // not atomic
    }
}
````

## 2. Synchronized

synchronized uses a monitor lock. Only one thread can enter the block at a time.
Good for small critical sections in thread-based code.
Avoid blocking main thread.

````kotlin
class SynchronizedCounter {
    private var count = 0

    @Synchronized
    fun increment() {
        count++
    }

    @Synchronized
    fun value(): Int = count
}
````

## 3. Atomic Types

AtomicInteger/AtomicReference provide lock-free atomic operations.
Good for simple counters, flags, and compare-and-set state transitions.

````kotlin
class AtomicCounter {
    private val count = AtomicInteger(0)
    fun increment(): Int = count.incrementAndGet()
    fun value(): Int = count.get()
}
````

## 4. Mutex For Coroutines

Mutex is coroutine-friendly.
It suspends instead of blocking the underlying thread.
Use Mutex when protecting shared mutable state inside coroutines.

````kotlin
class TokenStore {
    private val mutex = Mutex()
    private var cachedToken: String? = null

    suspend fun getToken(api: AuthApi): String = mutex.withLock {
        cachedToken ?: api.fetchToken().also { cachedToken = it }
    }
}
````

## 5. Synchronized vs Mutex

| Aspect | `synchronized` / `@Synchronized` | `Mutex` |
|---|---|---|
| Blocking model | Blocks the entire OS thread | Suspends the coroutine, the thread is freed for other work |
| Coroutine friendly | No, can pin a dispatcher thread and cause thread starvation | Yes, designed for coroutines |
| Reentrant | Yes, the same thread can re-enter the same lock | No, a single coroutine cannot re-acquire a `Mutex` it already holds |
| API style | `synchronized(lock) { }` or `@Synchronized` on JVM | `mutex.withLock { }` from `kotlinx.coroutines.sync` |
| Scope | Good for Java-style thread code and blocking critical sections | Good for protecting shared mutable state inside coroutines |
| Cancellation | Can not be cleanly cancelled while waiting | Can be integrated with coroutine cancellation and structured concurrency |

Which is better? It depends on the code you are writing.

- Use `Mutex` when you are inside Kotlin coroutines, especially when the critical section calls other `suspend` functions or does I/O. It keeps the thread pool alive and avoids blocking the main or dispatcher threads.
- Use `synchronized` only for legacy thread-based code, short JVM-only critical sections, or when you do not use coroutines at all.

In modern Android code written with coroutines, prefer `Mutex`.

## 6. Semaphore

A Semaphore is a synchronization primitive that controls access to a shared resource by
maintaining a fixed number of **permits**. A coroutine must acquire a permit before entering the
critical section, and releases it when done. Only N coroutines can hold a permit at the same time.

Think of it like a parking lot with a fixed number of spaces. Each car (coroutine) needs a space
(permit) to enter. When the lot is full, new cars wait until another car leaves and frees a space.

### How it differs from Mutex

| | `Mutex` | `Semaphore` |
|---|---|---|
| Permits | 1 | N (configurable) |
| Purpose | Only one coroutine at a time | Up to N coroutines at a time |
| Typical use | Protect shared mutable state | Limit concurrency, throttle, rate limit |

Analogy: a mutex is a single-lane road; a semaphore is a multi-lane road with a fixed number of lanes.

### Use cases

- Limit parallel uploads to a fixed number.
- Avoid overloading an API or database.
- Protect scarce resources like connection pools or background workers.

### Example

````kotlin
class ImageUploader(private val api: UploadApi) {
    private val semaphore = Semaphore(permits = 3)

    suspend fun uploadAll(images: List<Image>): List<UploadResult> = coroutineScope {
        images.map { image ->
            async {
                semaphore.withPermit {
                    api.upload(image)
                }
            }
        }.awaitAll()
    }
}
````

In this example, even if you start 100 uploads, only 3 run in parallel at any moment. The rest wait
until a permit is released. This prevents the upload API from being flooded.

## 7. Channels And Actor Pattern

### Channels

A `Channel` is a coroutine-based queue that supports suspending `send` and `receive`. It is the
coroutine equivalent of a `BlockingQueue`, but it suspends instead of blocking the thread. Channels
are useful for passing data between coroutines and for building pipelines.

Common channel types:

| Type | Behavior |
|---|---|
| Rendezvous (`Channel()` default) | Sender suspends until receiver is ready, and vice versa. No buffer. |
| Buffered (`Channel(capacity = N)`) | Holds up to N values before senders start suspending. |
| Conflated (`Channel.CONFLATED`) | Keeps only the latest value, overwriting older unread values. |
| Unlimited (`Channel.UNLIMITED`) | Buffers all values without ever suspending the sender. |

### Actor Pattern

The actor pattern means one coroutine owns a piece of mutable state and processes messages
sequentially. Instead of many coroutines locking shared data, they send commands to the actor. The
actor reads commands from a channel and updates the state in a single sequential loop. This avoids
locks because only the actor touches the state.

### Why use an actor?

- No explicit locks are needed. Single-owner access removes the race condition.
- State mutations are serialized by the channel.
- It works well when multiple coroutines need to read and write the same state.
- It can return results to callers using a `CompletableDeferred` or `Channel`.

### Example

````kotlin
sealed class CacheCommand {
    data class Put(val key: String, val value: String) : CacheCommand()
    data class Get(val key: String, val reply: CompletableDeferred<String?>) : CacheCommand()
}

class CacheActor(scope: CoroutineScope) {
    private val commands = scope.actor<CacheCommand> {
        val cache = mutableMapOf<String, String>()
        for (command in channel) {
            when (command) {
                is CacheCommand.Put -> cache[command.key] = command.value
                is CacheCommand.Get -> command.reply.complete(cache[command.key])
            }
        }
    }

    suspend fun put(key: String, value: String) {
        commands.send(CacheCommand.Put(key, value))
    }

    suspend fun get(key: String): String? {
        val reply = CompletableDeferred<String?>()
        commands.send(CacheCommand.Get(key, reply))
        return reply.await()
    }
}
````

### Modern alternatives

The `actor` builder is marked as experimental in `kotlinx.coroutines` and is not widely used in
production Android code. For state that needs to be observed from the UI, prefer a `StateFlow` with a
single owner. For one-time events, use a `SharedFlow` or a plain `Channel` consumed by a single
coroutine. The core idea remains the same: confine mutable state to one coroutine and expose it
through an immutable observable stream.

## 8. Deadlocks

Deadlock happens when two tasks wait forever for each other.
Classic pattern:
- Thread A holds Lock 1 and waits for Lock 2.
- Thread B holds Lock 2 and waits for Lock 1.

Prevention:
- Acquire locks in consistent order.
- Keep critical sections small.
- Prefer structured concurrency and single-owner state.

## Interview Questions

Q: What is thread safety?
A: Code is thread-safe if it remains correct when accessed concurrently by multiple threads.

Q: Mutex vs synchronized?
A: `synchronized` is a JVM monitor lock that blocks the whole OS thread. `Mutex` is a coroutine
primitive that suspends the coroutine and frees the thread for other work. `synchronized` is
reentrant, while a standard Kotlin `Mutex` is not. Use `Mutex` in coroutine-based Android code
because it avoids blocking dispatchers and works cleanly with cancellation. Use `synchronized`
only for legacy thread-based code or short blocking JVM critical sections.

Q: How can you avoid shared mutable state?
A: Use immutable data, StateFlow updates, actors/single owner, repositories as state boundaries,
or confine state to one dispatcher/thread.

Stubs

````kotlin
data class Image(val bytes: ByteArray)
data class UploadResult(val url: String)
interface AuthApi { suspend fun fetchToken(): String }
interface UploadApi { suspend fun upload(image: Image): UploadResult }
````
