/**
 * DAY 8-14: Thread Safety and Synchronization
 *
 * Goal: Understand race conditions, shared mutable state, and how Android/Kotlin avoids them.
 */

// ============ 1. RACE CONDITIONS ============
/*
A race condition happens when correctness depends on timing between threads/coroutines.

Example: counter++ is not atomic.
It is roughly:
1. read counter
2. add 1
3. write counter
Two threads can both read 0 and both write 1.
*/

class UnsafeCounter {
    var count = 0
    fun increment() {
        count++ // not atomic
    }
}

// ============ 2. SYNCHRONIZED ============
/*
synchronized uses a monitor lock. Only one thread can enter the block at a time.
Good for small critical sections in thread-based code.
Avoid blocking main thread.
*/

class SynchronizedCounter {
    private var count = 0

    @Synchronized
    fun increment() {
        count++
    }

    @Synchronized
    fun value(): Int = count
}

// ============ 3. ATOMIC TYPES ============
/*
AtomicInteger/AtomicReference provide lock-free atomic operations.
Good for simple counters, flags, and compare-and-set state transitions.
*/

class AtomicCounter {
    private val count = AtomicInteger(0)
    fun increment(): Int = count.incrementAndGet()
    fun value(): Int = count.get()
}

// ============ 4. MUTEX FOR COROUTINES ============
/*
Mutex is coroutine-friendly.
It suspends instead of blocking the underlying thread.
Use Mutex when protecting shared mutable state inside coroutines.
*/

class TokenStore {
    private val mutex = Mutex()
    private var cachedToken: String? = null

    suspend fun getToken(api: AuthApi): String = mutex.withLock {
        cachedToken ?: api.fetchToken().also { cachedToken = it }
    }
}

// ============ 5. SEMAPHORE ============
/*
Semaphore limits concurrency.
Use cases:
- Limit parallel uploads to 3.
- Avoid overloading API/database.
- Protect scarce resources.
*/

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

// ============ 6. CHANNELS AND ACTOR PATTERN ============
/*
A Channel is a coroutine queue.
Actor pattern means one coroutine owns state and processes messages sequentially.
This avoids locks because only the actor touches the state.
*/

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

// ============ 7. DEADLOCKS ============
/*
Deadlock happens when two tasks wait forever for each other.
Classic pattern:
- Thread A holds Lock 1 and waits for Lock 2.
- Thread B holds Lock 2 and waits for Lock 1.

Prevention:
- Acquire locks in consistent order.
- Keep critical sections small.
- Prefer structured concurrency and single-owner state.
*/

// ============ INTERVIEW QUESTIONS ============
/*
Q: What is thread safety?
A: Code is thread-safe if it remains correct when accessed concurrently by multiple threads.

Q: Mutex vs synchronized?
A: synchronized blocks a thread. Mutex suspends a coroutine, returning the thread to the pool.
   Prefer Mutex inside coroutine code.

Q: How can you avoid shared mutable state?
A: Use immutable data, StateFlow updates, actors/single owner, repositories as state boundaries,
   or confine state to one dispatcher/thread.
*/

// Stubs
data class Image(val bytes: ByteArray)
data class UploadResult(val url: String)
interface AuthApi { suspend fun fetchToken(): String }
interface UploadApi { suspend fun upload(image: Image): UploadResult }
