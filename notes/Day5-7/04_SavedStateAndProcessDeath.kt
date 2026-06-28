/**
 * DAY 5-7: Process Death, SavedStateHandle, and State Restoration
 *
 * Critical for interview: Understanding what happens during low memory kill
 * and how to properly restore state.
 */

import android.app.Application
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.os.bundleOf
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

// ============ 1. UNDERSTANDING PROCESS DEATH ============

/*
WHAT IS PROCESS DEATH?

Android kills app processes silently when memory is low. Key characteristics:

┌─────────────────────────────────────────────────────────────────────┐
│ SCENARIO: User opens app → switches to camera → takes photos →      │
│ returns to app via Recents                                          │
├─────────────────────────────────────────────────────────────────────┤
│ 1. Your app in foreground                                           │
│      ↓                                                              │
│ 2. User opens camera (your app backgrounded)                          │
│      ↓                                                              │
│ 3. Camera needs memory → System kills your process (SILENTLY!)       │
│      NO onStop(), NO onDestroy(), NO onSaveInstanceState() called   │
│      ↓                                                              │
│ 4. User returns via Recents                                         │
│      ↓                                                              │
│ 5. System recreates your Activity with savedInstanceState Bundle     │
│      onCreate(savedInstanceState) called with non-null bundle      │
└─────────────────────────────────────────────────────────────────────┘

IMPORTANT: onSaveInstanceState() is the ONLY reliable callback before death.
It's called when:
- App goes to background (home button pressed)
- Configuration change (rotation)
- System predicts possible process death

It's NOT called when:
- User force-stops app (swipe away from Recents)
- App crashes
- System kills without warning
*/

// ============ 2. ACTIVITY STATE HANDLING ============

class ProcessAwareActivity : AppCompatActivity() {

    private var userInput: String = ""
    private var scrollPosition: Int = 0
    private var selectedTab: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // RESTORE STATE after process death
        if (savedInstanceState != null) {
            userInput = savedInstanceState.getString("user_input", "")
            scrollPosition = savedInstanceState.getInt("scroll_position", 0)
            selectedTab = savedInstanceState.getInt("selected_tab", 0)

            // Restore UI
            restoreUI()
        }

        // Setup UI
        setupUI()
    }

    // Called BEFORE potential process death
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // SAVE critical UI state
        outState.putString("user_input", userInput)
        outState.putInt("scroll_position", scrollPosition)
        outState.putInt("selected_tab", selectedTab)

        // Only save lightweight data! Bundle size limit ~1MB
    }

    // For complex state, use this (called after onStop)
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Alternative place to restore state
        // Not called if using onCreate restoration
    }

    private fun setupUI() {
        // Setup views
    }

    private fun restoreUI() {
        // Restore view states
    }
}

// ============ 3. SAVEDSTATEHANDLE (MODERN APPROACH) ============

/**
 * SavedStateHandle is the modern way to handle process death in ViewModel.
 * It automatically saves/restores using onSaveInstanceState mechanism.
 */

class ProfileViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: UserRepository
) : ViewModel() {

    // Keys for SavedStateHandle
    companion object {
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val KEY_SELECTED_FILTER = "selected_filter"
        private const val KEY_SCROLL_POSITION = "scroll_position"
    }

    // Simple property delegation - automatically saved/restored
    var searchQuery: String
        get() = savedStateHandle.get<String>(KEY_SEARCH_QUERY) ?: ""
        set(value) {
            savedStateHandle[KEY_SEARCH_QUERY] = value
        }

    var selectedFilter: FilterType
        get() = savedStateHandle.get<FilterType>(KEY_SELECTED_FILTER) ?: FilterType.ALL
        set(value) {
            savedStateHandle[KEY_SELECTED_FILTER] = value
        }

    // StateFlow backed by SavedStateHandle - perfect for Compose
    private val _uiState = MutableStateFlow(
        ProfileUiState(
            searchQuery = searchQuery,
            selectedFilter = selectedFilter
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Alternative: Use SavedStateHandle as StateFlow source
    val savedSearchQuery: StateFlow<String> = savedStateHandle
        .getStateFlow(KEY_SEARCH_QUERY, "")

    val savedFilter: StateFlow<FilterType> = savedStateHandle
        .getStateFlow(KEY_SELECTED_FILTER, FilterType.ALL)

    init {
        // Restore from SavedStateHandle on initialization
        restoreState()
    }

    private fun restoreState() {
        _uiState.value = _uiState.value.copy(
            searchQuery = searchQuery,
            selectedFilter = selectedFilter
        )
    }

    fun updateSearch(query: String) {
        searchQuery = query  // Auto-saved to SavedStateHandle
        _uiState.value = _uiState.value.copy(searchQuery = query)
        performSearch(query)
    }

    fun updateFilter(filter: FilterType) {
        selectedFilter = filter
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val results = repository.search(query, selectedFilter)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = results
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}

// Enum must be Parcelable or Serializable for SavedStateHandle
enum class FilterType { ALL, ACTIVE, INACTIVE }

data class ProfileUiState(
    val searchQuery: String = "",
    val selectedFilter: FilterType = FilterType.ALL,
    val isLoading: Boolean = false,
    val results: List<User> = emptyList(),
    val error: String? = null
)

// Repository stub
class UserRepository {
    suspend fun search(query: String, filter: FilterType): List<User> = emptyList()
}

data class User(val id: String, val name: String)

// ============ 4. VIEWMODEL FACTORY WITH SAVEDSTATEHANDLE ============

// Option 1: Using AbstractSavedStateViewModelFactory
class ProfileViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val repository: UserRepository
) : AbstractSavedStateViewModelFactory(owner, null) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        return ProfileViewModel(handle, repository) as T
    }
}

// Usage in Activity
/*
class ProfileActivity : AppCompatActivity() {
    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(this, UserRepository())
    }
}
*/

// Option 2: Hilt (Recommended) - Automatic SavedStateHandle injection
/*
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: UserRepository
) : ViewModel() {
    // SavedStateHandle available automatically
}
*/

// ============ 5. REMEMBERSAVEABLE IN COMPOSE ============

@Composable
fun SearchScreen() {
    // Survives configuration changes AND process death
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var scrollPosition by rememberSaveable { mutableStateOf(0) }

    // For complex objects, provide custom Saver
    var user by rememberSaveable(stateSaver = UserSaver) {
        mutableStateOf(User("1", "", ""))
    }
}

// Custom Saver for complex objects
data class User(val id: String, val name: String, val email: String)

val UserSaver = Saver<User, Bundle>(
    save = { user ->
        bundleOf(
            "id" to user.id,
            "name" to user.name,
            "email" to user.email
        )
    },
    restore = { bundle ->
        User(
            id = bundle.getString("id") ?: "",
            name = bundle.getString("name") ?: "",
            email = bundle.getString("email") ?: ""
        )
    }
)

// Saver for lists
val UsersListSaver = Saver<List<User>, List<Bundle>>(
    save = { users -> users.map { user ->
        bundleOf("id" to user.id, "name" to user.name, "email" to user.email)
    }},
    restore = { bundles -> bundles.map { bundle ->
        User(
            bundle.getString("id") ?: "",
            bundle.getString("name") ?: "",
            bundle.getString("email") ?: ""
        )
    }}
)

// Alternative: Use @Parcelize for automatic Parcelable implementation
@Parcelize
data class ParcelableUser(
    val id: String,
    val name: String,
    val email: String
) : Parcelable

// Then use parcelableSaver()
/*
var user by rememberSaveable(
    stateSaver = parcelableSaver<ParcelableUser>()
) {
    mutableStateOf(ParcelableUser("", "", ""))
}
*/

// ============ 6. WHAT TO SAVE vs WHAT TO REFETCH ============

/*
┌─────────────────────────────────────────────────────────────────────┐
│                    SAVE TO SAVEDSTATEHANDLE                           │
├─────────────────────────────────────────────────────────────────────┤
│ ✓ User input (form data, search queries)                            │
│ ✓ UI state (selected tab, scroll position, expanded sections)       │
│ ✓ Navigation state (current screen, back stack info)                │
│ ✓ Small data (user ID, item ID being viewed)                          │
│ ✓ Filter/sort preferences                                             │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                  DO NOT SAVE (REFETCH INSTEAD)                      │
├─────────────────────────────────────────────────────────────────────┤
│ ✗ Large lists (re-fetch from repository)                            │
│ ✗ Network responses (re-fetch from cache/network)                   │
│ ✗ Images/Bitmaps (reload from cache/disk)                           │
│ ✗ Complex objects (keep in ViewModel, re-initialize)                │
│ ✗ Non-essential state (can be reset)                                │
└─────────────────────────────────────────────────────────────────────┘

RULE: If data can be reconstructed from IDs or re-fetched from Repository,
      don't save it. Only save minimal state needed to reconstruct UI.
*/

// GOOD: Save minimal state, re-fetch data
class GoodStateManagement(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ItemRepository
) : ViewModel() {

    // Save only what's needed
    var selectedItemId: String?
        get() = savedStateHandle.get<String>("selected_item_id")
        set(value) { savedStateHandle["selected_item_id"] = value }

    // Re-fetch the actual data
    private val _selectedItem = MutableStateFlow<Item?>(null)
    val selectedItem: StateFlow<Item?> = _selectedItem.asStateFlow()

    init {
        // Restore after process death
        selectedItemId?.let { id ->
            viewModelScope.launch {
                _selectedItem.value = repository.getItem(id)
            }
        }
    }
}

// BAD: Saving large objects
class BadStateManagement(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // DON'T DO THIS - can exceed Bundle size limit (~1MB)
    var allItems: List<Item>
        get() = savedStateHandle.get<List<Item>>("items") ?: emptyList()
        set(value) { savedStateHandle["items"] = value }
}

class ItemRepository {
    suspend fun getItem(id: String): Item? = null
}
data class Item(val id: String, val name: String)

// ============ 7. COMPLETE EXAMPLE: PROCESS-SAFE ARCHITECTURE ============

// Architecture: ViewModel with SavedStateHandle + Repository Pattern
@Composable
fun ArticleDetailScreen(
    articleId: String,
    viewModel: ArticleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> LoadingIndicator()
        uiState.error != null -> ErrorMessage(uiState.error)
        uiState.article != null -> ArticleContent(
            article = uiState.article,
            scrollPosition = uiState.scrollPosition,
            onScroll = viewModel::updateScrollPosition
        )
    }
}

class ArticleViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ArticleRepository
) : ViewModel() {

    companion object {
        private const val KEY_ARTICLE_ID = "article_id"
        private const val KEY_SCROLL_POSITION = "scroll_position"
        private const val KEY_IS_BOOKMARKED = "is_bookmarked"
    }

    // Persisted state
    private var articleId: String
        get() = savedStateHandle.get<String>(KEY_ARTICLE_ID) ?: ""
        set(value) { savedStateHandle[KEY_ARTICLE_ID] = value }

    var scrollPosition: Int
        get() = savedStateHandle.get<Int>(KEY_SCROLL_POSITION) ?: 0
        private set(value) { savedStateHandle[KEY_SCROLL_POSITION] = value }

    var isBookmarked: Boolean
        get() = savedStateHandle.get<Boolean>(KEY_IS_BOOKMARKED) ?: false
        private set(value) { savedStateHandle[KEY_IS_BOOKMARKED] = value }

    // Non-persisted (re-fetched)
    private val _uiState = MutableStateFlow(ArticleUiState())
    val uiState: StateFlow<ArticleUiState> = _uiState.asStateFlow()

    fun loadArticle(id: String) {
        articleId = id
        viewModelScope.launch {
            _uiState.value = ArticleUiState(isLoading = true)
            try {
                val article = repository.getArticle(id)
                _uiState.value = ArticleUiState(
                    article = article,
                    scrollPosition = scrollPosition,
                    isBookmarked = isBookmarked
                )
            } catch (e: Exception) {
                _uiState.value = ArticleUiState(error = e.message)
            }
        }
    }

    fun updateScrollPosition(position: Int) {
        scrollPosition = position
        _uiState.value = _uiState.value.copy(scrollPosition = position)
    }

    fun toggleBookmark() {
        isBookmarked = !isBookmarked
        _uiState.value = _uiState.value.copy(isBookmarked = isBookmarked)
        viewModelScope.launch {
            repository.updateBookmark(articleId, isBookmarked)
        }
    }
}

data class ArticleUiState(
    val article: Article? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val scrollPosition: Int = 0,
    val isBookmarked: Boolean = false
)

data class Article(
    val id: String,
    val title: String,
    val content: String
)

class ArticleRepository {
    suspend fun getArticle(id: String): Article? = null
    suspend fun updateBookmark(articleId: String, bookmarked: Boolean) {}
}

@Composable
fun LoadingIndicator() {}
@Composable
fun ErrorMessage(error: String?) {}
@Composable
fun ArticleContent(article: Article?, scrollPosition: Int, onScroll: (Int) -> Unit) {}

// ============ 8. TESTING PROCESS DEATH ============

/*
Manual Testing Process Death:

1. Developer Options → Don't keep activities (enable this)
   - Simulates activity destruction when backgrounded

2. OR use adb:
   adb shell am kill com.your.package
   - Kills process, preserves saved state

3. Test scenario:
   a. Open app, enter data
   b. Background app (home button)
   c. adb shell am kill <package>
   d. Return to app via Recents
   e. Verify state restored correctly

4. Test with strict mode:
   StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
       .detectNonSdkApiUsage()
       .penaltyLog()
       .build())
*/

// Automated test
/*
@RunWith(AndroidJUnit4::class)
class ProcessDeathTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun stateSurvivesProcessDeath() {
        // Enter some data
        onView(withId(R.id.searchInput))
            .perform(typeText("kotlin"))

        // Simulate process death
        activityRule.scenario.recreate()

        // Verify state restored
        onView(withId(R.id.searchInput))
            .check(matches(withText("kotlin")))
    }
}
*/

// ============ 9. COMMON PITFALLS ============

/*
PITFALL 1: Not handling null savedInstanceState
WRONG:
    override fun onCreate(savedInstanceState: Bundle) {
        val value = savedInstanceState.getString("key") // NPE if null!
    }

RIGHT:
    override fun onCreate(savedInstanceState: Bundle?) {
        val value = savedInstanceState?.getString("key") // Safe call
    }

PITFALL 2: Saving large objects
WRONG:
    outState.putSerializable("large_list", hugeList) // Can exceed 1MB

RIGHT:
    outState.putStringArrayList("ids", ArrayList(itemIds)) // Save IDs only

PITFALL 3: Forgetting to call super
WRONG:
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("key", value)
        // Missing super call!
    }

RIGHT:
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState) // Always call first
        outState.putString("key", value)
    }

PITFALL 4: Assuming onSaveInstanceState always called
WRONG:
    fun criticalSave() {
        // Expecting this data to always be saved
    }

RIGHT:
    fun criticalSave() {
        // Persist to disk/database for critical data
        // Use SavedStateHandle only for UI state
    }
*/

// ============ INTERVIEW QUESTIONS ============

/*
Q: What happens during process death? What lifecycle callbacks are called?
A: Process is killed silently WITHOUT onStop, onDestroy, or onSaveInstanceState.
   Only onSaveInstanceState (called earlier when backgrounding) gives chance to save.
   When restored, Activity recreated with savedInstanceState Bundle.

Q: What's the difference between SavedStateHandle and rememberSaveable?
A: SavedStateHandle is for ViewModel, survives config changes + process death.
   rememberSaveable is for Compose, also survives config changes + process death.
   SavedStateHandle is better for business logic state.

Q: Why is there a ~1MB limit on saved state?
A: State is stored in Binder transaction buffer, which has size limits.
   Large data should be persisted to disk (Room, DataStore, file).

Q: What's the difference between ViewModel and onSaveInstanceState?
A: ViewModel survives config changes but NOT process death (unless with SavedStateHandle).
   onSaveInstanceState/SavedStateHandle survives both config changes and process death.

Q: When should you use SavedStateHandle vs regular ViewModel properties?
A: Use SavedStateHandle for UI state that must survive process death (input, selections).
   Use regular properties for transient state or data that can be re-fetched.

Q: How do you test process death handling?
A: Enable "Don't keep activities" in Developer Options, or use adb shell am kill.
   Background app, kill process, return via Recents, verify state restored.
*/

// Stub annotations
annotation class Parcelize
annotation class Inject
annotation class HiltViewModel
annotation class ExperimentalCoroutinesApi
fun <T> MutableStateFlow(value: T): MutableStateFlow<T> { TODO() }
fun <T> MutableStateFlow<T>.asStateFlow(): StateFlow<T> { TODO() }
val StateFlow<*>.value: Any? get() = null
operator fun <T> StateFlow<T>.component1(): T { TODO() }
fun viewModel(): ArticleViewModel { TODO() }
@Composable
fun collectAsState(): androidx.compose.runtime.State<Any> { TODO() }
annotation class SavedStateHandleSaveableApi
annotation classComposable
annotation classComposable
