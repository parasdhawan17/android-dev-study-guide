# Navigation Component

NAVIGATION COMPONENT COMPLETE GUIDE
Modern Android Navigation - Jetpack Navigation Library

Benefits:
- Handles Fragment transactions automatically
- Type-safe argument passing with Safe Args
- Deep linking support
- Visual navigation graph
- Back stack management
- Up/Back button behavior

## 1. Setup & Basic Navigation

BUILD.GRADLE SETUP

build.gradle (app level):

plugins {
id 'androidx.navigation.safeargs.kotlin'
}

dependencies {
implementation "androidx.navigation:navigation-fragment-ktx:2.7.0"
implementation "androidx.navigation:navigation-ui-ktx:2.7.0"
// For Compose
implementation "androidx.navigation:navigation-compose:2.7.0"
}

NAVIGATION GRAPH (XML)

File: res/navigation/nav_graph.xml

&lt;?xml version="1.0" encoding="utf-8"?&gt;
&lt;navigation xmlns:android="http://schemas.android.com/apk/res/android"
xmlns:app="http://schemas.android.com/apk/res-auto"
android:id="@+id/nav_graph"
app:startDestination="@id/homeFragment"&gt;

&lt;fragment
android:id="@+id/homeFragment"
android:name="com.example.HomeFragment"
android:label="Home"&gt;
&lt;action
android:id="@+id/action_home_to_detail"
app:destination="@id/detailFragment" /&gt;
&lt;/fragment&gt;

&lt;fragment
android:id="@+id/detailFragment"
android:name="com.example.DetailFragment"
android:label="Detail"&gt;
&lt;argument
android:name="itemId"
app:argType="string"
app:nullable="false" /&gt;
&lt;/fragment&gt;

&lt;/navigation&gt;

NAV HOST SETUP (Activity Layout)

File: res/layout/activity_main.xml

&lt;?xml version="1.0" encoding="utf-8"?&gt;
&lt;androidx.constraintlayout.widget.ConstraintLayout
android:layout_width="match_parent"
android:layout_height="match_parent"&gt;

&lt;androidx.fragment.app.FragmentContainerView
android:id="@+id/nav_host_fragment"
android:name="androidx.navigation.fragment.NavHostFragment"
android:layout_width="match_parent"
android:layout_height="match_parent"
app:defaultNavHost="true"
app:navGraph="@navigation/nav_graph" /&gt;

&lt;/androidx.constraintlayout.widget.ConstraintLayout&gt;

## 2. Basic Navigation In Code

````kotlin
class NavigationBasics : Fragment() {

    /**
     * GETTING THE NAV CONTROLLER
     * ===========================
     */
    fun gettingNavController() {
        // Method 1: In Fragment
        val navController = findNavController()

        // Method 2: Via View
        view.findNavController()

        // Method 3: In Activity
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
    }

    /**
     * SIMPLE NAVIGATION
     * =================
     */
    fun navigateToDetail() {
        // Navigate using action ID
        findNavController().navigate(R.id.action_home_to_detail)
    }

    /**
     * NAVIGATION WITH SAFE ARGS
     * ==========================
     */
    fun navigateWithArguments(itemId: String) {
        // Using Safe Args plugin generates these classes automatically
        // Direction class generated from nav_graph.xml
        val action = HomeFragmentDirections.actionHomeToDetail(itemId)
        findNavController().navigate(action)
    }

    /**
     * NAVIGATION WITH BUNDLE (Without Safe Args)
     * ===========================================
     */
    fun navigateWithBundle(itemId: String) {
        val bundle = Bundle().apply {
            putString("itemId", itemId)
        }
        findNavController().navigate(R.id.detailFragment, bundle)
    }
}
````

## 3. Receiving Arguments

````kotlin
class DetailFragment : Fragment() {

    /**
     * SAFE ARGS - TYPE SAFE ARGUMENTS (Recommended)
     * =============================================
     */
    private val args: DetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Type-safe access to arguments
        val itemId = args.itemId
        val optionalParam = args.optionalParam  // If defined with default value
    }

    /**
     * TRADITIONAL BUNDLE ACCESS (Without Safe Args)
     * =============================================
     */
    fun accessArgumentsOldWay() {
        val itemId = arguments?.getString("itemId")
            ?: throw IllegalArgumentException("itemId required")
    }
}
````

## 4. Back Stack Management

````kotlin
class BackStackManagement : Fragment() {

    /**
     * NAVIGATE AND POP BEHAVIOR
     * ==========================
     *
     * POPUPTO WITH INCLUSIVE EXPLAINED:
     * ===================================
     *
     * popUpTo(destination) controls which destinations are removed from back stack.
     * The 'inclusive' flag determines whether the destination itself is also popped.
     *
     * VISUAL EXAMPLE:
     * ===============
     *
     * Scenario: User navigated through login flow, now going to home
     *
     * Current Back Stack:
     *     ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
     *     │  Splash │───►│  Login  │───►│   OTP   │───►│ Profile │  (current)
     *     │    A    │    │    B    │    │    C    │    │    D    │
     *     └─────────┘    └─────────┘    └─────────┘    └─────────┘
     *           ↑                                            ↑
     *      startDestination                              current
     *
     * ---------------------------------------------------------------------------
     * CASE 1: popUpTo(R.id.loginFragment) with inclusive = false
     * ---------------------------------------------------------------------------
     *
     * Code: popUpTo(R.id.loginFragment) { inclusive = false }
     *
     * Result: Pop everything ABOVE Login, keep Login and below
     *
     *     BEFORE:                       AFTER:
     *     ┌─────────┐                  ┌─────────┐
     *     │ Profile │  (current)       │  Home   │  (new destination)
     *     │    D    │                  │    E    │
     *     ├─────────┤   ← Popped       ├─────────┤
     *     │   OTP   │   ← Popped       │  Login  │  ← Kept (inclusive=false)
     *     │    C    │                  │    B    │
     *     ├─────────┤   ← Popped       ├─────────┤
     *     │  Login  │  ← Target       │  Splash │  ← Kept
     *     │    B    │                  │    A    │
     *     ├─────────┤                  └─────────┘
     *     │  Splash │
     *     │    A    │
     *     └─────────┘
     *
     * Back button from Home goes to: Login (B)
     *
     * ---------------------------------------------------------------------------
     * CASE 2: popUpTo(R.id.loginFragment) with inclusive = true
     * ---------------------------------------------------------------------------
     *
     * Code: popUpTo(R.id.loginFragment) { inclusive = true }
     *
     * Result: Pop everything UP TO AND INCLUDING Login
     *
     *     BEFORE:                       AFTER:
     *     ┌─────────┐                  ┌─────────┐
     *     │ Profile │  (current)       │  Home   │  (new destination)
     *     │    D    │                  │    E    │
     *     ├─────────┤   ← Popped       ├─────────┤
     *     │   OTP   │   ← Popped       │  Splash │  ← Kept (below target)
     *     │    C    │                  │    A    │
     *     ├─────────┤   ← Popped       └─────────┘
     *     │  Login  │  ← Target+Popped
     *     │    B    │   ↑ inclusive=true
     *     ├─────────┤
     *     │  Splash │
     *     │    A    │
     *     └─────────┘
     *
     * Back button from Home goes to: Splash (A) - or exits if A is start destination
     *
     * ---------------------------------------------------------------------------
     * CASE 3: popUpTo(R.id.nav_graph) with inclusive = true (CLEAR ALL)
     * ---------------------------------------------------------------------------
     *
     * Code: popUpTo(R.id.nav_graph) { inclusive = true }
     *
     * Result: Clear ENTIRE back stack
     *
     *     BEFORE:                       AFTER:
     *     ┌─────────┐                  ┌─────────┐
     *     │ Profile │  (current)       │  Home   │  (new destination)
     *     │    D    │                  │    E    │
     *     ├─────────┤   ← Popped       └─────────┘
     *     │   OTP   │   ← Popped              ↑
     *     ├─────────┤                        Empty back stack
     *     │  Login  │   ← Popped
     *     ├─────────┤
     *     │  Splash │   ← Popped (even start!)
     *     └─────────┘
     *
     * Back button from Home: Exits app (no back stack)
     *
     * COMMON USE CASES:
     * =================
     *
     * 1. After login → Home (clear login flow):
     *    popUpTo(R.id.loginFragment) { inclusive = true }
     *    - User can't go back to login with back button
     *
     * 2. Bottom navigation (no back stack accumulation):
     *    popUpTo(R.id.nav_graph) { inclusive = false }
     *    - Switching tabs clears other tabs' stacks
     *
     * 3. Single activity, finish on home:
     *    popUpTo(R.id.nav_graph) { inclusive = true }
     *    - Home is only destination, back exits
     */
    fun navigateAndClearBackStack() {
        findNavController().navigate(R.id.homeFragment, null, navOptions {
            // Clear entire back stack - remove ALL previous destinations
            // inclusive=true means even start destination is removed
            popUpTo(R.id.nav_graph) {
                inclusive = true
            }

            // OR: Pop up to specific destination, keeping it
            // inclusive=false means target destination (Login) stays in stack
            popUpTo(R.id.loginFragment) {
                inclusive = false  // LoginFragment remains in back stack
            }

            // OR: Pop up to and remove the target too
            // inclusive=true means target destination (Login) is also popped
            popUpTo(R.id.loginFragment) {
                inclusive = true   // LoginFragment is ALSO removed
            }
        })
    }

    /**
     * SINGLE TOP BEHAVIOR
     * ====================
     * Similar to Activity launchMode="singleTop"
     */
    fun singleTopNavigation() {
        findNavController().navigate(R.id.detailFragment, null, navOptions {
            // Don't create new instance if already on top
            launchSingleTop = true
        })
    }

    /**
     * PROGRAMMATIC BACK NAVIGATION
     * =============================
     */
    fun navigateBack() {
        // Simply pop back stack
        findNavController().popBackStack()

        // Pop to specific destination
        findNavController().popBackStack(R.id.homeFragment, false)
        // false = don't pop the destination itself
        // true = pop everything including the destination

        // Pop with result
        findNavController().previousBackStackEntry
            ?.savedStateHandle
            ?.set("result_key", "result_value")
        findNavController().popBackStack()
    }

    /**
     * LISTENING FOR BACK STACK CHANGES
     * ================================
     */
    fun observeBackStack() {
        findNavController().addOnDestinationChangedListener { controller, destination, arguments ->
            // Called on every navigation
            println("Navigated to: ${destination.label}")
            println("Destination ID: ${destination.id}")
            println("Arguments: $arguments")
        }
    }
}
````

## 5. Bottom Navigation Integration

````kotlin
class BottomNavigationSetup : AppCompatActivity() {

    lateinit var bottomNavigationView: BottomNavigationView

    /**
     * SETUP BOTTOM NAVIGATION WITH NAV CONTROLLER
     * ============================================
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Connect BottomNavigationView with NavController
        bottomNavigationView.setupWithNavController(navController)

        // Handle reselection behavior
        bottomNavigationView.setOnItemReselectedListener { item ->
            // Scroll to top or refresh current destination
            val currentDestination = navController.currentDestination?.id
            if (currentDestination == item.itemId) {
                // Handle reselection (e.g., scroll to top)
            }
        }
    }
}
````

BOTTOM NAVIGATION MENU

File: res/menu/bottom_nav_menu.xml

&lt;?xml version="1.0" encoding="utf-8"?&gt;
&lt;menu xmlns:android="http://schemas.android.com/apk/res/android"&gt;
&lt;item
android:id="@+id/homeFragment"
android:icon="@drawable/ic_home"
android:title="Home" /&gt;
&lt;item
android:id="@+id/searchFragment"
android:icon="@drawable/ic_search"
android:title="Search" /&gt;
&lt;item
android:id="@+id/profileFragment"
android:icon="@drawable/ic_profile"
android:title="Profile" /&gt;
&lt;/menu&gt;

Menu item IDs MUST match navigation graph destination IDs!

## 6. Deep Links

NAV GRAPH WITH DEEP LINKS

&lt;fragment
android:id="@+id/productDetailFragment"
android:name="com.example.ProductDetailFragment"&gt;

&lt;argument
android:name="productId"
app:argType="string" /&gt;

&lt;!-- Implicit Deep Link --&gt;
&lt;deepLink
android:id="@+id/deepLink"
app:uri="myapp://product/{productId}" /&gt;

&lt;!-- Multiple deep links --&gt;
&lt;deepLink
app:uri="https://www.myapp.com/product/{productId}" /&gt;

&lt;/fragment&gt;

````kotlin
class DeepLinkHandling : AppCompatActivity() {

    /**
     * MANIFEST CONFIGURATION FOR DEEP LINKS
     * ======================================
     *
 * <activity
 *     android:name=".MainActivity"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.intent.action.VIEW" />
 *         <category android:name="android.intent.category.DEFAULT" />
 *         <category android:name="android.intent.category.BROWSABLE" />
 *         <data android:scheme="myapp" android:host="product" />
 *         <data android:scheme="https" android:host="www.myapp.com" />
 *     </intent-filter>
 *     <nav-graph android:value="@navigation/nav_graph" />
 * </activity>
 */

    /**
     * PROGRAMMATIC DEEP LINK CREATION
     * ================================
     */
    fun createPendingIntentWithDeepLink(context: Context): PendingIntent {
        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.productDetailFragment)
            .setArguments(bundleOf("productId" to "123"))
            .createPendingIntent()

        return pendingIntent
    }

    /**
     * HANDLE DYNAMIC DEEP LINKS
     * ==========================
     */
    fun handleDynamicDeepLink() {
        // From notification
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("myapp://product/456"))

        // NavController will handle navigation automatically
        // if <deepLink> is defined in nav_graph
    }
}
````

## 7. Nested Navigation Graphs

NESTED NAVIGATION GRAPH STRUCTURE

&lt;navigation xmlns:app="http://schemas.android.com/apk/res-auto"
android:id="@+id/nav_graph"
app:startDestination="@id/home_graph"&gt;

&lt;!-- Nested Home Graph --&gt;
&lt;navigation
android:id="@+id/home_graph"
app:startDestination="@id/dashboardFragment"
app:moduleName="home_feature"&gt;

&lt;fragment
android:id="@+id/dashboardFragment"
android:name="com.example.DashboardFragment"&gt;
&lt;action
android:id="@+id/action_dashboard_to_notifications"
app:destination="@id/notificationsFragment" /&gt;
&lt;/fragment&gt;

&lt;fragment
android:id="@+id/notificationsFragment"
android:name="com.example.NotificationsFragment" /&gt;

&lt;/navigation&gt;

&lt;!-- Nested Profile Graph --&gt;
&lt;navigation
android:id="@+id/profile_graph"
app:startDestination="@id/profileFragment"&gt;

&lt;fragment
android:id="@+id/profileFragment"
android:name="com.example.ProfileFragment"&gt;
&lt;action
android:id="@+id/action_profile_to_settings"
app:destination="@id/settingsFragment" /&gt;
&lt;/fragment&gt;

&lt;fragment
android:id="@+id/settingsFragment"
android:name="com.example.SettingsFragment" /&gt;

&lt;/navigation&gt;

&lt;/navigation&gt;

````kotlin
class NestedNavigation : Fragment() {

    /**
     * NAVIGATE TO NESTED GRAPH
     * =========================
     */
    fun navigateToNestedGraph() {
        // Navigate to start destination of nested graph
        findNavController().navigate(R.id.profile_graph)

        // Or navigate to specific destination within graph
        findNavController().navigate(R.id.settingsFragment)
    }

    /**
     * POP UP TO NESTED GRAPH START
     * ==============================
     */
    fun popToNestedGraphStart() {
        findNavController().navigate(R.id.someDestination, null, navOptions {
            popUpTo(R.id.home_graph) {
                saveState = true  // Save state of nested graph
            }
        })
    }
}
````

## 8. Navigation With Compose

````kotlin
class NavigationComposeExample {

    /**
     * JETPACK COMPOSE NAVIGATION
     * ==========================
     */
    @Composable
    fun MyAppNavHost(
        modifier: Modifier = Modifier,
        navController: NavHostController = rememberNavController(),
        startDestination: String = "home"
    ) {
        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = startDestination
        ) {
            // Define destinations
            composable("home") { HomeScreen() }

            composable(
                "detail/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                DetailScreen(itemId = itemId)
            }

            // Navigation with object routes (Kotlin 2.0+)
            composable<Home> { HomeScreen() }

            composable<Detail> { backStackEntry ->
                val detail: Detail = backStackEntry.toRoute()
                DetailScreen(itemId = detail.itemId)
            }
        }
    }

    // Type-safe routes with Kotlin serialization
    @Serializable
    object Home

    @Serializable
    data class Detail(val itemId: String)

    /**
     * NAVIGATE IN COMPOSE
     * ===================
     */
    @Composable
    fun HomeScreen(navController: NavHostController = rememberNavController()) {
        Button(onClick = {
            // Simple navigation
            navController.navigate("detail/123")

            // With options
            navController.navigate("detail/123") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }

            // Type-safe navigation (Kotlin 2.0+)
            navController.navigate(Detail(itemId = "123"))
        }) {
            Text("Go to Detail")
        }
    }

    /**
     * BOTTOM NAVIGATION WITH COMPOSE
     * ===============================
     */
    @Composable
    fun BottomNavigationWithNavHost() {
        val navController = rememberNavController()
        val items = listOf("home", "search", "profile")

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val currentDestination = navController
                        .currentBackStackEntryAsState()
                        .value?.destination

                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { /* Icon */ },
                            label = { Text(screen) },
                            selected = currentDestination?.route == screen,
                            onClick = {
                                navController.navigate(screen) {
                                    // Pop up to start destination
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("home") { HomeScreen() }
                composable("search") { SearchScreen() }
                composable("profile") { ProfileScreen() }
            }
        }
    }

    /**
     * PASSING COMPLEX DATA IN COMPOSE NAVIGATION
     * ==========================================
     * Never pass objects directly - use IDs to fetch from ViewModel/Repository
     */
    @Composable
    fun DetailScreen(itemId: String?, viewModel: DetailViewModel = hiltViewModel()) {
        // Fetch data using ID
        val item by viewModel.getItem(itemId).collectAsState()

        // Display item
    }
}
````

## 9. Advanced Navigation Patterns

````kotlin
class AdvancedNavigationPatterns : Fragment() {

    /**
     * PASSING RESULTS BACK
     * ====================
     */
    fun navigateForResult() {
        // Navigate to destination
        findNavController().navigate(R.id.selectionFragment)
    }

    fun returnWithResult(selectedItem: String) {
        // Set result in saved state handle
        findNavController().previousBackStackEntry
            ?.savedStateHandle
            ?.set("selected_item", selectedItem)

        findNavController().popBackStack()
    }

    // In the caller fragment
    fun observeResult() {
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("selected_item")
            ?.observe(viewLifecycleOwner) { result ->
                // Handle result
            }
    }

    /**
     * CONDITIONAL NAVIGATION
     * =======================
     */
    fun conditionalNavigation() {
        val isLoggedIn = checkLoginStatus()

        if (isLoggedIn) {
            findNavController().navigate(R.id.action_login_to_home)
        } else {
            findNavController().navigate(R.id.action_splash_to_login)
        }
    }

    /**
     * GLOBAL ACTIONS
     * ==============
     * Actions defined at graph level, callable from any destination
     */
    fun useGlobalAction() {
        // Defined in nav_graph.xml at <navigation> level
        // <action android:id="@+id/action_global_to_settings" ... />
        findNavController().navigate(R.id.action_global_to_settings)
    }

    /**
     * NAVIGATION WITH ANIMATIONS
     * ===========================
     *
     * In nav_graph.xml:
     * <action
     *     android:id="@+id/action_home_to_detail"
     *     app:destination="@id/detailFragment"
     *     app:enterAnim="@anim/slide_in_right"
     *     app:exitAnim="@anim/slide_out_left"
     *     app:popEnterAnim="@anim/slide_in_left"
     *     app:popExitAnim="@anim/slide_out_right" />
     */

    /**
     * DYNAMIC NAVIGATION GRAPH
     * =========================
     */
    fun dynamicNavigationGraph(activity: AppCompatActivity) {
        val navHostFragment = NavHostFragment.create(R.navigation.nav_graph)

        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_container, navHostFragment)
            .setPrimaryNavigationFragment(navHostFragment)
            .commit()

        // Later get controller
        val navController = navHostFragment.navController
    }

    private fun checkLoginStatus(): Boolean = false
}
````

## 10. Interview Questions & Answers

Q: What are the benefits of Navigation Component?
A: - Handles fragment transactions automatically
- Type-safe argument passing with Safe Args
- Visual navigation graph in XML
- Deep linking support
- Proper back stack management
- Consistent up/back button behavior
- Navigation UI integration (BottomNav, Drawer)

Q: How does Navigation Component handle the back stack?
A: - Maintains a back stack of destinations
- Each navigate() adds to back stack (by default)
- popBackStack() removes current and goes back
- popUpTo in navOptions clears up to specific destination
- popUpTo with inclusive=true also removes that destination

Q: What's the difference between navigate() and popBackStack()?
A: - navigate() pushes new destination onto back stack
- popBackStack() pops current, returns to previous
- Navigate with popUpTo can simulate pop + navigate

Q: How do you pass data between destinations?
A: - Safe Args: Define arguments in nav_graph, use generated Directions classes
- Bundle: Traditional but not type-safe
- Result API: Use SavedStateHandle for returning results
- ViewModel: Shared ViewModel for complex data sharing

Q: Can Navigation Component work with multiple Fragments visible?
A: - Designed for single-destination-at-a-time (single activity pattern)
- For master-detail, use separate FragmentContainerViews
- Or use Navigation Rail/SlidingPaneLayout for large screens

Q: How does Navigation Component handle configuration changes?
A: - Back stack is automatically saved and restored
- ViewModels survive (scoped to NavBackStackEntry)
- Uses FragmentManager which handles state restoration

Q: What's a nested navigation graph?
A: - Sub-graph within main navigation graph
- Has own start destination
- Encapsulates related screens (e.g., login flow)
- Can be loaded as dynamic feature module
* - Cleared together when popping the graph

Q: How do you implement Bottom Navigation with Navigation?
A: - Menu items with same IDs as graph destinations
- Use BottomNavigationView.setupWithNavController()
- Each tab is a separate back stack (with proper setup)
- Use NavOptions to save/restore state when switching

Q: What are common Navigation Component pitfalls?
A: - Calling navigate() multiple times rapidly (debounce needed)
- Not using viewLifecycleOwner in Fragment with NavHost
- Using fragmentManager directly instead of NavController
- Passing complex objects as arguments (use IDs instead)
- Not handling deep links properly in manifest

## Mock Classes For Compilation

````kotlin
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.bundle.bundleOf
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.Navigation
import androidx.navigation.Navigator
import androidx.navigation.createNavOptions
import androidx.navigation.dynamic.features.DynamicInstallMonitor
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navArgs
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.collectAsState
import kotlinx.serialization.Serializable
import androidx.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHost
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.padding

class HomeFragment : Fragment()
class DashboardFragment : Fragment()
class NotificationsFragment : Fragment()
class ProfileFragment : Fragment()
class SettingsFragment : Fragment()
class DetailFragment : Fragment()
class ProductDetailFragment : Fragment()
class SelectionFragment : Fragment()
class LoginFragment : Fragment()
class SplashFragment : Fragment()
class SearchFragment : Fragment()

class HomeScreen
class DetailScreen(val itemId: String?)
class SearchScreen
class ProfileScreen

class DetailViewModel {
    fun getItem(itemId: String?) = kotlinx.coroutines.flow.flowOf<Any?>(null)
}
````

Generated by Safe Args plugin

````kotlin
class HomeFragmentDirections {
    companion object {
        fun actionHomeToDetail(itemId: String): Any = Any()
    }
}

class DetailFragmentArgs {
    val itemId: String = ""
    val optionalParam: String? = null
}

inline fun <reified T> Fragment.navArgs(): Lazy<T> = lazy { throw NotImplementedError() }

class R {
    object id {
        const val nav_host_fragment = 1
        const val nav_graph = 2
        const val homeFragment = 3
        const val detailFragment = 4
        const val home_graph = 5
        const val profile_graph = 6
        const val profileFragment = 7
        const val settingsFragment = 8
        const val loginFragment = 9
        const val action_home_to_detail = 10
        const val action_login_to_home = 11
        const val action_splash_to_login = 12
        const val action_global_to_settings = 13
        const val nav_host_container = 14
        const val productDetailFragment = 15
        const val selectionFragment = 16
    }
    object navigation {
        const val nav_graph = 1
    }
    object menu {
        const val bottom_nav_menu = 1
    }
    object layout {
        const val activity_main = 1
    }
    object anim {
        const val slide_in_right = 1
        const val slide_out_left = 2
        const val slide_in_left = 3
        const val slide_out_right = 4
    }
}

class Bundle {
    fun putString(key: String, value: String) {}
    fun getString(key: String): String? = null
}

class View {
    fun findNavController(): NavController = NavController()
}

class NavDeepLinkBuilder(val context: Context) {
    fun setGraph(graph: Int): NavDeepLinkBuilder = this
    fun setDestination(destination: Int): NavDeepLinkBuilder = this
    fun setArguments(args: Bundle): NavDeepLinkBuilder = this
    fun createPendingIntent(): PendingIntent = throw NotImplementedError()
}

class Intent(val action: String, val uri: Uri)
class Uri {
    companion object {
        fun parse(uriString: String): Uri = Uri()
    }
}

class ViewModel

class BottomNavigationView {
    fun setupWithNavController(navController: NavController) {}
    fun setOnItemReselectedListener(listener: (Any) -> Unit) {}
}

class NavController {
    fun navigate(destination: Int, args: Bundle? = null, navOptions: NavOptions? = null) {}
    fun navigate(destination: Any) {}
    fun navigate(route: String) {}
    fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {}
    fun popBackStack(): Boolean = false
    fun popBackStack(destination: Int, inclusive: Boolean): Boolean = false
    fun addOnDestinationChangedListener(listener: (NavController, NavDestination, Bundle?) -> Unit) {}
    val currentDestination: NavDestination? = null
    val graph: NavGraph = NavGraph()
    val currentBackStackEntry: Any? = null
    val previousBackStackEntry: Any? = null
}

class NavDestination {
    val id: Int = 0
    val label: CharSequence? = null
}

class NavGraph {
    fun findStartDestination(): Any = Any()
}

class NavOptions
class NavOptionsBuilder {
    var inclusive: Boolean = false
    var saveState: Boolean = false
    var launchSingleTop: Boolean = false
    var restoreState: Boolean = false
    fun popUpTo(destination: Int, popUpToBuilder: PopUpToBuilder.() -> Unit = {}) {}
    fun popUpTo(route: String, popUpToBuilder: PopUpToBuilder.() -> Unit = {}) {}
}

class PopUpToBuilder {
    var inclusive: Boolean = false
    var saveState: Boolean = false
}

inline fun navOptions(builder: NavOptionsBuilder.() -> Unit): NavOptions = NavOptions()

class NavHostFragment {
    val navController: NavController = NavController()

    companion object {
        fun create(graphResId: Int): NavHostFragment = NavHostFragment()
    }
}

class FragmentManager {
    fun beginTransaction(): Any = Any()
    fun findFragmentById(id: Int): Fragment? = null
}

class FragmentTransaction {
    fun replace(containerId: Int, fragment: Fragment): FragmentTransaction = this
    fun setPrimaryNavigationFragment(fragment: Fragment): FragmentTransaction = this
    fun commit(): Int = 0
}

class FragmentContainerView
class Modifier {
    companion object
    fun padding(paddingValues: Any): Modifier = this
}

class NavHostController : NavController()

@Composable
fun rememberNavController(): NavHostController = NavHostController()

@Composable
fun NavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit
) {}

class NavGraphBuilder {
    fun composable(
        route: String,
        arguments: List<Any> = emptyList(),
        content: @Composable (Any) -> Unit
    ) {}
    inline fun <reified T> composable(content: @Composable (Any) -> Unit) {}
}

fun navArgument(name: String, builder: NavArgumentBuilder.() -> Unit): Any = Any()

class NavArgumentBuilder {
    var type: NavType<*> = NavType.StringType
    var nullable: Boolean = false
    var defaultValue: Any? = null
}

abstract class NavType<T> {
    object StringType : NavType<String>()
}

inline fun <reified T> Any.toRoute(): T = throw NotImplementedError()

class NavigationBar
class NavigationBarItem
class Scaffold
class Text
class Button

@Composable
fun Button(onClick: () -> Unit, content: @Composable () -> Unit) {}

class hiltViewModel {
    companion object {
        inline operator fun <reified T> invoke(): T = throw NotImplementedError()
    }
}

class Home
class Detail

val supportFragmentManager: FragmentManager
    get() = FragmentManager()

class ComponentActivity : AppCompatActivity()
````
