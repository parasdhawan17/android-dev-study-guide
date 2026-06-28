# Data Classes

DAY 1-2: Core Kotlin Concepts - Data Classes

Data classes are Kotlin's way of creating classes that hold data.
They automatically generate: equals(), hashCode(), toString(), copy(), componentN()

## 1. Basic Data Class

Perfect for model/data objects in Android (API responses, UI state, entities)

````kotlin
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val isActive: Boolean = true  // Default parameter
)
````

## 2. Data Class With Nullable Properties

Common in real-world scenarios where some fields might be missing

````kotlin
data class Product(
    val id: String,
    val name: String,
    val description: String?,  // Nullable
    val price: Double,
    val imageUrl: String? = null  // Nullable with default
)
````

## 3. Data Class In Android: UI State

Typical pattern for ViewModel state in MVVM architecture

````kotlin
data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null
)
````

## 4. Data Class Destructuring

Extract properties into variables - useful for returning multiple values

````kotlin
data class NetworkResult<T>(
    val data: T,
    val statusCode: Int,
    val headers: Map<String, String>
)

fun main() {
    println("=== DATA CLASSES DEMO ===\n")
    
    // 1. Creating instances
    val user1 = User(id = 1, name = "Alice", email = "alice@example.com")
    val user2 = User(id = 2, name = "Bob", email = "bob@example.com", isActive = false)
    
    // 2. toString() - automatically generated
    println("1. toString() output:")
    println("   $user1")
    println()
    
    // 3. equals() and hashCode() - value equality, not reference
    val user1Copy = User(id = 1, name = "Alice", email = "alice@example.com")
    println("2. equals() comparison:")
    println("   user1 == user1Copy: ${user1 == user1Copy}")  // true
    println("   user1 === user1Copy: ${user1 === user1Copy}") // false (different objects)
    println()
    
    // 4. copy() - create modified copies (IMMUTABILITY pattern)
    // CRITICAL for Android: Use copy() to update state in ViewModels
    val updatedUser = user1.copy(name = "Alice Smith", isActive = false)
    println("3. copy() - create modified copy:")
    println("   Original: $user1")
    println("   Updated:  $updatedUser")
    println()
    
    // 5. componentN() destructuring
    val (id, name, email, isActive) = user1
    println("4. Destructuring:")
    println("   ID: $id, Name: $name, Email: $email, Active: $isActive")
    println()
    
    // 6. Practical Android pattern: State updates with copy()
    var uiState = ProfileUiState(isLoading = true)
    println("5. Android UI State Pattern:")
    println("   Initial: $uiState")
    
    // Simulate successful data load
    uiState = uiState.copy(
        isLoading = false,
        user = user1
    )
    println("   Success: $uiState")
    
    // Simulate error
    uiState = uiState.copy(
        isLoading = false,
        errorMessage = "Network error"
    )
    println("   Error:   $uiState")
    println()
    
    // 7. Common interview question: Why use data classes over regular classes?
    println("6. KEY INTERVIEW POINTS:")
    println("   - Auto-generated equals()/hashCode() for proper value comparison")
    println("   - copy() enables immutable state updates (crucial for Compose/Flow)")
    println("   - toString() helps debugging")
    println("   - Destructuring support for clean syntax")
    println()
    
    // 8. Limitations
    println("7. LIMITATIONS:")
    println("   - Cannot be open/abstract/sealed")
    println("   - Primary constructor must have at least one property")
    println("   - Cannot extend other classes (can implement interfaces)")
}
````

## Interview Questions To Expect

Q: "What's the difference between == and === in Kotlin with data classes?"
A: == compares values (uses equals()), === compares references

Q: "When would you use copy() in Android development?"
A: In ViewModels when updating UI state immutably - state.copy(updatedProperty = value)

Q: "Can data classes inherit from other classes?"
A: No, they can't extend classes but can implement interfaces
