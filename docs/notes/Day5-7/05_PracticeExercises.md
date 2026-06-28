# Practice Exercises

DAY 5-7: Practice Exercises

Complete these exercises to solidify understanding of:
- Compose lifecycle
- Process death handling
- Background execution
- Compose vs View interoperability

## Exercise 1: Compose Lifecycle - Lifecycle-Aware Network Observer

TASK: Create a composable that observes network connectivity
and automatically retries failed API calls when connection restores.

Requirements:
1. Register network callback using DisposableEffect
2. Track online/offline state
3. When coming back online, automatically retry failed request
4. Properly unregister callback on disposal
5. Handle configuration changes correctly

TODO: Implement this composable

````kotlin
@Composable
fun AutoRetryContent(
    viewModel: AutoRetryViewModel
) {
    // Hints:
    // - Use DisposableEffect to listen to connectivity changes
    // - Use ConnectivityManager.registerDefaultNetworkCallback()
    // - Call viewModel.onNetworkAvailable() when connection restored
    // - Properly unregister in onDispose
}

class AutoRetryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AutoRetryUiState())
    val uiState: StateFlow<AutoRetryUiState> = _uiState.asStateFlow()

    fun onNetworkAvailable() {
        // TODO: Retry failed request if there was one
    }

    fun loadData() {
        // TODO: Load data, handle failure, track failure state
    }
}

data class AutoRetryUiState(
    val data: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOnline: Boolean = true,
    val hasFailedRequest: Boolean = false
)
````

## Exercise 2: Process Death - Search Screen With State Restoration

TASK: Implement a search screen that survives process death completely.

Requirements:
1. Search query must survive process death (SavedStateHandle)
2. Selected filter must survive process death
3. Scroll position must survive process death (rememberSaveable)
4. After process death, results should be re-fetched (not saved)
5. Loading state should reset after process death

BONUS: Save the expanded/collapsed state of filter chips

Data class (already Parcelable for SavedStateHandle)

````kotlin
@Parcelize
data class SearchFilter(
    val category: String,
    val sortOrder: SortOrder,
    val priceRange: PriceRange
) : Parcelable

enum class SortOrder { RELEVANCE, PRICE_LOW_HIGH, PRICE_HIGH_LOW, NEWEST }
@Parcelize
data class PriceRange(val min: Int, val max: Int) : Parcelable
````

TODO: Implement ViewModel with SavedStateHandle

````kotlin
class ProcessSafeSearchViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: SearchRepository
) : ViewModel() {

    // TODO:
    // 1. Use SavedStateHandle for searchQuery, selectedFilter
    // 2. Use StateFlow for UI state
    // 3. Re-fetch results on init if searchQuery exists after process death
    // 4. Don't save search results to SavedStateHandle (too large)
}

@Composable
fun ProcessSafeSearchScreen(
    viewModel: ProcessSafeSearchViewModel
) {
    // TODO:
    // 1. Use rememberSaveable for LazyListState (scroll position)
    // 2. Collect StateFlow from ViewModel
    // 3. Handle loading, success, error states
    // 4. Debounce search input (500ms)
}

class SearchRepository {
    suspend fun search(query: String, filter: SearchFilter): List<SearchResult> {
        // Simulate network delay
        kotlinx.coroutines.delay(1000)
        return emptyList()
    }
}

data class SearchResult(val id: String, val title: String, val price: Double)
````

## Exercise 3: Background Execution - File Upload With Workmanager

TASK: Implement a robust file upload system using WorkManager.

Requirements:
1. Upload work should only run on WiFi (user preference)
2. Show notification during upload (progress)
3. Retry up to 3 times with exponential backoff
4. Chain multiple uploads: validate → compress → upload → notify
5. Observe upload progress from UI
6. Handle "unique work" to prevent duplicate uploads

TODO: Implement Worker classes

````kotlin
class ValidateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // TODO: Validate file exists and is valid format
        return Result.success()
    }
}

class CompressWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // TODO: Compress image/video if needed
        // Return compressed file path as output
        return Result.success()
    }
}

class UploadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // TODO:
        // 1. Set foreground info with notification showing progress
        // 2. Upload file with progress updates (setProgress())
        // 3. Return success/failure/retry appropriately
        return Result.success()
    }
}

class UploadCompleteWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // TODO: Show "upload complete" notification
        return Result.success()
    }
}
````

TODO: Implement upload scheduling

````kotlin
class UploadManager(private val context: Context) {

    fun scheduleUpload(fileUri: String, requireWiFi: Boolean = true) {
        // TODO:
        // 1. Create work request with constraints (WiFi if required)
        // 2. Set up work chain: validate → compress → upload → notify
        // 3. Use unique work to prevent duplicates (based on file URI)
        // 4. Configure retry policy (exponential backoff)
    }

    fun observeUpload(fileUri: String): Flow<UploadStatus> {
        // TODO: Return flow that emits upload progress/status
        return kotlinx.coroutines.flow.emptyFlow()
    }

    fun cancelUpload(fileUri: String) {
        // TODO: Cancel work by tag
    }
}

sealed class UploadStatus {
    object Pending : UploadStatus()
    data class Progress(val percent: Int) : UploadStatus()
    object Success : UploadStatus()
    data class Failed(val error: String) : UploadStatus()
}
````

## Exercise 4: Compose vs View - Hybrid Migration

TASK: Migrate a legacy View-based screen to Compose gradually.

Scenario: You have an existing Fragment with a RecyclerView showing a list
of items. You want to migrate the item cards to Compose while keeping the
RecyclerView for performance.

Requirements:
1. Keep RecyclerView for list container
2. Migrate item layout to ComposeView
3. Handle click events from Compose back to Fragment
4. Properly dispose ComposeViews in ViewHolder recycling

Legacy Fragment (don't modify - simulate existing code)

````kotlin
class LegacyListFragment : androidx.fragment.app.Fragment() {
    private lateinit var recyclerView: RecyclerView
    private val items = mutableListOf<Item>()

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_list, container, false)
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.adapter = LegacyAdapter(items)
        return view
    }

    fun onItemClicked(item: Item, action: ItemAction) {
        // Handle click from item
    }
}
````

TODO: Implement hybrid adapter

````kotlin
class HybridAdapter(
    private val items: List<Item>,
    private val onItemAction: (Item, ItemAction) -> Unit
) : RecyclerView.Adapter<HybridViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HybridViewHolder {
        // TODO: Create ViewHolder with ComposeView
        return TODO()
    }

    override fun onBindViewHolder(holder: HybridViewHolder, position: Int) {
        // TODO: Set Compose content for this item
    }

    override fun getItemCount() = items.size

    override fun onViewRecycled(holder: HybridViewHolder) {
        // TODO: Properly dispose ComposeView
        super.onViewRecycled(holder)
    }
}

class HybridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    // TODO: Reference to ComposeView and dispose function
}

@Composable
fun ItemCard(
    item: Item,
    onAction: (ItemAction) -> Unit
) {
    // TODO: Implement item card in Compose
    // Include: image, title, subtitle, action buttons
}

data class Item(
    val id: String,
    val imageUrl: String,
    val title: String,
    val subtitle: String
)

enum class ItemAction { LIKE, SHARE, DELETE, CLICK }
````

## Exercise 5: Advanced - Lifecycle-Aware Polling With Proper Cleanup

TASK: Implement a polling mechanism that fetches data every 30 seconds
but only when the app is in foreground AND has network connectivity.

Requirements:
1. Poll every 30 seconds
2. Stop polling when app backgrounded (Lifecycle awareness)
3. Stop polling when offline, resume when online
4. Cancel in-flight request when condition changes
5. Handle configuration changes without restarting polling
6. Use proper effect handlers (DisposableEffect, LaunchedEffect)

````kotlin
@Composable
fun SmartPollingScreen(
    viewModel: SmartPollingViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // TODO: Implement smart polling composable
    // Hints:
    // - Use DisposableEffect for network callback
    // - Use LaunchedEffect with lifecycle and connectivity as keys
    // - Use coroutine scope that's lifecycle-aware
    // - Cancel and restart polling when conditions change
}

class SmartPollingViewModel : ViewModel() {
    private val _data = MutableStateFlow<List<DataItem>>(emptyList())
    val data: StateFlow<List<DataItem>> = _data.asStateFlow()

    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

    suspend fun fetchData(): Result<List<DataItem>> {
        // Simulate API call
        kotlinx.coroutines.delay(500)
        return Result.success(emptyList())
    }
}

data class DataItem(val id: String, val value: String)
````

## Exercise 6: Testing Challenge - Test Process Death Handling

TASK: Write tests for process death handling.

Test 1: ViewModel state survives process death simulation
Test 2: UI restores correctly after activity recreation
Test 3: Background work continues after process restart

Test skeleton (requires Robolectric or instrumented test)

```kotlin
@RunWith(AndroidJUnit4::class)
class ProcessDeathTest {

@get:Rule
val instantExecutorRule = InstantTaskExecutorRule()

@Test
fun viewModelState_survivesProcessDeath() {
// TODO:
// 1. Create SavedStateHandle with initial state
// 2. Create ViewModel, modify state
// 3. Simulate process death (clear and restore)
// 4. Verify state restored correctly
}

@Test
fun activity_recreate_restoresScrollPosition() {
// TODO:
// 1. Launch activity
// 2. Scroll to position, enter search query
// 3. Call scenario.recreate()
// 4. Verify scroll position and query restored
}
}
```

WorkManager test skeleton

```kotlin
@RunWith(AndroidJUnit4::class)
class UploadWorkerTest {

@Test
fun uploadWorker_success_updatesProgress() {
// TODO:
// 1. Create TestListenableWorkerBuilder
// 2. Mock repository
// 3. Run worker
// 4. Verify progress updates and final success
}

@Test
fun uploadWorker_networkFailure_retries() {
// TODO:
// 1. Setup worker with mock that throws network exception
// 2. Run worker
// 3. Verify Result.retry() returned
}
}
```

## Solution Templates (Uncomment To See Approach)

```kotlin
// EXERCISE 1 SOLUTION - AutoRetryContent:

@Composable
fun AutoRetryContent(viewModel: AutoRetryViewModel) {
val context = LocalContext.current
val uiState by viewModel.uiState.collectAsState()

DisposableEffect(Unit) {
val connectivityManager = context.getSystemService(
Context.CONNECTIVITY_SERVICE
) as ConnectivityManager

val callback = object : ConnectivityManager.NetworkCallback() {
override fun onAvailable(network: Network) {
viewModel.onNetworkAvailable()
}
}

connectivityManager.registerDefaultNetworkCallback(callback)

onDispose {
connectivityManager.unregisterNetworkCallback(callback)
}
}

// UI based on state
when {
uiState.isLoading -&gt; CircularProgressIndicator()
uiState.error != null &amp;&amp; !uiState.isOnline -&gt; {
Text("Offline. Will retry when connected.")
}
uiState.error != null -&gt; Text("Error: ${uiState.error}")
uiState.data != null -&gt; Text(uiState.data)
}
}

class AutoRetryViewModel : ViewModel() {
private val _uiState = MutableStateFlow(AutoRetryUiState())
val uiState: StateFlow&lt;AutoRetryUiState&gt; = _uiState.asStateFlow()

fun onNetworkAvailable() {
if (_uiState.value.hasFailedRequest) {
loadData()
}
_uiState.value = _uiState.value.copy(isOnline = true)
}

fun loadData() {
viewModelScope.launch {
_uiState.value = _uiState.value.copy(isLoading = true, error = null)
try {
val data = fetchFromNetwork()
_uiState.value = AutoRetryUiState(
data = data,
isOnline = true
)
} catch (e: Exception) {
_uiState.value = _uiState.value.copy(
isLoading = false,
error = e.message,
hasFailedRequest = true
)
}
}
}

private suspend fun fetchFromNetwork(): String {
delay(1000)
// Simulate occasional failure
if (Random.nextBoolean()) throw Exception("Network error")
return "Data loaded!"
}
}

// EXERCISE 2 SOLUTION - ProcessSafeSearchViewModel:

class ProcessSafeSearchViewModel(
private val savedStateHandle: SavedStateHandle,
private val repository: SearchRepository
) : ViewModel() {

companion object {
private const val KEY_QUERY = "search_query"
private const val KEY_FILTER = "search_filter"
private const val KEY_CHIPS_EXPANDED = "chips_expanded"
}

var searchQuery: String
get() = savedStateHandle.get&lt;String&gt;(KEY_QUERY) ?: ""
set(value) {
savedStateHandle[KEY_QUERY] = value
if (value.isNotEmpty()) performSearch()
}

var selectedFilter: SearchFilter
get() = savedStateHandle.get&lt;SearchFilter&gt;(KEY_FILTER)
?: SearchFilter("all", SortOrder.RELEVANCE, PriceRange(0, Int.MAX_VALUE))
set(value) {
savedStateHandle[KEY_FILTER] = value
performSearch()
}

var chipsExpanded: Boolean
get() = savedStateHandle.get&lt;Boolean&gt;(KEY_CHIPS_EXPANDED) ?: false
set(value) { savedStateHandle[KEY_CHIPS_EXPANDED] = value }

private val _results = MutableStateFlow&lt;List&lt;SearchResult&gt;&gt;(emptyList())
val results: StateFlow&lt;List&lt;SearchResult&gt;&gt; = _results.asStateFlow()

private val _isLoading = MutableStateFlow(false)
val isLoading: StateFlow&lt;Boolean&gt; = _isLoading.asStateFlow()

init {
// Re-fetch after process death
if (searchQuery.isNotEmpty()) {
performSearch()
}
}

private fun performSearch() {
viewModelScope.launch {
_isLoading.value = true
_results.value = repository.search(searchQuery, selectedFilter)
_isLoading.value = false
}
}
}

@Composable
fun ProcessSafeSearchScreen(viewModel: ProcessSafeSearchViewModel) {
val query = viewModel.searchQuery
val filter = viewModel.selectedFilter
val results by viewModel.results.collectAsState()
val isLoading by viewModel.isLoading.collectAsState()
val chipsExpanded = viewModel.chipsExpanded

// Use rememberSaveable for LazyListState (scroll position)
val listState = rememberSaveable(saver = LazyListState.Saver) {
LazyListState()
}

Column {
TextField(
value = query,
onValueChange = { viewModel.searchQuery = it },
label = { Text("Search") }
)

// Filter chips with expansion state
FilterChips(
filter = filter,
expanded = chipsExpanded,
onExpandedChange = { viewModel.chipsExpanded = it },
onFilterChange = { viewModel.selectedFilter = it }
)

if (isLoading) {
CircularProgressIndicator()
} else {
LazyColumn(state = listState) {
items(results) { result -&gt;
SearchResultItem(result)
}
}
}
}
}

// EXERCISE 3 SOLUTION - UploadManager:

class UploadManager(private val context: Context) {

fun scheduleUpload(fileUri: String, requireWiFi: Boolean = true) {
val constraints = Constraints.Builder().apply {
if (requireWiFi) {
setRequiredNetworkType(NetworkType.UNMETERED)
} else {
setRequiredNetworkType(NetworkType.CONNECTED)
}
}.build()

val validateWork = OneTimeWorkRequestBuilder&lt;ValidateWorker&gt;()
.setInputData(workDataOf("file_uri" to fileUri))
.build()

val compressWork = OneTimeWorkRequestBuilder&lt;CompressWorker&gt;()
.build()

val uploadWork = OneTimeWorkRequestBuilder&lt;UploadWorker&gt;()
.setConstraints(constraints)
.setBackoffCriteria(
BackoffPolicy.EXPONENTIAL,
WorkRequest.MIN_BACKOFF_MILLIS,
TimeUnit.MILLISECONDS
)
.setInputMerger(ArrayCreatingInputMerger::class)
.addTag("upload_$fileUri")
.build()

val notifyWork = OneTimeWorkRequestBuilder&lt;UploadCompleteWorker&gt;()
.addTag("upload_$fileUri")
.build()

WorkManager.getInstance(context)
.beginUniqueWork(
"upload_$fileUri",
ExistingWorkPolicy.KEEP,
validateWork
)
.then(compressWork)
.then(uploadWork)
.then(notifyWork)
.enqueue()
}

fun observeUpload(fileUri: String): Flow&lt;UploadStatus&gt; {
return WorkManager.getInstance(context)
.getWorkInfosByTagFlow("upload_$fileUri")
.map { workInfos -&gt;
val uploadWork = workInfos.find { it.tags.contains("UploadWorker") }
when (uploadWork?.state) {
WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -&gt; UploadStatus.Pending
WorkInfo.State.RUNNING -&gt; {
val progress = uploadWork.progress.getInt("progress", 0)
UploadStatus.Progress(progress)
}
WorkInfo.State.SUCCEEDED -&gt; UploadStatus.Success
WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -&gt;
UploadStatus.Failed(uploadWork.outputData.getString("error") ?: "Unknown")
null -&gt; UploadStatus.Pending
}
}
}
}

```

Stub classes for compilation

````kotlin
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ArrayCreatingInputMerger
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

annotation class Parcelize

interface Parcelable

annotation class Inject

class RecyclerView(context: Context) {
    var adapter: RecyclerView.Adapter<*>? = null
}

abstract class Adapter<VH : RecyclerView.ViewHolder> {
    abstract fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH
    abstract fun onBindViewHolder(holder: VH, position: Int)
    abstract fun getItemCount(): Int
    open fun onViewRecycled(holder: VH) {}
}

open class ViewHolder(itemView: View)

class ViewGroup
class View
class LayoutInflater
class R {
    object layout {
        const val fragment_list = 1
    }

    object id {
        const val recycler_view = 1
    }
}

fun <T> MutableStateFlow(value: T): MutableStateFlow<T> {
    TODO()
}

fun <T> StateFlow<T>.collectAsState(): androidx.compose.runtime.State<T> {
    TODO()
}

val androidx.compose.runtime.State<*>.value: Any? get() = null

@Composable
fun CircularProgressIndicator() {}

@Composable
fun Text(text: String) {}

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit
) {
}

@Composable
fun FilterChips(
    filter: SearchFilter,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onFilterChange: (SearchFilter) -> Unit
) {
}

@Composable
fun SearchResultItem(result: SearchResult) {}

@Composable
fun LazyColumn(
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit
) {
}

@Composable
fun rememberLazyListState(): LazyListState {
    TODO()
}

class LazyListState {
    companion object {
        val Saver: androidx.compose.runtime.saveable.Saver<LazyListState, *> = TODO()
    }
}

interface LazyListScope {
    fun items(items: List<Any>, itemContent: @Composable (Any) -> Unit)
}

fun rememberSaveable(saver: Any? = null, init: () -> LazyListState): LazyListState {
    TODO()
}

fun Column(content: @Composable () -> Unit) {}

annotation classComposable
annotation classComposable
````
