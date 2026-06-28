# Day 3-4: Android Component Lifecycles - Cheat Sheet

---

## Activity Lifecycle Quick Reference

### Complete Activity Lifecycle

```
Created              Started              Resumed              Started              Created
   │                    │                    │                    │                    │
   ▼                    ▼                    ▼                    ▼                    ▼
onCreate() ──► onStart() ──► onResume() ──► onPause() ──► onStop() ──► onDestroy()
                   ▲           (User Active)     │              ▲
                   └─────────────────────────────┘              │
                            onRestart() (if returning)          │
                              (only after onStop)
```

### Activity Callbacks Summary

| Callback | When Called | Use For |
|----------|-------------|---------|
| `onCreate()` | Activity first created | UI setup, ViewModel init, restore state |
| `onStart()` | Activity visible | Refresh data, start animations |
| `onResume()` | Activity focused | Resume camera, acquire resources |
| `onPause()` | Activity losing focus | Pause camera, stop animations (**QUICK!**) |
| `onStop()` | Activity not visible | Heavy cleanup, save to database |
| `onRestart()` | Activity restarting | Rarely used |
| `onDestroy()` | Activity destroyed | Final cleanup |

### Configuration Changes
- System destroys and recreates Activity on rotation, language change, etc.
- ViewModel **survives** configuration changes
- `onSaveInstanceState()` → Activity destroyed → `onCreate(Bundle)` with saved state

---

## Fragment Lifecycle Quick Reference

### Complete Fragment Lifecycle

```
    onAttach() ──► onCreate() ──► onCreateView() ──► onViewCreated()
       (to Activity)       (instance)       (create View)    (setup Views)
                                              │                    │
                                              ▼                    ▼
                                          onStart() ──► onResume()
                                              │      (Active)      │
                                              ▼                    ▼
                                          onPause() ◄── onStop()
                                              │
                                              ▼
                                          onDestroyView() ◄── View destroyed
                                              │              (Fragment alive)
                                              ▼
                                          onDestroy() ──► onDetach()
```

### Fragment Key Points
- **Two lifecycles**: Fragment lifecycle AND View lifecycle
- `viewLifecycleOwner` for UI observations (Flow, LiveData)
- `lifecycle` for Fragment-level operations
- Always clear `binding` in `onDestroyView()` to prevent memory leaks

---

## ViewModel Lifecycle

```
Activity Created
       │
       ▼
ViewModel Created ──── Configuration Change ────► ViewModel Survives
       │                                                │
       │                                         Activity Recreated
       │                                                │
       │                                                ▼
       │                                         ViewModel Still Active
       │                                                │
       ▼                                                │
Activity Destroyed ────────────────────────────────────┘
       │
       ▼
ViewModel.onCleared()
```

### ViewModel Key Points
- Survives configuration changes
- Destroyed when Activity/Fragment truly finishes
- Use `viewModelScope` for coroutines (auto-canceled on clear)
- Use `SavedStateHandle` for process death survival

### ViewModel Scopes

```kotlin
// Activity-scoped (shared across fragments)
val viewModel: MyViewModel by viewModels()

// Fragment-scoped (private to this fragment)
val viewModel: MyViewModel by viewModels()

// Parent Fragment-scoped
val viewModel: MyViewModel by viewModels({ requireParentFragment() })

// Activity-scoped from Fragment (shared)
val viewModel: MyViewModel by activityViewModels()
```

---

## State Persistence Comparison

| Storage | Survives Config Change | Survives Process Death | Use For |
|---------|---------------------|----------------------|---------|
| Variable in Activity | ❌ No | ❌ No | Temporary calculation |
| ViewModel | ✅ Yes | ❌ No | In-memory UI state |
| `onSaveInstanceState()` | ✅ Yes | ✅ Yes | Small UI state (scroll, input) |
| `SavedStateHandle` | ✅ Yes | ✅ Yes | UI state in ViewModel |
| SharedPreferences | ✅ Yes | ✅ Yes | Settings, user preferences |
| Database | ✅ Yes | ✅ Yes | Large/complex data |

---

## Memory Leaks - Common Scenarios

### 1. Anonymous Inner Class
```kotlin
// ❌ BAD - Holds Activity reference
handler.postDelayed(object : Runnable {
    override fun run() { updateUI() }
}, 60000)

// ✅ GOOD - Static class with WeakReference
class SafeRunnable(activity: MyActivity) : Runnable {
    private val weakRef = WeakReference(activity)
    override fun run() {
        weakRef.get()?.takeIf { !it.isDestroyed }?.updateUI()
    }
}
```

### 2. Unregistered Listeners
```kotlin
// ❌ BAD - Never unregister
locationManager.requestLocationUpdates(..., listener)

// ✅ GOOD - Unregister in onStop
override fun onStop() {
    super.onStop()
    locationManager.removeUpdates(listener)
}
```

### 3. Fragment Binding Leak
```kotlin
// ❌ BAD - View reference not cleared
override fun onDestroyView() {
    super.onDestroyView()
    // forgot: binding = null
}

// ✅ GOOD - Clear binding reference
override fun onDestroyView() {
    super.onDestroyView()
    _binding = null  // CRITICAL!
}
```

### 4. Coroutine Scope Issues
```kotlin
// ❌ BAD - GlobalScope lives forever
GlobalScope.launch { ... }

// ❌ BAD - Using this instead of viewLifecycleOwner
viewModel.data.observe(this) { ... }

// ✅ GOOD - lifecycleScope
lifecycleScope.launch { ... }

// ✅ GOOD - viewLifecycleOwner for UI
viewModel.data.observe(viewLifecycleOwner) { ... }
```

---

## Context Usage Guide

| Context | Use For | Don't Use For |
|---------|---------|---------------|
| `Activity` | Starting activities, Dialogs, Themed layouts | Long-lived objects |
| `Application` | Singletons, Databases, Services | UI operations |
| `Fragment.requireContext()` | Fragment UI operations | Assumes attached state |
| `view.context` | View-specific operations | - |

**Rule**: If object lives longer than Activity, use `applicationContext`.

---

## Lifecycle-Aware Components

### Basic Observer
```kotlin
class MyObserver : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) { /* start updates */ }
    override fun onStop(owner: LifecycleOwner) { /* stop updates */ }
}

// Register
lifecycle.addObserver(MyObserver())
```

### repeatOnLifecycle (Flow Collection)
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.data.collect { /* update UI */ }
    }
}
// Automatically stops collecting when not STARTED
```

### flowWithLifecycle
```kotlin
viewModel.data
    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
    .collect { /* update UI */ }
```

---

## SavedStateHandle Quick Guide

```kotlin
class MyViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Read saved value
    val userId: String = savedStateHandle["user_id"] ?: ""

    // Save value (survives process death)
    fun updateQuery(query: String) {
        savedStateHandle["query"] = query
    }

    // Observe as LiveData
    val queryLiveData: LiveData<String> = savedStateHandle.getLiveData("query")
}

// Factory for SavedStateHandle
class Factory(owner: SavedStateRegistryOwner) : 
    AbstractSavedStateViewModelFactory(owner, null) {
    
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        return MyViewModel(handle) as T
    }
}
```

---

## Fragment Transactions

```kotlin
// Basic transaction
parentFragmentManager.commit {
    setReorderingAllowed(true)  // Optimizes transactions
    replace<MyFragment>(R.id.container)
    addToBackStack("tag")  // Enable back navigation
}

// Safe transaction (check state)
if (!parentFragmentManager.isStateSaved) {
    parentFragmentManager.commit { ... }
}
```

### Important Fragment Rules
1. Always use `viewLifecycleOwner` for UI observations
2. Clear `_binding` in `onDestroyView()`
3. Use `setReorderingAllowed(true)` for better performance
4. Check `isStateSaved` before committing transactions

---

## Interview Quick Answers

### "Difference between onCreate and onStart?"
- `onCreate`: Called once, initialize ViewModel, restore state
- `onStart`: Called multiple times, UI visible but not interactive

### "How does ViewModel survive configuration changes?"
- ViewModel is stored in `ViewModelStore` tied to Activity/Fragment
- System retains `ViewModelStore` during config changes
- New Activity instance retrieves same ViewModel from store

### "When is onDestroy not called?"
- When system kills app due to low memory
- When process is terminated by system
- Don't rely on it for critical cleanup

### "Process death vs configuration change?"
- **Config change**: Activity recreated, ViewModel survives, state in memory
- **Process death**: Everything destroyed, need `SavedStateHandle` or persistence

### "How to detect memory leaks?"
- LeakCanary library (automatic detection)
- Android Studio Profiler (heap dump analysis)
- StrictMode (some leak detection)

### "viewLifecycleOwner vs lifecycle?"
- `lifecycle`: Fragment lifecycle (onAttach to onDetach)
- `viewLifecycleOwner`: View lifecycle (onCreateView to onDestroyView)
- Use `viewLifecycleOwner` for UI data to avoid crashes after view destroyed

---

## Checklist for Writing Lifecycle-Safe Code

- [ ] Clear Fragment bindings in `onDestroyView()`
- [ ] Use `viewLifecycleOwner` for UI observations
- [ ] Unregister listeners/receivers in `onStop()` or `onDestroy()`
- [ ] Cancel coroutines in `onCleared()` or use `lifecycleScope`
- [ ] Use `WeakReference` for callbacks that outlive Activity
- [ ] Save transient UI state in `onSaveInstanceState()` or `SavedStateHandle`
- [ ] Use `applicationContext` for singletons and long-lived objects
- [ ] Check `isStateSaved` before fragment transactions
- [ ] Use `repeatOnLifecycle` for Flow collection
- [ ] Handle process death with `SavedStateHandle` or persistence

---

## Resources

- [Android Lifecycle Guide](https://developer.android.com/guide/components/activities/activity-lifecycle)
- [ViewModel Overview](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [Saved State Module](https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate)
- [Lifecycle-Aware Components](https://developer.android.com/topic/libraries/architecture/lifecycle)

---

**Good luck with your Android interview! 🚀**
