# Coroutines Fundamentals

DAY 8-14: Coroutines Fundamentals

Goal: Understand coroutines as structured, cancellable units of asynchronous work.
Interview angle: Explain not just syntax, but why coroutines are safer than unmanaged threads.

## 1. What Is A Coroutine?

A coroutine is a lightweight task that can suspend without blocking its thread.

Thread blocking:
Thread.sleep(1000) -&gt; OS thread is unusable for 1 second.

Coroutine suspension:
delay(1000) -&gt; coroutine pauses, thread is returned to the dispatcher.

Key idea:
- A coroutine is not a thread.
- Many coroutines can share a small pool of threads.
- Suspension points allow work to pause and resume later.

````kotlin
suspend fun loadUserProfile(userId: String): UserProfile {
    val user = fetchUser(userId)          // suspension point
    val settings = fetchSettings(userId)  // suspension point
    return UserProfile(user, settings)
}
````

## 2. Structured Concurrency

Structured concurrency means child coroutines are tied to a parent scope.

Why it matters:
- Parent waits for children before completing.
- Cancelling parent cancels children.
- Exceptions propagate predictably.
- No leaked background work after screen is gone.

Android example:
viewModelScope.launch { ... }
The work is automatically cancelled when ViewModel.onCleared() runs.

````kotlin
class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {
    fun loadProfile(userId: String) {
        viewModelScope.launch {
            val profile = repository.getProfile(userId)
            // Update UI state here.
        }
    }
}
````

## 3. Launch vs Async

launch:
- Fire-and-forget coroutine.
- Returns Job.
- Use for updating state, logging, navigation side effects.

async:
- Produces a result.
- Returns Deferred&lt;T&gt;.
- Call await() to get the value or exception.

Interview rule:
Use async only when you genuinely need a result, usually for parallel decomposition.

````kotlin
suspend fun loadDashboard(repository: DashboardRepository): Dashboard {
    return coroutineScope {
        val userDeferred = async { repository.getUser() }
        val cardsDeferred = async { repository.getCards() }
        val notificationsDeferred = async { repository.getNotifications() }

        Dashboard(
            user = userDeferred.await(),
            cards = cardsDeferred.await(),
            notifications = notificationsDeferred.await()
        )
    }
}
````

## 4. Running Coroutines Sequentially

Sequential execution means the second coroutine starts only after the first one completes. In Kotlin coroutines, the simplest way is to call two `suspend` functions one after another inside the same coroutine.

````kotlin
suspend fun loadUserThenOrders(repository: OrderRepository): UserOrders {
    val user = repository.getUser()          // completes first
    val orders = repository.getOrders(user.id) // starts only after getUser returns
    return UserOrders(user, orders)
}
````

When using `async`, call `await()` on the first `Deferred` before starting the second `async`. This guarantees the second operation sees the result of the first.

````kotlin
suspend fun loadUserThenOrders(repository: OrderRepository): UserOrders = coroutineScope {
    val user = async { repository.getUser() }.await()             // wait for the user
    val orders = async { repository.getOrders(user.id) }.await()  // starts after user is ready
    UserOrders(user, orders)
}
````

Sequential vs concurrent:

| Style | Code pattern | Result |
|-------|--------------|--------|
| Sequential | `val a = async { ... }.await(); val b = async { ... }.await()` | Second waits for first. |
| Concurrent | `val a = async { ... }; val b = async { ... }; awaitAll(a, b)` | Both run at the same time. |

For two fire-and-forget `launch` blocks that must run in order, put them in the same coroutine and call them one after another.

````kotlin
viewModelScope.launch {
    syncProfile()   // completes first
    syncSettings()  // runs only after syncProfile finishes
}
````

## 5. Dispatchers

Dispatchers decide where coroutine code runs.

Dispatchers.Main:
- Android main thread.
- UI updates, collecting UI state.

Dispatchers.IO:
- Blocking IO: database, files, network APIs that block.
- Backed by elastic pool.

Dispatchers.Default:
- CPU-heavy work: JSON parsing, sorting big lists, image transformations.
- Limited by CPU cores.

Dispatchers.Unconfined:
- Rarely used in app code.
- Starts in caller thread, resumes wherever suspension decides.

````kotlin
suspend fun loadAndParse(repository: Repository): ParsedData = withContext(Dispatchers.IO) {
    val raw = repository.readLargeFile()
    withContext(Dispatchers.Default) {
        parseLargePayload(raw)
    }
}
````

## 6. Cancellation

Coroutine cancellation is cooperative.
A coroutine must reach a suspension point or check cancellation to stop.

Common cancellable suspension points:
- delay()
- withContext()
- Flow collection
- most kotlinx.coroutines APIs

For CPU loops, check isActive or call yield().

````kotlin
suspend fun computeHashes(files: List<FileData>): List<String> = withContext(Dispatchers.Default) {
    val result = mutableListOf<String>()
    for (file in files) {
        ensureActive() // throws CancellationException if cancelled
        result += sha256(file.bytes)
    }
    result
}
````

## 7. Exception Handling

Important rules:
- In launch, uncaught exceptions crash the coroutine and propagate to parent.
- In async, exception is stored until await() is called.
- CancellationException is normal cancellation and should usually not be swallowed.
- SupervisorJob lets siblings fail independently.

````kotlin
class FeedViewModel(private val repository: FeedRepository) : ViewModel() {
    fun loadFeed() {
        viewModelScope.launch {
            try {
                val feed = repository.getFeed()
                // state = Success(feed)
            } catch (e: CancellationException) {
                throw e // never convert cancellation into an error state
            } catch (e: Exception) {
                // state = Error(e.message)
            }
        }
    }
}
````

## 8. Common Android Patterns

Pattern: expose immutable StateFlow from ViewModel

````kotlin
class SearchViewModel(private val repository: SearchRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    fun search(query: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val results = repository.search(query)
                _state.value = SearchUiState(results = results)
            } catch (e: Exception) {
                _state.value = SearchUiState(error = e.message ?: "Unknown error")
            }
        }
    }
}
````

Pattern: switch dispatcher at repository boundary

````kotlin
class UserRepository(private val api: UserApi, private val dao: UserDao) {
    suspend fun refreshUser(id: String): User = withContext(Dispatchers.IO) {
        val user = api.getUser(id)
        dao.save(user)
        user
    }
}
````

## Interview Questions

Q: What is structured concurrency?
A: Child coroutines live inside a parent scope. Parent completion, cancellation, and errors
are coordinated so work does not leak beyond its lifecycle.

Q: launch vs async?
A: launch returns Job and is for side effects. async returns Deferred&lt;T&gt; and is for results.
Use async when combining independent concurrent computations.

Q: Why should CancellationException usually be rethrown?
A: It signals normal cooperative cancellation. Swallowing it can make cancelled work look like
a real failure and can keep parent scopes alive incorrectly.

Q: When should you use withContext(Dispatchers.IO)?
A: Around blocking IO or calls whose implementation blocks threads. For already-suspending
Retrofit calls, it is often unnecessary unless doing additional blocking work.

Q: How do you run two coroutines sequentially?
A: Call the suspend functions in order inside one coroutine, or call `await()` on the first
`async` before starting the second one. Use a single `launch` or `async` block and rely on
coroutine suspension between calls.

Simple stubs for study snippets

````kotlin
data class User(val id: String)
data class Settings(val theme: String)
data class UserProfile(val user: User, val settings: Settings)
data class Dashboard(val user: User, val cards: List<String>, val notifications: List<String>)
data class UserOrders(val user: User, val orders: List<String>)
data class ParsedData(val value: String)
data class FileData(val bytes: ByteArray)
data class SearchUiState(val results: List<String> = emptyList(), val isLoading: Boolean = false, val error: String? = null)
suspend fun fetchUser(id: String) = User(id)
suspend fun fetchSettings(id: String) = Settings("system")
fun parseLargePayload(raw: String) = ParsedData(raw)
fun sha256(bytes: ByteArray) = bytes.contentHashCode().toString()
suspend fun syncProfile() { }
suspend fun syncSettings() { }
interface ProfileRepository { suspend fun getProfile(userId: String): UserProfile }
interface DashboardRepository { suspend fun getUser(): User; suspend fun getCards(): List<String>; suspend fun getNotifications(): List<String> }
interface OrderRepository { suspend fun getUser(): User; suspend fun getOrders(userId: String): List<String> }
interface Repository { fun readLargeFile(): String }
interface FeedRepository { suspend fun getFeed(): List<String> }
interface SearchRepository { suspend fun search(query: String): List<String> }
interface UserApi { suspend fun getUser(id: String): User }
interface UserDao { fun save(user: User) }
open class ViewModel { val viewModelScope = CoroutineScope(Dispatchers.Main) }
````
