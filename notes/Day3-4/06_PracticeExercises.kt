/**
 * ============================================================================
 * DAY 3-4: ANDROID COMPONENT LIFECYCLES
 * ============================================================================
 * PRACTICE EXERCISES
 * ============================================================================
 *
 * Instructions:
 * 1. Try to solve each exercise on your own first
 * 2. Check the solution after attempting
 * 3. Run the code and verify it works
 * 4. Review the explanation for deeper understanding
 */

// ============================================================================
// EXERCISE 1: Lifecycle-Aware Timer
// ============================================================================
// Create a countdown timer that:
// - Automatically pauses when the Activity goes to background
// - Automatically resumes when the Activity comes to foreground
// - Preserves remaining time across configuration changes
// - Properly cleans up when Activity is destroyed

/**
 * YOUR TASK:
 * 1. Implement LifecycleAwareCountdownTimer class
 * 2. Use SavedStateHandle to preserve time across config changes
 * 3. Handle onPause() to pause timer
 * 4. Handle onResume() to resume timer
 * 5. Ensure no memory leaks
 */

// TODO: Implement your solution here
// class Exercise1TimerActivity : AppCompatActivity() { }

// ============================================================================
// SOLUTION 1: Lifecycle-Aware Timer
// ============================================================================
class Exercise1TimerActivity : AppCompatActivity() {

    private lateinit var viewModel: TimerViewModel
    private lateinit var timerText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        timerText = findViewById(R.id.timerText)

        // Get ViewModel with SavedStateHandle
        viewModel = ViewModelProvider(
            this,
            TimerViewModel.Factory(this, savedInstanceState)
        )[TimerViewModel::class.java]

        // Observe timer state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timerState.collect { state ->
                    timerText.text = formatTime(state.remainingSeconds)
                }
            }
        }

        // Add lifecycle-aware timer observer
        lifecycle.addObserver(TimerLifecycleObserver(viewModel))
    }

    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }
}

class TimerViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val KEY_REMAINING_TIME = "remaining_time"
        private const val KEY_IS_RUNNING = "is_running"
        private const val DEFAULT_TIME = 60 // 60 seconds
    }

    private val _timerState = MutableStateFlow(
        TimerState(
            remainingSeconds = savedStateHandle[KEY_REMAINING_TIME] ?: DEFAULT_TIME,
            isRunning = false
        )
    )
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer() {
        if (timerJob?.isActive == true) return

        timerJob = viewModelScope.launch {
            _timerState.update { it.copy(isRunning = true) }
            savedStateHandle[KEY_IS_RUNNING] = true

            while (_timerState.value.remainingSeconds > 0 && isActive) {
                delay(1000)
                val newTime = _timerState.value.remainingSeconds - 1
                _timerState.update { it.copy(remainingSeconds = newTime) }
                savedStateHandle[KEY_REMAINING_TIME] = newTime
            }

            _timerState.update { it.copy(isRunning = false) }
            savedStateHandle[KEY_IS_RUNNING] = false
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.update { it.copy(isRunning = false) }
        savedStateHandle[KEY_IS_RUNNING] = false
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    data class TimerState(
        val remainingSeconds: Int,
        val isRunning: Boolean
    )

    class Factory(
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle?
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            return TimerViewModel(handle) as T
        }
    }
}

class TimerLifecycleObserver(
    private val viewModel: TimerViewModel
) : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        // Resume timer when Activity is visible
        viewModel.startTimer()
    }

    override fun onPause(owner: LifecycleOwner) {
        // Pause timer when Activity goes to background
        viewModel.pauseTimer()
    }
}

// ============================================================================
// EXERCISE 2: Fragment with Proper View Lifecycle
// ============================================================================
// Create a Fragment that:
// - Uses ViewBinding correctly without memory leaks
// - Observes Flow using viewLifecycleOwner
// - Handles configuration changes properly
// - Saves scroll position across process death

/**
 * YOUR TASK:
 * 1. Implement ArticleListFragment with proper ViewBinding
 * 2. Collect Flow using viewLifecycleOwner
 * 3. Save and restore RecyclerView scroll position
 * 4. Clear binding reference in onDestroyView()
 */

// TODO: Implement your solution here
// class Exercise2ArticleListFragment : Fragment() { }

// ============================================================================
// SOLUTION 2: Fragment with Proper View Lifecycle
// ============================================================================
class Exercise2ArticleListFragment : Fragment() {

    // ✅ Proper ViewBinding pattern - nullable backing field
    private var _binding: ArticleListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ArticleListViewModel by viewModels()
    private lateinit var adapter: ArticleAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ArticleListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        restoreScrollPosition(savedInstanceState)
    }

    private fun setupRecyclerView() {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = ArticleAdapter { article ->
            navigateToDetail(article)
        }

        binding.recyclerView.apply {
            this.layoutManager = this@Exercise2ArticleListFragment.layoutManager
            this.adapter = this@Exercise2ArticleListFragment.adapter
        }
    }

    private fun observeViewModel() {
        // ✅ Use viewLifecycleOwner for Flow collection
        // This ensures collection stops when view is destroyed
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.articles.collect { articles ->
                    adapter.submitList(articles)
                }
            }
        }

        // ✅ Use viewLifecycleOwner for LiveData
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        // Handle errors
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errors.collect { error ->
                    showError(error)
                }
            }
        }
    }

    private fun restoreScrollPosition(savedInstanceState: Bundle?) {
        savedInstanceState?.getInt(KEY_SCROLL_POSITION, 0)?.let { position ->
            if (position > 0) {
                binding.recyclerView.post {
                    layoutManager.scrollToPosition(position)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current scroll position
        val position = layoutManager.findFirstVisibleItemPosition()
        outState.putInt(KEY_SCROLL_POSITION, position)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ✅ CRITICAL: Clear binding reference to prevent memory leak
        _binding = null
    }

    private fun navigateToDetail(article: Article) {
        val action = ArticleListFragmentDirections.actionToDetail(article.id)
        findNavController().navigate(action)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    companion object {
        private const val KEY_SCROLL_POSITION = "scroll_position"
    }
}

class ArticleListViewModel : ViewModel() {
    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles.asStateFlow()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errors = MutableSharedFlow<String>()
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    init {
        loadArticles()
    }

    private fun loadArticles() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Simulate network call
                delay(1000)
                _articles.value = listOf(
                    Article("1", "Article 1", "Content 1"),
                    Article("2", "Article 2", "Content 2"),
                    Article("3", "Article 3", "Content 3")
                )
            } catch (e: Exception) {
                _errors.emit("Failed to load articles")
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class ArticleAdapter(
    private val onItemClick: (Article) -> Unit
) : ListAdapter<Article, ArticleAdapter.ViewHolder>(ArticleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemArticleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemArticleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(article: Article) {
            binding.titleText.text = article.title
            binding.root.setOnClickListener { onItemClick(article) }
        }
    }
}

class ArticleDiffCallback : DiffUtil.ItemCallback<Article>() {
    override fun areItemsTheSame(old: Article, new: Article): Boolean = old.id == new.id
    override fun areContentsTheSame(old: Article, new: Article): Boolean = old == new
}

// ============================================================================
// EXERCISE 3: Detect and Fix Memory Leak
// ============================================================================
// Find and fix the memory leak in this code:

class LeakyLocationActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateUI(location)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Request location updates (LEAK!)
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000,
            10f,
            locationListener
        )
    }

    private fun updateUI(location: Location) {
        // Update UI with location
    }
}

/**
 * YOUR TASK:
 * Identify the memory leak and provide the fix
 */

// ============================================================================
// SOLUTION 3: Fixed Memory Leak
// ============================================================================
class FixedLocationActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager

    // ✅ Move listener to be a field, unregister in onStop/onDestroy
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Check if Activity is still valid
            if (!isFinishing && !isDestroyed) {
                updateUI(location)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    // ✅ Request updates in onStart
    override fun onStart() {
        super.onStart()
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                10f,
                locationListener,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Handle permission
        }
    }

    // ✅ CRITICAL: Unregister in onStop to prevent leak
    override fun onStop() {
        super.onStop()
        locationManager.removeUpdates(locationListener)
    }

    // Alternative: Use LifecycleObserver approach
    private fun setupLifecycleAwareLocation() {
        lifecycle.addObserver(LifecycleAwareLocationObserver(
            locationManager,
            onLocationUpdate = { location ->
                updateUI(location)
            }
        ))
    }

    private fun updateUI(location: Location) {
        findViewById<TextView>(R.id.locationText).text =
            "Lat: ${location.latitude}, Lng: ${location.longitude}"
    }
}

// ✅ Better: Lifecycle-aware observer pattern
class LifecycleAwareLocationObserver(
    private val locationManager: LocationManager,
    private val onLocationUpdate: (Location) -> Unit
) : DefaultLifecycleObserver {

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onLocationUpdate(location)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                10f,
                listener,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Handle permission
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        locationManager.removeUpdates(listener)
    }
}

// ============================================================================
// EXERCISE 4: Shared ViewModel Pattern
// ============================================================================
// Implement Master-Detail pattern with shared ViewModel:
// - MasterFragment shows list of items
// - DetailFragment shows item details
// - Both share same ViewModel scoped to Activity
// - Selecting item in Master updates Detail

// TODO: Implement Master-Detail with shared ViewModel

// ============================================================================
// SOLUTION 4: Shared ViewModel Pattern
// ============================================================================

// Shared ViewModel - Activity scoped
class MasterDetailViewModel : ViewModel() {

    private val _items = MutableLiveData<List<Item>>(emptyList())
    val items: LiveData<List<Item>> = _items

    // Selected item as StateFlow for reactive updates
    private val _selectedItem = MutableStateFlow<Item?>(null)
    val selectedItem: StateFlow<Item?> = _selectedItem.asStateFlow()

    init {
        // Load initial data
        loadItems()
    }

    private fun loadItems() {
        _items.value = listOf(
            Item("1", "Item 1", "Description 1"),
            Item("2", "Item 2", "Description 2"),
            Item("3", "Item 3", "Description 3")
        )
    }

    fun selectItem(item: Item) {
        _selectedItem.value = item
    }

    fun clearSelection() {
        _selectedItem.value = null
    }
}

// Master Fragment
class MasterFragment : Fragment() {

    // ✅ Use activityViewModels() to get Activity-scoped ViewModel
    private val viewModel: MasterDetailViewModel by activityViewModels()

    private var _binding: FragmentMasterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMasterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = MasterAdapter { item ->
            viewModel.selectItem(item)
            // Navigate to detail on phone, or detail updates on tablet
            if (isPhone()) {
                findNavController().navigate(R.id.action_master_to_detail)
            }
        }

        binding.recyclerView.adapter = adapter

        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }
    }

    private fun isPhone(): Boolean {
        return resources.configuration.smallestScreenWidthDp < 600
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Detail Fragment
class DetailFragment : Fragment() {

    // ✅ Same ViewModel instance as MasterFragment
    private val viewModel: MasterDetailViewModel by activityViewModels()

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Observe same selectedItem flow from shared ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedItem.collect { item ->
                    updateUI(item)
                }
            }
        }
    }

    private fun updateUI(item: Item?) {
        if (item == null) {
            binding.emptyText.isVisible = true
            binding.contentGroup.isVisible = false
        } else {
            binding.emptyText.isVisible = false
            binding.contentGroup.isVisible = true
            binding.titleText.text = item.name
            binding.descriptionText.text = item.description
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class MasterAdapter(
    private val onItemClick: (Item) -> Unit
) : ListAdapter<Item, MasterAdapter.ViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMasterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMasterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.nameText.text = item.name
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}

class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
    override fun areItemsTheSame(old: Item, new: Item): Boolean = old.id == new.id
    override fun areContentsTheSame(old: Item, new: Item): Boolean = old == new
}

// ============================================================================
// MOCK CLASSES FOR COMPILATION
// ============================================================================
class R {
    object layout {
        const val activity_timer = 1
        const val activity_location = 2
        const val fragment_master = 3
        const val fragment_detail = 4
        const val item_article = 5
        const val item_master = 6
    }
    object id {
        const val timerText = 1
        const val recyclerView = 2
        const val progressBar = 3
        const val locationText = 4
        const val titleText = 5
        const val descriptionText = 6
        const val emptyText = 7
        const val contentGroup = 8
        const val root = 9
        const val fragment_container = 10
        const val nameText = 11
    }
}

class AppCompatActivity {
    fun findViewById(id: Int): Any = Any()
    fun getSystemService(name: String): Any = Any()
    fun setContentView(layout: Int) {}
    fun onCreate(savedInstanceState: Bundle?) {}
    val lifecycle: Lifecycle = Lifecycle()
    val isFinishing: Boolean = false
    val isDestroyed: Boolean = false
}

class Lifecycle {
    fun addObserver(observer: Any) {}
}

class SavedStateRegistryOwner
class AbstractSavedStateViewModelFactory(owner: SavedStateRegistryOwner, args: Bundle?)
class Bundle {
    fun putInt(key: String, value: Int) {}
    fun getInt(key: String, default: Int): Int = 0
}

class TextView
class Snackbar {
    companion object {
        fun make(view: Any, message: String, duration: Int): Snackbar = Snackbar()
        const val LENGTH_LONG = 0
    }
}
class View {
    fun isVisible(visible: Boolean) {}
}
class LinearLayoutManager(context: Any)
class ListAdapter<T, VH> : RecyclerView.Adapter<VH>()
class RecyclerView {
    var layoutManager: Any? = null
    var adapter: Any? = null
    fun post(action: () -> Unit) {}
    class Adapter<VH>
    class ViewHolder(itemView: View)
}
class DiffUtil {
    abstract class ItemCallback<T> {
        abstract fun areItemsTheSame(old: T, new: T): Boolean
        abstract fun areContentsTheSame(old: T, new: T): Boolean
    }
}
class FragmentActivity : AppCompatActivity()
class ViewModelProvider(owner: Any, factory: Any? = null) {
    inline fun <reified T> get(): T = throw NotImplementedError()
}
class ViewModelStoreOwner
class ViewTreeViewModelStoreOwner
inline fun <reified VM : ViewModel> ComponentActivity.viewModels(): Lazy<VM> = lazy { throw NotImplementedError() }
inline fun <reified VM : ViewModel> Fragment.viewModels(): Lazy<VM> = lazy { throw NotImplementedError() }
inline fun <reified VM : ViewModel> Fragment.activityViewModels(): Lazy<VM> = lazy { throw NotImplementedError() }
abstract class ViewModel {
    val viewModelScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    open fun onCleared() {}
}
class SavedStateHandle {
    operator fun <T> set(key: String, value: T) {}
    operator fun <T> get(key: String): T? = null
}
class MutableLiveData<T>(value: T? = null) : LiveData<T>()
open class LiveData<T> {
    fun observe(owner: Any, observer: (T) -> Unit) {}
}
class MutableStateFlow<T>(value: T)
class StateFlow<T>
class SharedFlow<T>
class MutableSharedFlow<T>
fun <T> MutableStateFlow<T>.asStateFlow(): StateFlow<T> = StateFlow()
fun <T> MutableSharedFlow<T>.asSharedFlow(): SharedFlow<T> = SharedFlow()
class CoroutineScope(val dispatcher: Any)
class Dispatchers {
    companion object {
        val Main = Any()
        val IO = Any()
    }
}
fun CoroutineScope.launch(block: suspend () -> Unit): Job = Job()
suspend fun delay(time: Long) {}
class Job {
    fun cancel() {}
    val isActive: Boolean = false
}
fun LifecycleOwner.repeatOnLifecycle(state: Any, block: suspend () -> Unit) {}
class LifecycleOwner
class DefaultLifecycleObserver {
    open fun onResume(owner: LifecycleOwner) {}
    open fun onPause(owner: LifecycleOwner) {}
    open fun onStart(owner: LifecycleOwner) {}
    open fun onStop(owner: LifecycleOwner) {}
    open fun onCreate(owner: LifecycleOwner) {}
    open fun onDestroy(owner: LifecycleOwner) {}
}
class ComponentActivity : AppCompatActivity()
class Fragment {
    val viewLifecycleOwner: LifecycleOwner = LifecycleOwner()
    val lifecycleScope: CoroutineScope = CoroutineScope(Any())
    val resources: Resources = Resources()
    fun requireContext(): Any = Any()
    fun findNavController(): NavController = NavController()
}
class Resources {
    val configuration: Configuration = Configuration()
}
class Configuration {
    val smallestScreenWidthDp: Int = 0
}
class NavController {
    fun navigate(action: Int) {}
    fun navigate(directions: Any) {}
}
class TimerViewModel
class Article
class ArticleListBinding
class ItemArticleBinding
class ItemMasterBinding
class ArticleListBinding_
class FragmentMasterBinding
class FragmentDetailBinding
class MasterDetailViewModel
class Item
class MasterAdapter
class Location
class LocationManager {
    fun requestLocationUpdates(provider: String, minTime: Long, minDistance: Float, listener: LocationListener, looper: Looper?) {}
    fun removeUpdates(listener: LocationListener) {}
    companion object {
        const val GPS_PROVIDER = "gps"
    }
}
interface LocationListener {
    fun onLocationChanged(location: Location)
}
class Looper {
    companion object {
        fun getMainLooper(): Looper = Looper()
    }
}
class LayoutInflater {
    companion object {
        fun from(context: Any): LayoutInflater = LayoutInflater()
    }
    fun inflate(resource: Int, root: ViewGroup?, attachToRoot: Boolean): View = View()
}
class ViewGroup
class Context {
    companion object {
        const val LOCATION_SERVICE = "location"
    }
}
class ArticleAdapter(val onItemClick: (Article) -> Unit)
class ArticleListFragmentDirections {
    companion object {
        fun actionToDetail(articleId: String): Any = Any()
    }
}
class TimerState(val remainingSeconds: Int, val isRunning: Boolean)
class ArticleListViewModel
class TimerLifecycleObserver(val viewModel: TimerViewModel) : DefaultLifecycleObserver
class FixedLocationActivity : AppCompatActivity()
class LifecycleAwareLocationObserver(val locationManager: LocationManager, val onLocationUpdate: (Location) -> Unit) : DefaultLifecycleObserver
class MasterFragment : Fragment()
class DetailFragment : Fragment()
class MasterDetailViewModel_ : ViewModel()
class Exercise2ArticleListFragment : Fragment()
class LeakyLocationActivity : AppCompatActivity()
class Exercise1TimerActivity : AppCompatActivity()
