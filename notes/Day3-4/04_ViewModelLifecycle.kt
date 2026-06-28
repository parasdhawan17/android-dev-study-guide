/**
 * ============================================================================
 * DAY 3-4: ANDROID COMPONENT LIFECYCLES
 * ============================================================================
 * Topic 4: ViewModel Lifecycle and Configuration Changes
 * ============================================================================
 */

/**
 * VIEWMODEL LIFECYCLE
 * ===================
 *
 * ViewModel survives configuration changes (rotation, theme, locale)
 * but is destroyed when Activity/Fragment is finished
 *
 * Lifecycle Scope:
 * - Created: First ViewModelProvider.get() call
 * - Cleared: Activity.onDestroy() or Fragment.onDestroy() (when truly finishing)
 *
 *                           Activity Created
 *                               ↓
 *                           ViewModel Created
 *                               ↓
 *                    ┌─────────────────────┐
 *                    │   Configuration     │ ← ViewModel survives
 *                    │    Change         │
 *                    └─────────────────────┘
 *                               ↓
 *                           Activity Destroyed
 *                               ↓
 *                           ViewModel.onCleared()
 */

// ============================================================================
// BASIC VIEWMODEL
// ============================================================================

class UserProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    // UI State exposed as StateFlow (preferred over LiveData)
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    // One-time events using Channel (or SharedFlow)
    private val _events = Channel<UserProfileEvent>()
    val events = _events.receiveAsFlow()

    private var fetchJob: Job? = null

    init {
        // Called when ViewModel is created
        // Safe to start initial data loading
        loadUser()
    }

    fun loadUser(userId: String = "current") {
        // Cancel previous job to avoid multiple concurrent requests
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val user = userRepository.getUser(userId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                _events.send(UserProfileEvent.ShowError(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * onCleared() - ViewModel being destroyed
     * Called when Activity/Fragment is truly finishing (not config change)
     * Good for: Canceling all operations, releasing resources
     */
    override fun onCleared() {
        super.onCleared()
        // Cancel all coroutines
        fetchJob?.cancel()
        // Clean up resources
        println("🧹 ViewModel cleared - cleaning up resources")
    }
}

// ============================================================================
// VIEWMODEL WITH SAVED STATE
// ============================================================================

/**
 * ViewModel that survives process death using SavedStateHandle
 */
class SearchViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val searchRepository: SearchRepository
) : ViewModel() {

    // Keys for SavedStateHandle
    companion object {
        private const val KEY_QUERY = "query"
        private const val KEY_SELECTED_CATEGORY = "selected_category"
    }

    // Restore query from saved state (survives process death)
    private val _query = MutableStateFlow(
        savedStateHandle.get<String>(KEY_QUERY) ?: ""
    )
    val query: StateFlow<String> = _query.asStateFlow()

    // Persist query when it changes
    fun setQuery(newQuery: String) {
        _query.value = newQuery
        savedStateHandle[KEY_QUERY] = newQuery  // Survives process death
    }

    // Selected category with automatic persistence
    var selectedCategory: String?
        get() = savedStateHandle[KEY_SELECTED_CATEGORY]
        set(value) {
            savedStateHandle[KEY_SELECTED_CATEGORY] = value
        }

    // Search results
    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()

    fun search() {
        viewModelScope.launch {
            val currentQuery = _query.value
            if (currentQuery.isNotBlank()) {
                _results.value = searchRepository.search(currentQuery, selectedCategory)
            }
        }
    }
}

// ============================================================================
// VIEWMODEL FACTORY PATTERNS
// ============================================================================

/**
 * Factory with Dependencies (Manual DI)
 */
class UserViewModelFactory(
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserProfileViewModel::class.java)) {
            return UserProfileViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

// Usage in Activity
class MyActivity : AppCompatActivity() {
    private val viewModel: UserProfileViewModel by viewModels {
        UserViewModelFactory(AppDependencies.userRepository)
    }
}

/**
 * Factory with SavedStateHandle
 */
class SearchViewModelFactory(
    private val searchRepository: SearchRepository,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(handle, searchRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

// Usage
class SearchActivity : AppCompatActivity() {
    private val viewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(
            AppDependencies.searchRepository,
            this,
            intent.extras
        )
    }
}

/**
 * ViewModel with Assisted Injection (using Hilt)
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: ItemRepository,
    @Assisted private val itemId: String
) : ViewModel() {
    // ...
}

// ============================================================================
// VIEWMODEL SCOPES
// ============================================================================

/**
 * Different ViewModel scopes and their lifetimes
 */
class ViewModelScopeExamples : AppCompatActivity() {

    /**
     * Activity-Scoped ViewModel
     * - Lives as long as Activity
     * - Shared between all Fragments in the Activity
     */
    private val activityViewModel: SharedViewModel by viewModels()

    /**
     * Fragment-Scoped ViewModel
     * - Lives as long as Fragment
     * - Private to this Fragment
     */
    // In Fragment:
    // private val fragmentViewModel: MyViewModel by viewModels()

    /**
     * Parent Fragment-Scoped ViewModel
     * - Lives as long as parent Fragment
     * - Shared between parent and child Fragments
     */
    // In Child Fragment:
    // private val parentViewModel: ParentViewModel by viewModels({ requireParentFragment() })

    /**
     * Navigation Graph-Scoped ViewModel
     * - Lives as long as navigation graph
     * - Shared between destinations in the same graph
     */
    // private val navViewModel: NavViewModel by viewModels({ navController.getViewModelStore(R.id.nav_graph) })
}

/**
 * Shared ViewModel between Activity and Fragments
 */
class SharedViewModel : ViewModel() {

    private val _selectedItem = MutableSharedFlow<Item>()
    val selectedItem = _selectedItem.asSharedFlow()

    fun selectItem(item: Item) {
        viewModelScope.launch {
            _selectedItem.emit(item)
        }
    }
}

// In Activity - host the shared ViewModel
class MainActivity : AppCompatActivity() {
    val sharedViewModel: SharedViewModel by viewModels()
}

// In Fragment - access same instance
class ListFragment : Fragment() {
    private val sharedViewModel: SharedViewModel by activityViewModels()
}

class DetailFragment : Fragment() {
    private val sharedViewModel: SharedViewModel by activityViewModels()
}

// ============================================================================
// CONFIGURATION CHANGE HANDLING
// ============================================================================

/**
 * Best Practices for Configuration Changes
 */
class ConfigurationChangeBestPractices : AppCompatActivity() {

    private lateinit var viewModel: MyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[MyViewModel::class.java]

        /**
         * APPROACH 1: Let system handle it (Recommended)
         * - Activity is recreated
         * - ViewModel survives
         * - UI state in ViewModel is preserved
         */
        observeViewModel()

        /**
         * APPROACH 2: Handle config change manually (Rarely needed)
         * Add to AndroidManifest.xml:
         * android:configChanges="orientation|screenSize|keyboardHidden"
         *
         * Then handle in onConfigurationChanged()
         */
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Handle configuration change manually
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Adjust layout for landscape
        } else {
            // Adjust layout for portrait
        }
    }

    private fun observeViewModel() {
        // ViewModel survives config change
        // UI automatically updates with preserved state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: UiState) {
        // Update UI based on state
    }
}

// ============================================================================
// PROCESS DEATH HANDLING WITH VIEWMODEL
// ============================================================================

/**
 * Complete example of handling both config changes and process death
 */
class ProcessDeathSafeViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ArticleRepository
) : ViewModel() {

    companion object {
        private const val KEY_ARTICLE_ID = "article_id"
        private const val KEY_SCROLL_POSITION = "scroll_position"
        private const val KEY_COMMENTS_EXPANDED = "comments_expanded"
    }

    // Article ID restored from saved state
    val articleId: String = savedStateHandle[KEY_ARTICLE_ID]
        ?: throw IllegalStateException("Article ID required")

    // UI State - survives config change via ViewModel, process death via SavedStateHandle
    data class ArticleUiState(
        val article: Article? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        // Saved to handle
        val scrollPosition: Int = 0,
        val isCommentsExpanded: Boolean = false
    )

    private val _uiState = MutableStateFlow(
        ArticleUiState(
            scrollPosition = savedStateHandle[KEY_SCROLL_POSITION] ?: 0,
            isCommentsExpanded = savedStateHandle[KEY_COMMENTS_EXPANDED] ?: false
        )
    )
    val uiState: StateFlow<ArticleUiState> = _uiState.asStateFlow()

    init {
        loadArticle()
    }

    private fun loadArticle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val article = repository.getArticle(articleId)
                _uiState.update { it.copy(article = article, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    // Update scroll position - persists across process death
    fun updateScrollPosition(position: Int) {
        _uiState.update { it.copy(scrollPosition = position) }
        savedStateHandle[KEY_SCROLL_POSITION] = position
    }

    // Toggle comments - persists across process death
    fun toggleComments() {
        val newValue = !_uiState.value.isCommentsExpanded
        _uiState.update { it.copy(isCommentsExpanded = newValue) }
        savedStateHandle[KEY_COMMENTS_EXPANDED] = newValue
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any resources
    }
}

// ============================================================================
// DATA CLASSES
// ============================================================================

data class UserProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

sealed class UserProfileEvent {
    data class ShowError(val message: String) : UserProfileEvent()
    data class NavigateToSettings(val userId: String) : UserProfileEvent()
}

data class User(val id: String, val name: String, val email: String)
data class Item(val id: String, val name: String)
data class SearchResult(val id: String, val title: String, val snippet: String)
data class Article(val id: String, val title: String, val content: String)

// Repository interfaces
interface UserRepository {
    suspend fun getUser(userId: String): User
}

interface SearchRepository {
    suspend fun search(query: String, category: String?): List<SearchResult>
}

interface ItemRepository
interface ArticleRepository {
    suspend fun getArticle(articleId: String): Article
}

// App dependencies placeholder
object AppDependencies {
    val userRepository: UserRepository = object : UserRepository {
        override suspend fun getUser(userId: String): User {
            return User(userId, "John Doe", "john@example.com")
        }
    }
    val searchRepository: SearchRepository = object : SearchRepository {
        override suspend fun search(query: String, category: String?): List<SearchResult> = emptyList()
    }
    val articleRepository: ArticleRepository = object : ArticleRepository {
        override suspend fun getArticle(articleId: String): Article {
            return Article(articleId, "Sample", "Content")
        }
    }
}
