# Day 1-2: Core Kotlin Concepts - Quick Reference

## Data Classes

```kotlin
data class User(
    val id: Int,
    val name: String,
    val email: String = ""  // Default value
)

// Auto-generated: equals(), hashCode(), toString(), copy(), componentN()

// Usage
val user = User(1, "Alice", "alice@example.com")
val updated = user.copy(name = "Alice Smith")  // Immutability pattern

// Destructuring
val (id, name, email) = user
```

**Key Points:**
- Primary constructor must have at least one `val`/`var` property
- Cannot be `open`, `abstract`, or `sealed`
- `copy()` is essential for immutable state updates in Android
- Use for: API models, UI state, database entities

---

## Sealed Classes

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// Exhaustive when - no else needed!
val message = when (result) {
    is Result.Success -> "✓ ${result.data}"
    is Result.Error -> "✗ ${result.message}"
    Result.Loading -> "⟳ Loading..."
}
```

**Key Points:**
- All subclasses known at compile time
- Compiler enforces exhaustive `when` expressions
- Perfect for: UI states, API results, navigation events
- Each subclass can have different data

---

## Extension Functions

```kotlin
// Add function to existing class
fun String.isValidEmail(): Boolean {
    return matches(Regex("..."))
}

// Usage
"test@example.com".isValidEmail()  // true

// Extension properties
val String.isPalindrome: Boolean
    get() = this == reversed()
```

**Key Points:**
- Static dispatch (resolved at compile time)
- Cannot access private members
- Don't actually modify the class
- Common Android uses: View visibility, Context utilities, String validation

---

## Higher-Order Functions & Lambdas

```kotlin
// Function taking lambda
fun process(items: List<T>, operation: (T) -> R): List<R> {
    return items.map(operation)
}

// Lambda syntax
val sum = { a: Int, b: Int -> a + b }
val square: (Int) -> Int = { it * it }  // Single param = 'it'

// Collection operations
numbers
    .filter { it > 0 }      // Keep positive
    .map { it * 2 }          // Transform
    .sortedBy { it }         // Sort
    .take(5)                 // First 5

// Scope functions
obj.let { it }      // Transform + null check
obj.apply { }        // Configure, return receiver
obj.run { }          // Configure, return result
obj.also { }         // Side effect, return receiver
with(obj) { }        // Non-extension version of run
```

**Key Points:**
- Functions are first-class citizens
- Lambdas can capture variables (closures)
- Scope functions differ in what they return and how they reference receiver
- `asSequence()` for large datasets (lazy evaluation)

---

## Null Safety

```kotlin
// Types
var nonNull: String = "value"      // Cannot be null
var nullable: String? = null       // Can be null

// Safe call ?.
val length: Int? = nullable?.length  // null if nullable is null

// Elvis ?:
val lengthOrZero = nullable?.length ?: 0
val result = nullable ?: return      // Early return pattern

// Not-null assertion !! (AVOID if possible!)
val risky = nullable!!               // Throws NPE if null

// Safe cast as?
val string: String? = obj as? String

// let for null-check
nullable?.let { safeValue ->
    // safeValue is non-null here
}

// Late initialization
lateinit var name: String            // Promise to set before use
if (::name.isInitialized) { }        // Check before access

val lazyValue: String by lazy {      // Initialize on first access
    expensiveComputation()
}

// Smart cast
if (nullable != null) {
    // nullable is non-null in this block
}
```

**Key Points:**
- `?.` - call if not null, return null otherwise
- `?:` - default value if null
- `!!` - force non-null (risky, avoid in production)
- Smart cast works for local vars, not mutable properties
- Platform types (from Java) - treat as nullable for safety

---

## Generics & Variance

```kotlin
// Invariant (default) - both read and write
class Box<T>(var item: T) {
    fun get(): T = item
    fun set(value: T) { item = value }
}

// Covariant (out) - only produces T
interface Producer<out T> {
    fun produce(): T
    // Cannot: fun consume(item: T)
}
// Producer<Cat> is Producer<Animal>

// Contravariant (in) - only consumes T
interface Consumer<in T> {
    fun consume(item: T)
    // Cannot: fun produce(): T
}
// Consumer<Animal> is Consumer<Cat>
```

**PECS Principle:**
- **P**roducer **E**xtends (**out**) - produces T, can use subtype
- **C**onsumer **S**uper (**in**) - consumes T, can use supertype

**Memory Trick:**
- `out` = produces/outputs T → Covariant (Sub is Super)
- `in` = consumes/inputs T → Contravariant (Super is Sub)

**Android Examples:**
- `List<out E>` - read-only, covariant
- `MutableList<E>` - read-write, invariant
- `LiveData<out T>` - produces, covariant
- `Observer<in T>` - consumes, contravariant
- `Comparator<in T>` - compares, contravariant

---

## Common Android Patterns

### ViewModel State (Sealed Class + Data Class)
```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class MyViewModel : ViewModel() {
    private val _state = MutableStateFlow<UiState<User>>(UiState.Loading)
    val state: StateFlow<UiState<User>> = _state.asStateFlow()
    
    fun loadUser() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val user = repository.getUser()
                _state.value = UiState.Success(user)
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// In Compose/Activity
when (val state = viewModel.state.collectAsState().value) {
    is UiState.Loading -> LoadingSpinner()
    is UiState.Success -> UserProfile(state.data)
    is UiState.Error -> ErrorMessage(state.message)
}
```

### Extension Functions for Android
```kotlin
// View extensions
fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }
fun View.isVisible(): Boolean = visibility == View.VISIBLE
fun View.onClick(action: () -> Unit) = setOnClickListener { action() }

// Context extensions  
fun Context.dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
fun Context.showToast(message: String) = 
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

// String extensions
fun String.isValidEmail(): Boolean = 
    matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))

fun String?.orEmpty(default: String = ""): String = this ?: default
```

### Null Safety Patterns
```kotlin
// Safe API response handling
val user = api.getUser()?.let { dto ->
    User(dto.id, dto.name.orEmpty())
} ?: return Result.Error("Failed to load user")

// Chain of operations
val displayName = user
    ?.profile
    ?.name
    ?.takeIf { it.isNotBlank() }
    ?: "Anonymous"

// Elvis with return/throw
val importantValue = nullable ?: throw IllegalStateException("Required value missing")
val config = nullableConfig ?: return
```

---

## Quick Interview Checklist

| Topic | Key Points |
|-------|------------|
| **Data Class** | copy(), immutability, destructuring, componentN() |
| **Sealed Class** | Exhaustive when, compile-time sealed hierarchy |
| **Extension** | Static dispatch, receiver = 'this' or 'it' |
| **Lambda** | { params -> body }, last expression = return |
| **let/run/apply/also/with** | Return type, receiver reference (this/it) |
| **?.** | Safe call - null if receiver null |
| **?:** | Elvis - default value if null |
| **!!** | Force non-null - throws NPE |
| **out (covariant)** | Produces T, Box<Sub> is Box<Super> |
| **in (contravariant)** | Consumes T, Box<Super> is Box<Sub> |

---

## Common Interview Questions

1. **Data Classes:**
   - What's the difference between `==` and `===`?
   - When to use `copy()` in Android?
   - Can data classes inherit from other classes?

2. **Sealed Classes:**
   - Sealed class vs Enum?
   - What's the benefit of exhaustive when?
   - How do sealed classes help with ViewModel state?

3. **Extension Functions:**
   - How do they work under the hood?
   - Extension vs member function priority?
   - Can they access private members?

4. **Null Safety:**
   - `?.` vs `?:` vs `!!`?
   - How does smart casting work?
   - What's a platform type?

5. **Variance:**
   - `in` vs `out` vs no modifier?
   - Why is List covariant but MutableList invariant?
   - PECS principle?
