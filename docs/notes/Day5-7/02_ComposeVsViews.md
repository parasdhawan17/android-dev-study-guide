# Compose Vs Views

DAY 5-7: Jetpack Compose vs Legacy View System

Understanding differences, interoperability, and migration strategies.

````kotlin
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner
````

## 1. Key Differences Table

┌─────────────────────┬──────────────────────────┬────────────────────────────┐
│ Aspect              │ View System              │ Jetpack Compose            │
├─────────────────────┼──────────────────────────┼────────────────────────────┤
│ UI Definition       │ XML + findViewById       │ Kotlin functions           │
│ State Management    │ Manual view updates      │ Automatic recomposition    │
│ Updates             │ Explicit setText/etc     │ State-driven, reactive     │
│ Layout              │ Measure/Layout pass      │ Single-pass composition    │
│ Custom Views        │ Extend View class      │ Composable functions       │
│ Preview             │ XML visual editor        │ @Preview annotation        │
│ Threading           │ UI thread required       │ Composition any thread     │
│ Performance         │ View inflation cost      │ Compiler optimizations     │
│ Learning Curve      │ XML + View APIs          │ Kotlin + Compose APIs      │
│ Tooling             │ Layout Inspector         │ Compose Preview, Layout    │
└─────────────────────┴──────────────────────────┴────────────────────────────┘

## 2. State Management Comparison

VIEW SYSTEM - Imperative (manual updates)

````kotlin
class LegacyProfileActivity : AppCompatActivity() {
    
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var loadingView: View
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        // Find views
        nameTextView = findViewById(R.id.nameText)
        emailTextView = findViewById(R.id.emailText)
        loadingView = findViewById(R.id.loadingView)
        
        // Manual update when data changes
        loadUserData()
    }
    
    private fun updateUI(user: User?, isLoading: Boolean, error: String?) {
        // Manual visibility and text updates
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        
        if (error != null) {
            nameTextView.text = "Error: $error"
            emailTextView.visibility = View.GONE
        } else if (user != null) {
            nameTextView.text = user.name
            emailTextView.text = user.email
            emailTextView.visibility = View.VISIBLE
        }
    }
    
    private fun loadUserData() {
        // Must manually call updateUI after each state change
        updateUI(null, true, null)
        // ... fetch data ...
        updateUI(User("John", "john@example.com"), false, null)
    }
}
````

COMPOSE - Declarative (state-driven)

````kotlin
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    // Automatic recomposition when state changes
    val uiState by viewModel.uiState.collectAsState()
    
    when {
        uiState.isLoading -> LoadingIndicator()
        uiState.error != null -> ErrorMessage(uiState.error)
        uiState.user != null -> UserProfile(uiState.user)
    }
    // No manual view updates needed!
}
````

## 3. Interoperability: Compose In Views

Add ComposeView to existing XML layout
&lt;!-- res/layout/activity_legacy.xml --&gt;
&lt;LinearLayout ...&gt;
&lt;TextView android:id="@+id/titleText" ... /&gt;

&lt;!-- Compose UI embedded here --&gt;
&lt;androidx.compose.ui.platform.ComposeView
android:id="@+id/compose_view"
android:layout_width="match_parent"
android:layout_height="wrap_content" /&gt;

&lt;Button android:id="@+id/doneButton" ... /&gt;
&lt;/LinearLayout&gt;

Activity using ComposeView

````kotlin
class HybridActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legacy)
        
        findViewById<ComposeView>(R.id.compose_view).apply {
            setContent {
                MaterialTheme {
                    ModernSettingsUI(
                        onSettingChanged = { setting ->
                            // Communicate back to Activity
                            handleSettingChange(setting)
                        }
                    )
                }
            }
        }
    }
    
    private fun handleSettingChange(setting: Setting) {
        // Handle changes from Compose UI
    }
}
````

## 4. Interoperability: Views In Compose

````kotlin
@Composable
fun HybridAnalyticsChart(
    data: ChartData,
    onPointSelected: (DataPoint) -> Unit
) {
    Column {
        Text("Analytics", style = MaterialTheme.typography.headlineSmall)
        
        // Legacy custom View inside Compose
        AndroidView(
            factory = { context ->
                // Create legacy custom view
                CustomChartView(context).apply {
                    // Initial setup
                    setupChart()
                }
            },
            update = { chartView ->
                // Called on recomposition - update the view
                chartView.setData(data)
                chartView.onPointClick = { point ->
                    onPointSelected(point)
                }
            },
            // Optional: key to force recreation
            // modifier = Modifier.fillMaxWidth().height(200.dp)
        )
        
        Text("Tap chart points for details", 
             style = MaterialTheme.typography.bodySmall)
    }
}
````

More complex example with lifecycle awareness

````kotlin
@Composable
fun VideoPlayer(
    videoUrl: String,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoPath(videoUrl)
            }
        },
        update = { videoView ->
            // Handle lifecycle
            when (lifecycleOwner.lifecycle.currentState) {
                androidx.lifecycle.Lifecycle.State.RESUMED -> videoView.start()
                androidx.lifecycle.Lifecycle.State.PAUSED -> videoView.pause()
                else -> { /* no-op */ }
            }
        }
    )
}
````

## 5. Migration Strategies

Strategy 1: New Screens in Compose

````kotlin
class NewFeatureActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Full Compose screen
        setContent {
            MyApplicationTheme {
                NewFeatureScreen()
            }
        }
    }
}
````

Strategy 2: Gradual Migration with ComposeView

````kotlin
class ExistingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.existing_layout)
        
        // Migrate one component at a time
        findViewById<ComposeView>(R.id.header_compose).setContent {
            HeaderComponent()
        }
        
        findViewById<ComposeView>(R.id.list_compose).setContent {
            ListComponent()
        }
    }
}
````

Strategy 3: Fragment-based Migration

````kotlin
class ComposeFragment : androidx.fragment.app.Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                FragmentContent()
            }
        }
    }
}
````

## 6. When To Use Each

USE COMPOSE:
✓ New features/screens
✓ Lists with dynamic content (LazyColumn/LazyRow)
✓ Animations and transitions
✓ Complex conditional UI
✓ Theming and design system
✓ Rapid prototyping
✓ Complex forms with validation

KEEP VIEWS:
✓ Existing complex custom views (no immediate rewrite needed)
✓ Performance-critical rendering (games, video, camera)
✓ Heavy reliance on third-party View libraries
✓ Large existing XML layouts (gradual migration)
✓ Apps with minimal UI updates

## 7. Compose-Specific Features

Preview - instant UI preview in IDE

````kotlin
@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                user = User("Preview User", "preview@test.com"),
                isLoading = false,
                error = null
            )
        )
    }
}
````

Multiple previews

````kotlin
@Preview(name = "Loading", showBackground = true)
@Composable
fun ProfileLoadingPreview() {
    MaterialTheme {
        ProfileScreen(
            uiState = ProfileUiState(isLoading = true)
        )
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
fun ProfileErrorPreview() {
    MaterialTheme {
        ProfileScreen(
            uiState = ProfileUiState(error = "Network error")
        )
    }
}
````

## 8. Common Migration Patterns

XML Layout → Compose
BEFORE (XML):
&lt;LinearLayout android:orientation="vertical"&gt;
&lt;TextView android:text="@{viewModel.title}" /&gt;
&lt;ProgressBar android:visibility="@{viewModel.loading ? visible : gone}" /&gt;
&lt;RecyclerView android:adapter="@adapter" /&gt;
&lt;/LinearLayout&gt;

AFTER (Compose):

````kotlin
@Composable
fun MigratedScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column {
        Text(uiState.title)
        
        if (uiState.isLoading) {
            CircularProgressIndicator()
        }
        
        LazyColumn {
            items(uiState.items) { item ->
                ItemRow(item)
            }
        }
    }
}
````

RecyclerView.Adapter → LazyColumn
BEFORE:
class MyAdapter : RecyclerView.Adapter&lt;ViewHolder&gt;() {
override fun onCreateViewHolder(...) = ...
override fun onBindViewHolder(holder, position) {
holder.bind(items[position])
}
}

AFTER:

````kotlin
@Composable
fun MyList(items: List<Item>) {
    LazyColumn {
        items(
            items = items,
            key = { it.id } // Stable keys for performance
        ) { item ->
            ItemCard(item)
        }
    }
}
````

View Binding → Compose Parameters
BEFORE:
binding.nameText.text = user.name
binding.emailText.text = user.email
binding.profileImage.load(user.avatarUrl)

AFTER:

````kotlin
@Composable
fun UserCard(user: User) {
    Column {
        Text(user.name)
        Text(user.email)
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = "Profile picture"
        )
    }
}
````

## 9. Performance Considerations

Compose optimization: Stable keys for lists

````kotlin
@Composable
fun OptimizedList(users: List<User>) {
    LazyColumn {
        items(
            items = users,
            key = { user -> user.id } // Stable key prevents unnecessary recomposition
        ) { user ->
            UserRow(user)
        }
    }
}
````

Compose optimization: remember for expensive operations

````kotlin
@Composable
fun ExpensiveCalculation(data: List<Int>) {
    // Cache result until data changes
    val sorted = remember(data) {
        data.sortedDescending() // Expensive sort
    }
}
````

View system optimization: ViewHolder pattern
(Still relevant when using RecyclerView within Compose)

COMPOSE PERFORMANCE TIPS:
1. Use @Stable/@Immutable for custom classes
2. Avoid unnecessary recomposition with remember
3. Use LazyColumn/LazyRow for large lists
4. Profile with Layout Inspector (recomposition counts)
5. Use derivedStateOf for expensive derived values

VIEW SYSTEM PERFORMANCE TIPS:
1. ViewHolder pattern for RecyclerView
2. Avoid deep view hierarchies
3. Use ConstraintLayout to flatten layouts
4. Recycle views properly

## 10. Testing Comparison

View System Testing
@RunWith(AndroidJUnit4::class)
class ProfileActivityTest {
@get:Rule
val activityRule = ActivityScenarioRule(ProfileActivity::class.java)

@Test
fun displaysUserName() {
onView(withId(R.id.nameText))
.check(matches(withText("John Doe")))
}
}

Compose Testing
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun profileScreen_displaysUserName() {
composeTestRule.setContent {
ProfileScreen(
uiState = ProfileUiState(
user = User("John Doe", "john@example.com")
)
)
}

composeTestRule
.onNodeWithText("John Doe")
.assertIsDisplayed()
}

````kotlin
@Composable
fun ProfileScreen(uiState: ProfileUiState) {
    // Stub implementation
}

@Composable
fun LoadingIndicator() {}
@Composable
fun ErrorMessage(error: String?) {}
@Composable
fun UserProfile(user: User) {}
@Composable
fun ModernSettingsUI(onSettingChanged: (Setting) -> Unit) {}
@Composable
fun HeaderComponent() {}
@Composable
fun ListComponent() {}
@Composable
fun FragmentContent() {}
@Composable
fun ItemRow(item: Item) {}
@Composable
fun ItemCard(item: Item) {}
@Composable
fun UserRow(user: User) {}
@Composable
fun UserCard(user: User) {}
@Composable
fun CircularProgressIndicator() {}
@Composable
fun AsyncImage(model: String, contentDescription: String) {}

annotation class Preview(val showBackground: Boolean = false, val name: String = "", val device: String = "")
````

Stub classes

````kotlin
data class User(val name: String, val email: String)
data class Setting(val key: String, val value: String)
data class Item(val id: String, val name: String)
data class ChartData(val points: List<DataPoint>)
data class DataPoint(val x: Float, val y: Float, val label: String)
class CustomChartView(context: Context) : View(context) {
    fun setupChart() {}
    fun setData(data: ChartData) {}
    var onPointClick: ((DataPoint) -> Unit)? = null
}
class ProfileViewModel { val uiState: kotlinx.coroutines.flow.StateFlow<ProfileUiState> by lazy { kotlinx.coroutines.flow.MutableStateFlow(ProfileUiState()) } }
data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
class MyViewModel { val uiState: kotlinx.coroutines.flow.StateFlow<MyUiState> by lazy { kotlinx.coroutines.flow.MutableStateFlow(MyUiState()) } }
data class MyUiState(val title: String = "", val isLoading: Boolean = false, val items: List<Item> = emptyList())
class VideoView(context: Context) : View(context) {
    fun setVideoPath(path: String) {}
    fun start() {}
    fun pause() {}
}
object R {
    object layout {
        const val activity_profile = 1
        const val existing_layout = 2
    }
    object id {
        const val nameText = 1
        const val emailText = 2
        const val loadingView = 3
        const val compose_view = 4
        const val header_compose = 5
        const val list_compose = 6
    }
}

fun AppCompatActivity.setContent(content: @Composable () -> Unit) {}
fun Column(content: @Composable () -> Unit) {}
fun LazyColumn(content: LazyListScope.() -> Unit) {}
interface LazyListScope {
    fun <T> items(items: List<T>, key: ((T) -> Any)? = null, itemContent: @Composable (T) -> Unit)
}
fun <T> MutableStateFlow(value: T): kotlinx.coroutines.flow.MutableStateFlow<T> { TODO() }
fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsState(): androidx.compose.runtime.State<T> { TODO() }
val androidx.compose.runtime.State<*>.value: Any? get() = null
annotation classComposable
annotation classComposable
````
