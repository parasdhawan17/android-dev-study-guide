# Fragment Lifecycle

DAY 3-4: ANDROID COMPONENT LIFECYCLES
Topic 2: Fragment Lifecycle and Transaction Management

FRAGMENT LIFECYCLE OVERVIEW

Fragment lifecycle is MORE COMPLEX than Activity:
- Tied to host Activity's lifecycle
- Has its own additional states for view creation

onAttach()
↓
onCreate()
↓
onCreateView() ←──┐
↓               │
onViewCreated()     │
↓               │
onStart()           │
↓               │
onResume()          │
↓               │
(ACTIVE)            │
↓               │
onPause()           │
↓               │
onStop()            │
↓               │
onDestroyView() ────┘ (View destroyed, Fragment alive)
↓
onDestroy()
↓
onDetach()

FRAGMENT-SPECIFIC STATES:
- INITIALIZED: Fragment instantiated, not attached
- ATTACHED: Attached to Activity/FragmentManager
- CREATED: onCreate() called
- VIEW_CREATED: onCreateView() and onViewCreated() called
- STARTED: onStart() called
- RESUMED: onResume() called
- DESTROYED: Fragment destroyed

````kotlin
class FragmentLifecycleDemo : Fragment() {

    private var _binding: FragmentDemoBinding? = null
    // Use this property only between onCreateView and onDestroyView
    private val binding get() = _binding!!
````

## Onattach() - Fragment Attached To Host

````kotlin
    // Called when Fragment is attached to Activity/Fragment
    // Good for: Getting reference to host, accessing arguments
    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Get reference to host Activity
        if (context is OnFragmentInteractionListener) {
            listener = context
        }

        // Access arguments passed during creation
        val args = arguments
        val userId = args?.getString("user_id")

        println("📎 onAttach: Fragment attached to host")
    }
````

## Oncreate() - Fragment Created

````kotlin
    // Called when Fragment is created (before view creation)
    // Good for: Non-UI initialization, ViewModel setup, parsing arguments
    // BAD for: View-related operations (view doesn't exist yet)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModel (scoped to Fragment)
        viewModel = ViewModelProvider(this)[FragmentViewModel::class.java]

        // Parse arguments
        val args: FragmentArgs by navArgs()
        userId = args.userId

        // Restore state
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt("selected_tab", 0)
        }

        println("🏗️ onCreate: Fragment instance created")
    }
````

## Oncreateview() - Create Fragment'S View Hierarchy

````kotlin
    // Called to inflate/create the Fragment's UI
    // Returns: View hierarchy root, or null for non-UI Fragment
    // CRITICAL: Only inflate here, DON'T setup views (use onViewCreated)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout
        _binding = FragmentDemoBinding.inflate(inflater, container, false)

        println("🎨 onCreateView: View hierarchy created")
        return binding.root
    }
````

## Onviewcreated() - View Created, Ready For Setup

````kotlin
    // Called immediately after onCreateView() returns
    // Good for: Finding views, setting listeners, observing LiveData/Flow
    // View is fully created and safe to use
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = myAdapter

        // Setup click listeners
        binding.buttonSubmit.setOnClickListener { onSubmitClicked() }

        // Observe ViewModel
        viewModel.items.observe(viewLifecycleOwner) { items ->
            myAdapter.submitList(items)
        }

        // Observe Flow (lifecycle-aware)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { state ->
                    updateUi(state)
                }
        }

        println("✅ onViewCreated: Views ready for setup")
    }
````

## Onstart() - Fragment Visible

````kotlin
    // Fragment's view is visible to user
    // Good for: Refreshing data, starting animations
    override fun onStart() {
        super.onStart()
        println("🟢 onStart: Fragment visible")
    }
````

## Onresume() - Fragment Active

````kotlin
    // Fragment active and user can interact with it
    override fun onResume() {
        super.onResume()
        println("🟡 onResume: Fragment active")
    }
````

## Onpause() - Fragment Losing Focus

````kotlin
    override fun onPause() {
        super.onPause()
        println("🟠 onPause: Fragment losing focus")
    }
````

## Onstop() - Fragment Not Visible

````kotlin
    override fun onStop() {
        super.onStop()
        println("🔴 onStop: Fragment not visible")
    }
````

## Ondestroyview() - View Destroyed, Fragment Still Alive

````kotlin
    // Called when Fragment's view is destroyed but Fragment instance lives on
    // Happens during: Back stack navigation, replace transaction, config change
    // CRITICAL: Clean up View references to prevent memory leaks!
    override fun onDestroyView() {
        super.onDestroyView()

        // CRITICAL: Clear binding reference to prevent memory leaks
        _binding = null

        // Remove observers tied to view
        // Cancel view-related coroutines

        println("💥 onDestroyView: View destroyed, Fragment instance alive")
    }
````

## Ondestroy() - Fragment Instance Destroyed

````kotlin
    override fun onDestroy() {
        super.onDestroy()
        println("⚫ onDestroy: Fragment instance destroyed")
    }
````

## Ondetach() - Fragment Detached From Host

````kotlin
    override fun onDetach() {
        super.onDetach()
        listener = null
        println("🔓 onDetach: Fragment detached from host")
    }
````

## Onsaveinstancestate() - Save State

````kotlin
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selected_tab", selectedTab)
        println("💾 onSaveInstanceState: State saved")
    }
}
````

FRAGMENT TRANSACTIONS - BEST PRACTICES

````kotlin
class FragmentTransactionBestPractices : AppCompatActivity() {

    fun demonstrateTransactions() {
````

## Basic Transactions

````kotlin
        // Add Fragment
        supportFragmentManager.commit {
            setReorderingAllowed(true)  // Optimizes transaction
            add<HomeFragment>(R.id.fragment_container, "home")
        }

        // Replace Fragment
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<DetailsFragment>(R.id.fragment_container, "details")
            addToBackStack("details_transaction")  // Allows back navigation
        }

        // Remove Fragment
        supportFragmentManager.commit {
            remove(existingFragment)
        }
````

## Back Stack Management

````kotlin
        // Add to back stack (user can press back to reverse)
        supportFragmentManager.commit {
            replace<DetailsFragment>(R.id.fragment_container)
            addToBackStack("details")  // Name for this back stack entry
        }

        // Pop back stack (programmatic back)
        supportFragmentManager.popBackStack()

        // Pop to specific transaction
        supportFragmentManager.popBackStack("details", FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // Clear entire back stack
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
````

## Transition Animations

````kotlin
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<DetailsFragment>(R.id.fragment_container)

            // Set custom animations
            setCustomAnimations(
                R.anim.slide_in_right,   // Enter
                R.anim.slide_out_left,     // Exit
                R.anim.slide_in_left,      // Pop enter
                R.anim.slide_out_right     // Pop exit
            )
        }
    }
````

## Fragment Communication Patterns

````kotlin
    /**
     * PATTERN 1: Interface Callback (Activity as Mediator)
     * Best for: Child-to-Parent communication
     */
    interface OnFragmentInteractionListener {
        fun onItemSelected(itemId: String)
    }

    /**
     * PATTERN 2: Shared ViewModel (Recommended for siblings)
     * Best for: Communication between sibling Fragments
     */
    class SharedViewModel : ViewModel() {
        private val _selectedItem = MutableLiveData<Item>()
        val selectedItem: LiveData<Item> = _selectedItem

        fun selectItem(item: Item) {
            _selectedItem.value = item
        }
    }

    /**
     * PATTERN 3: Navigation Component (Modern approach)
     * Best for: App-wide navigation
     */
    fun navigateWithArgs() {
        val action = ListFragmentDirections.actionListToDetail(itemId = "123")
        findNavController().navigate(action)
    }
}
````

VIEWLIFECYCLEOWNER vs LIFECYCLEOWNER

Fragment has TWO lifecycles:

1. lifecycle (Fragment lifecycle)
- Starts: onAttach()
- Ends: onDetach()
- Use for: Fragment-level operations, ViewModel observation

2. viewLifecycleOwner.lifecycle (View lifecycle)
- Starts: onCreateView()
- Ends: onDestroyView()
- Use for: UI observations (LiveData, Flow for UI updates)

WHY TWO LIFECYCLES?
- Fragment can exist without view (back stack)
- View can be destroyed/recreated while Fragment lives
- Observing UI data with Fragment lifecycle causes crashes after onDestroyView()

````kotlin
class ViewLifecycleExample : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ❌ WRONG: Using Fragment lifecycle for UI observation
        viewModel.items.observe(this) { /* ... */ }
        // Problem: Observer fires after onDestroyView() → crashes!

        // ✅ CORRECT: Using viewLifecycleOwner for UI
        viewModel.items.observe(viewLifecycleOwner) { items ->
            binding.recyclerView.adapter = MyAdapter(items)
        }

        // ✅ CORRECT: Using lifecycleScope for Fragment-level work
        lifecycleScope.launch {
            // Fragment-level coroutine
        }

        // ✅ CORRECT: Using viewLifecycleOwner.lifecycleScope for UI work
        viewLifecycleOwner.lifecycleScope.launch {
            // View-level coroutine (canceled in onDestroyView)
        }
    }
}
````

FRAGMENT LIFECYCLE - COMMON PITFALLS

1. Memory Leak: Holding view reference after onDestroyView()
✅ Fix: Clear binding in onDestroyView()

2. IllegalStateException: Transaction after onSaveInstanceState()
✅ Fix: Use commitAllowingStateLoss() or check isStateSaved

3. Observing LiveData after onDestroyView()
✅ Fix: Use viewLifecycleOwner instead of 'this'

4. ChildFragmentManager vs ParentFragmentManager confusion
✅ Fix: Use childFragmentManager for nested fragments

5. Not setting setReorderingAllowed(true)
✅ Fix: Always set it for better performance

FRAGMENT TRANSACTION STATE SAFETY

````kotlin
fun safeFragmentTransaction(fragmentManager: FragmentManager) {
    if (!fragmentManager.isStateSaved) {
        // Safe to commit
        fragmentManager.commit {
            replace<NewFragment>(R.id.container)
        }
    } else {
        // State already saved, use alternative
        // Option 1: Don't commit (operation will be lost)
        // Option 2: Commit allowing state loss (may lose state)
        fragmentManager.commit(allowStateLoss = true) {
            replace<NewFragment>(R.id.container)
        }
    }
}
````
