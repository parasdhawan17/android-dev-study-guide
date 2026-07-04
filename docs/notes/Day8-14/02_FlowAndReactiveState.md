# Flow And Reactive State

DAY 8-14: Flow and Reactive Programming

Goal: Learn Flow as a stream of asynchronous values and understand how it powers UI state.

## 1. Flow Mental Model

A Flow&lt;T&gt; is an asynchronous stream that emits values over time.

Cold Flow:
- Does nothing until collected.
- Each collector runs the producer again.
- Example: flow { emit(api.getUser()) }

Hot Flow:
- Exists independently of collectors.
- Collectors observe the latest/current emissions.
- Examples: StateFlow, SharedFlow.

````kotlin
fun userFlow(userId: String): Flow<User> = flow {
    emit(api.getUser(userId))
}
````

## 2. Stateflow

StateFlow is a hot stream for state.

Properties:
- Always has a current value.
- Replays exactly one latest value to new collectors.
- Great for screen UI state.
- Similar purpose to LiveData, but coroutine-native.

````kotlin
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = LoginUiState(isLoading = true)
            val result = authRepository.login(email, password)
            _state.value = result.fold(
                onSuccess = { LoginUiState(user = it) },
                onFailure = { LoginUiState(error = it.message ?: "Login failed") }
            )
        }
    }
}
````

## 3. LiveData

LiveData is a lifecycle-aware observable data holder from Android Jetpack. It is the older Android equivalent of StateFlow for exposing UI state from a ViewModel.

### Properties

- Lifecycle-aware: only active observers (STARTED or RESUMED) receive updates.
- Auto-cleanup: removes observers when the lifecycle is destroyed.
- Always holds the latest emitted value (like StateFlow).
- Safe to update from the main thread only (use `postValue` from background threads).

### MutableLiveData

Expose `LiveData<T>` publicly while mutating `MutableLiveData<T>` internally.

````kotlin
class ProfileViewModel : ViewModel() {
    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    fun loadUser(id: String) {
        _user.value = User(id) // main thread only
    }
}
````

### Observing

In a Fragment or Activity, observe with a `LifecycleOwner`.

````kotlin
class ProfileFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.user.observe(viewLifecycleOwner) { user ->
            render(user)
        }
    }
}
````

Avoid `observeForever` unless you manually manage the observer lifecycle.

### Transformations

`Transformations.map` derives a new LiveData. `Transformations.switchMap` flattens a LiveData that returns another LiveData.

````kotlin
val userName: LiveData<String> = Transformations.map(user) { it.name }

val userDetails: LiveData<UserDetails> = Transformations.switchMap(userId) { id ->
    repository.getUserDetails(id)
}
````

### LiveData vs StateFlow

| LiveData | StateFlow |
|---|---|
| Lifecycle-aware by default | Needs `repeatOnLifecycle` / `collectAsStateWithLifecycle` |
| Main-thread updates only | Can update from any thread |
| Android-specific | Kotlin coroutines-native |
| Simpler API | Richer operators and Flow ecosystem |
| Built-in lifecycle cleanup | Collector must be scoped correctly |

Prefer StateFlow for new Kotlin-first code. LiveData remains valid in existing codebases and for simple UI state.

### Gotchas

- Updating `_liveData.value` from a background thread throws an exception; use `postValue` instead.
- `Transformations` create new LiveData chains; don't chain excessively.
- `observeForever` leaks if not removed.

## 4. Sharedflow

SharedFlow is a hot stream for events.

Use it for:
- Snackbars
- Navigation commands
- One-time messages

Why not StateFlow for events?
StateFlow replays the latest value on configuration changes, which can repeat navigation or
snackbars accidentally. SharedFlow can be configured with replay = 0.

````kotlin
class PaymentViewModel : ViewModel() {
    private val _events = MutableSharedFlow<PaymentEvent>()
    val events: SharedFlow<PaymentEvent> = _events.asSharedFlow()

    fun onPaymentSuccess() {
        viewModelScope.launch {
            _events.emit(PaymentEvent.NavigateToReceipt)
        }
    }
}

sealed class PaymentEvent {
    object NavigateToReceipt : PaymentEvent()
    data class ShowError(val message: String) : PaymentEvent()
}
````

## 5. Core Operators

map: transform each value.
filter: keep matching values.
combine: latest values from multiple flows.
zip: pair emissions one-by-one.
flatMapLatest: switch to new inner flow, cancel old one.
catch: handle upstream exceptions.
onStart: emit loading state before upstream starts.

````kotlin
fun observeSearchResults(queryFlow: Flow<String>, repository: SearchRepository): Flow<SearchUiState> {
    return queryFlow
        .debounce(300)
        .filter { it.length >= 2 }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            flow {
                val results = repository.search(query)
                emit(SearchUiState(results = results))
            }.onStart {
                emit(SearchUiState(isLoading = true))
            }.catch { e ->
                emit(SearchUiState(error = e.message ?: "Search failed"))
            }
        }
}
````

## 6. Collecting In UI

Android lifecycle matters. Do not collect forever from an Activity/Fragment lifecycle.

Recommended:
- Compose: collectAsStateWithLifecycle()
- Views: repeatOnLifecycle(Lifecycle.State.STARTED)

This prevents work while UI is stopped and avoids leaks.

````kotlin
class FeedFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    render(state)
                }
            }
        }
    }
}
````

## 7. Backpressure And Conflation

If producer is faster than consumer:
- buffer(): allow producer to get ahead up to buffer size.
- conflate(): keep only latest value, skip intermediate values.
- collectLatest(): cancel previous collector block when new value arrives.

UI search often wants collectLatest/flatMapLatest.
Progress rendering might use conflate.

````kotlin
suspend fun renderProgress(progressFlow: Flow<Int>) {
    progressFlow
        .conflate()
        .collect { progress -> renderProgressBar(progress) }
}
````

## 8. Interview Questions

Q: Flow vs LiveData?
A: Flow is coroutine-native, supports rich operators, has cold and hot variants, and is not
lifecycle-aware by default. LiveData is lifecycle-aware by default but less expressive.

Q: StateFlow vs SharedFlow?
A: StateFlow is for state and always has a value. SharedFlow is for events or broadcasts and
can be configured with replay/buffer behavior.

Q: LiveData vs StateFlow?
A: LiveData is lifecycle-aware by default and Android-specific; it must be updated on the main
thread (use `postValue` from background threads). StateFlow is coroutine-native, can be updated
from any thread, and requires lifecycle-aware collection helpers such as `repeatOnLifecycle` or
`collectAsStateWithLifecycle` to avoid wasting resources.

Q: What does flatMapLatest do?
A: It starts a new inner flow for each upstream value and cancels the previous one. Useful for
search because old network calls become irrelevant when the query changes.

Stubs

````kotlin
data class User(val id: String)
data class LoginUiState(val user: User? = null, val isLoading: Boolean = false, val error: String? = null)
data class SearchUiState(val results: List<String> = emptyList(), val isLoading: Boolean = false, val error: String? = null)
interface AuthRepository { suspend fun login(email: String, password: String): Result<User> }
interface SearchRepository { suspend fun search(query: String): List<String> }
object api { suspend fun getUser(id: String) = User(id) }
open class ViewModel { val viewModelScope = CoroutineScope(Dispatchers.Main) }
open class Fragment { lateinit var viewLifecycleOwner: LifecycleOwner; lateinit var viewModel: FeedViewModel; fun render(state: Any) {} }
class View
class Bundle
class LifecycleOwner { val lifecycle = Lifecycle(); val lifecycleScope = CoroutineScope(Dispatchers.Main) }
class Lifecycle { enum class State { STARTED } }
class FeedViewModel { val state: Flow<Any> = flowOf(Unit); val user: LiveData<User> = MutableLiveData() }
data class UserDetails(val info: String = "")
open class LiveData<T> { open var value: T? = null; fun observe(owner: LifecycleOwner, observer: (T) -> Unit) {} }
class MutableLiveData<T> : LiveData<T>() { override var value: T? = null }
object Transformations {
    fun <X, Y> map(source: LiveData<X>, transform: (X) -> Y): LiveData<Y> = MutableLiveData()
    fun <X, Y> switchMap(source: LiveData<X>, transform: (X) -> LiveData<Y>): LiveData<Y> = MutableLiveData()
}
fun renderProgressBar(progress: Int) {}
````
