# Kotlin Flows: Complete Study Guide

## Table of Contents
1. [Cold vs Hot Streams](#cold-vs-hot-streams)
2. [Flow (Cold Stream)](#flow-cold-stream)
3. [StateFlow](#stateflow)
4. [SharedFlow](#sharedflow)
5. [Comparison Summary](#comparison-summary)
6. [Android Patterns](#android-patterns)
7. [Common Operators](#common-operators)
8. [Best Practices](#best-practices)

---

## Cold vs Hot Streams

### Cold Streams (Flow)
- **Lazy**: Only produces values when collected
- **Independent**: Each collector gets its own fresh stream
- **Unicast**: Data produced separately per consumer

```kotlin
val coldFlow = flow {
    println("Flow started")
    emit(1)
    emit(2)
    emit(3)
}

// Each collector triggers a fresh execution
coldFlow.collect { println("A: $it") }  // Prints: Flow started, 1, 2, 3
coldFlow.collect { println("B: $it") }  // Prints: Flow started, 1, 2, 3 (again!)
```

### Hot Streams (StateFlow/SharedFlow)
- **Eager**: Active regardless of collectors
- **Shared**: Multiple collectors share the same stream
- **Multicast**: Late collectors miss prior emissions

```kotlin
val hotFlow = MutableStateFlow(0)
hotFlow.value = 1  // Emits immediately, no collector needed
```

---

## Flow (Cold Stream)

### Creating Flows

```kotlin
// Builder
val myFlow = flow {
    emit(1)
    delay(100)
    emit(2)
}

// From collections
val flowFromList = listOf(1, 2, 3).asFlow()
val flowOfValues = flowOf(1, 2, 3)

// From callback
val callbackFlow = callbackFlow {
    val listener = object : SomeCallback {
        override fun onEvent(value: String) {
            trySend(value)  // synchronous send
        }
    }
    api.register(listener)
    awaitClose { api.unregister(listener) }  // cleanup
}
```

### Collecting Flows

```kotlin
// Basic collection
flow.collect { value ->
    println(value)
}

// With lifecycle (Android)
lifecycleScope.launch {
    viewModel.uiState.collect { state ->
        updateUi(state)
    }
}

// Collect latest (cancels previous if new arrives)
flow.collectLatest { value ->
    // If new value arrives, this coroutine is cancelled
    delay(1000)  // Simulated work
}
```

### Flow Builders

| Builder | Use Case |
|---------|----------|
| `flow { }` | Custom emission logic |
| `flowOf()` | Fixed set of values |
| `asFlow()` | Convert collection to Flow |
| `channelFlow` | Send from multiple coroutines |
| `callbackFlow` | Convert callbacks to Flow |

---

## StateFlow

### Definition
A hot stream that always holds a **single current state value**. New collectors immediately receive the latest value.

### Key Characteristics
- Always has a **current value** (never empty)
- **Initial value required** at creation
- New collectors get **latest value immediately**
- `distinctUntilChanged()` behavior built-in

### Creating StateFlow

```kotlin
// In ViewModel
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// With initial value
private val _count = MutableStateFlow(0)  // starts at 0
val count: StateFlow<Int> = _count.asStateFlow()
```

### Reading/Writing Values

```kotlin
// Read current value (synchronous)
val current = _uiState.value

// Write new value
_uiState.value = UiState(isLoading = true)

// Update based on current
_uiState.update { current ->
    current.copy(isLoading = true)
}
```

### Collection in UI

```kotlin
// Activity/Fragment
lifecycleScope.launch {
    viewModel.uiState
        .flowWithLifecycle(lifecycle)  // lifecycle-aware
        .collect { state ->
            updateUi(state)
        }
}

// Or with repeatOnLifecycle (recommended)
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            updateUi(state)
        }
    }
}
```

### When to Use StateFlow
- UI state that always exists (loading/content/error)
- Form input states
- Settings/preferences
- Any state that should survive configuration changes

---

## SharedFlow

### Definition
A general-purpose hot stream for sharing emissions among multiple collectors. Configurable replay cache.

### Key Characteristics
- **No initial value** (starts empty)
- Configurable **replay cache** (default: 0)
- Buffer and overflow strategies configurable
- Multiple collectors share emissions

### Creating SharedFlow

```kotlin
// No replay (default)
private val _events = MutableSharedFlow<Event>()
val events: SharedFlow<Event> = _events.asSharedFlow()

// Replay last 2 values
private val _messages = MutableSharedFlow<Message>(replay = 2)

// Full configuration
private val _events = MutableSharedFlow<Event>(
    replay = 0,                    // replay last N values
    extraBufferCapacity = 10,      // buffer beyond replay
    onBufferOverflow = BufferOverflow.DROP_OLDEST  // strategy
)
```

### Buffer Overflow Strategies

| Strategy | Behavior |
|----------|----------|
| `SUSPEND` | Suspend emitter until space (default) |
| `DROP_OLDEST` | Drop oldest value in buffer |
| `DROP_LATEST` | Drop the value being emitted |

### Emitting Values

```kotlin
viewModelScope.launch {
    _events.emit(NavigateToDetail)  // fire and forget
}

// Try emit (non-suspending)
val sent = _events.tryEmit(event)  // returns true if successful
```

### Collection in UI

```kotlin
// For one-time events (no replay)
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.events.collect { event ->
        when (event) {
            is ShowToast -> showToast(event.message)
            is Navigate -> navigate(event.direction)
        }
    }
}
```

### When to Use SharedFlow
- One-time events (toasts, navigation, snackbars)
- Broadcasting data to multiple consumers
- Sharing expensive computations
- Event streams where latecomers shouldn't see old events

---

## Comparison Summary

| Feature | Flow (Cold) | StateFlow | SharedFlow |
|---------|-------------|-----------|------------|
| Stream type | Cold | Hot | Hot |
| Initial value | Not applicable | Required | Optional |
| Always has value | No | Yes | No |
| Replay | All values | Latest only | Configurable (0+) |
| Multiple collectors | Independent streams | Shared | Shared |
| `value` property | No | Yes | No |
| Best for | One-shot ops | UI state | Events/sharing |

---

## Android Patterns

### Complete ViewModel Example

```kotlin
class NewsViewModel(private val repository: NewsRepository) : ViewModel() {

    // ===== STATE (StateFlow) =====
    // UI state that always has a value
    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    // ===== EVENTS (SharedFlow) =====
    // One-time events (navigation, toasts)
    private val _events = MutableSharedFlow<NewsEvent>()
    val events: SharedFlow<NewsEvent> = _events.asSharedFlow()

    init {
        loadNews()
    }

    fun loadNews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.getNews()
                .catch { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
                .collect { news ->
                    _uiState.update { it.copy(isLoading = false, news = news) }
                }
        }
    }

    fun onArticleClick(article: Article) {
        viewModelScope.launch {
            _events.emit(NewsEvent.NavigateToArticle(article.id))
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            _events.emit(NewsEvent.ShowToast("Refreshing..."))
            loadNews()
        }
    }
}

// State data class
data class NewsUiState(
    val isLoading: Boolean = false,
    val news: List<Article> = emptyList(),
    val error: String? = null
)

// Events sealed class
sealed class NewsEvent {
    data class NavigateToArticle(val id: String) : NewsEvent()
    data class ShowToast(val message: String) : NewsEvent()
}
```

### Fragment Collection Pattern

```kotlin
class NewsFragment : Fragment() {

    private val viewModel: NewsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Collect state (lifecycle-aware, survives rotation)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.newsRecyclerView.isVisible = !state.isLoading
                    adapter.submitList(state.news)
                }
            }
        }

        // Collect events (consumed once, not re-emitted on rotation)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is NavigateToArticle -> findNavController().navigate(...)
                        is ShowToast -> Toast.makeText(...).show()
                    }
                }
            }
        }
    }
}
```

### Converting Cold to Hot

```kotlin
// Convert cold Flow to StateFlow
val stateFlow = repository.getData()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

// Convert cold Flow to SharedFlow
val sharedFlow = repository.getEvents()
    .shareIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 1
    )
```

### Sharing Strategies

```kotlin
// WhileSubscribed: Active while collectors exist, keeps for 5s after last collector
.started = SharingStarted.WhileSubscribed(5000)

// Eagerly: Starts immediately, never stops
.started = SharingStarted.Eagerly

// Lazily: Starts on first collector, never stops
.started = SharingStarted.Lazily
```

---

## Common Operators

### Transformations

```kotlin
flow.map { it * 2 }              // Transform each value
flow.filter { it > 0 }           // Filter values
flow.flatMapConcat { getData(it) }  // Flatten nested flows
flow.flatMapLatest { search(it) }   // Cancel previous on new value
```

### Combining Flows

```kotlin
// Combine two flows
combine(flow1, flow2) { a, b -> a to b }

// Merge multiple flows
merge(flow1, flow2, flow3)

// Zip (pairs values)
zip(flow1, flow2) { a, b -> Pair(a, b) }
```

### Buffering & Conflation

```kotlin
flow.buffer(10)        // Buffer emissions
flow.conflate()        // Keep only latest on backpressure
flow.collectLatest { } // Cancel previous collection on new value
```

### Error Handling

```kotlin
flow
    .catch { e ->
        emit(defaultValue)  // Recover with default
    }
    .onCompletion { cause ->
        // Runs on completion (success or error)
    }
    .retry(3)              // Retry on error
```

---

## Best Practices

### DO's

1. **Use StateFlow for UI State**
   ```kotlin
   // Good - always has value
   val uiState: StateFlow<UiState>
   ```

2. **Use SharedFlow for Events**
   ```kotlin
   // Good - events consumed once
   val events: SharedFlow<Event>
   ```

3. **Use `asStateFlow()`/`asSharedFlow()` for immutability**
   ```kotlin
   private val _state = MutableStateFlow(State())
   val state: StateFlow<State> = _state.asStateFlow()  // Read-only
   ```

4. **Use `repeatOnLifecycle` for collection**
   ```kotlin
   lifecycleScope.launch {
       repeatOnLifecycle(Lifecycle.State.STARTED) {
           viewModel.state.collect { }
       }
   }
   ```

5. **Use `stateIn`/`shareIn` for sharing streams**
   ```kotlin
   val shared = repository.getData()
       .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
   ```

### DON'Ts

1. **Don't expose Mutable types**
   ```kotlin
   // Bad
   val state: MutableStateFlow<State> = _state

   // Good
   val state: StateFlow<State> = _state.asStateFlow()
   ```

2. **Don't use StateFlow for one-time events**
   ```kotlin
   // Bad - survives rotation, re-triggers
   _state.value = ShowToast

   // Good - consumed once
   _events.emit(ShowToast)
   ```

3. **Don't forget lifecycle awareness**
   ```kotlin
   // Bad - may crash or leak
   lifecycleScope.launch { viewModel.state.collect { } }

   // Good - lifecycle-aware
   repeatOnLifecycle(State.STARTED) { viewModel.state.collect { } }
   ```

4. **Don't update StateFlow from multiple threads without synchronization**
   ```kotlin
   // Risky
   _state.value = newValue  // from background thread

   // Safe - use update
   _state.update { it.copy(...) }
   ```

---

## Quick Reference

### When to Use What

| Scenario | Use |
|----------|-----|
| UI state (loading/content/error) | `StateFlow` |
| Form inputs/settings | `StateFlow` |
| Navigation events | `SharedFlow` (replay=0) |
| Toast/Snackbar messages | `SharedFlow` (replay=0) |
| Network/DB calls | Cold `Flow` |
| Sharing expensive computation | `SharedFlow` or `stateIn` |
| Real-time updates | `SharedFlow` or `StateFlow` |

### Common Signatures

```kotlin
// ViewModel State
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// ViewModel Events
private val _events = MutableSharedFlow<Event>()
val events: SharedFlow<Event> = _events.asSharedFlow()

// Repository
fun getData(): Flow<Data>
suspend fun saveData(data: Data)

// Use case/Interactor
operator fun invoke(): Flow<Result<Data>>
```

---

*Last updated: June 2026*
