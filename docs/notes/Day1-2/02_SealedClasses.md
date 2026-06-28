# Sealed Classes

DAY 1-2: Core Kotlin Concepts - Sealed Classes

Sealed classes represent restricted class hierarchies.
All subclasses must be declared in the same file/package.
PERFECT for: State management, API results, UI events

## 1. Basic Sealed Class

Use when you have a known, limited set of types

````kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()  // Singleton for state without data
}
````

## 2. Sealed Class For UI State (Android Pattern)

````kotlin
sealed class LoginUiState {
    object Initial : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val userId: String, val welcomeMessage: String) : LoginUiState()
    data class Error(val errorMessage: String, val canRetry: Boolean = true) : LoginUiState()
    data class ValidationError(val fieldErrors: Map<String, String>) : LoginUiState()
}
````

## 3. Sealed Class With State And Data

````kotlin
sealed class PaymentState {
    object Idle : PaymentState()
    data class Processing(val transactionId: String) : PaymentState()
    data class Completed(val receipt: Receipt) : PaymentState()
    data class Failed(val error: PaymentError, val remainingAttempts: Int) : PaymentState()
}

data class Receipt(val id: String, val amount: Double, val timestamp: Long)
data class PaymentError(val code: String, val description: String)
````

## 4. Sealed Class For Navigation Events

````kotlin
sealed class NavigationEvent {
    object Back : NavigationEvent()
    data class ToProductDetail(val productId: String) : NavigationEvent()
    data class ToCheckout(val cartItems: List<String>) : NavigationEvent()
    data class ToExternalUrl(val url: String) : NavigationEvent()
    data class ShowDialog(val title: String, val message: String) : NavigationEvent()
}

fun main() {
    println("=== SEALED CLASSES DEMO ===\n")
    
    // 1. Creating instances
    val successResult: Result<String> = Result.Success("Data loaded")
    val errorResult: Result<String> = Result.Error(
        exception = RuntimeException("Network error"),
        message = "Failed to load"
    )
    val loadingResult: Result<String> = Result.Loading
    
    // 2. WHEN expression with EXHAUSTIVE checking
    // The compiler knows all possible types - no else needed!
    println("1. EXHAUSTIVE when expressions:")
    handleResult(successResult)
    handleResult(errorResult)
    handleResult(loadingResult)
    println()
    
    // 3. UI State handling pattern
    println("2. UI State pattern in Android:")
    val states = listOf(
        LoginUiState.Initial,
        LoginUiState.Loading,
        LoginUiState.Success("user123", "Welcome back!"),
        LoginUiState.Error("Invalid credentials", canRetry = true),
        LoginUiState.ValidationError(mapOf("email" to "Invalid format"))
    )
    
    states.forEach { state ->
        renderLoginState(state)
    }
    println()
    
    // 4. Smart casting
    println("3. SMART CASTING:")
    val someResult: Result<String> = Result.Success("Hello")
    
    when (someResult) {
        is Result.Success -> {
            // Automatically cast to Success - no explicit cast needed!
            println("   Data: ${someResult.data}")
        }
        is Result.Error -> {
            // Automatically cast to Error
            println("   Error: ${someResult.message}")
        }
        Result.Loading -> {
            println("   Loading...")
        }
    }
    println()
    
    // 5. Comparison with enums
    println("4. SEALED CLASS vs ENUM:")
    println("   - Enum: Fixed instances (constants)")
    println("   - Sealed: Fixed TYPES, each can have different data")
    println("   - Sealed = Type safety + data per type")
    println()
    
    // 6. Why this matters for Android
    println("5. ANDROID USE CASES:")
    println("   - ViewModel StateFlow states")
    println("   - Repository API results")
    println("   - Navigation events")
    println("   - UI events (clicks, swipes)")
    println()
    
    // 7. Pattern matching with data extraction
    println("6. PATTERN MATCHING:")
    val paymentStates = listOf(
        PaymentState.Idle,
        PaymentState.Processing("txn-001"),
        PaymentState.Completed(Receipt("r-001", 99.99, System.currentTimeMillis())),
        PaymentState.Failed(PaymentError("INSUFFICIENT_FUNDS", "Not enough balance"), 2)
    )
    
    paymentStates.forEach { state ->
        val message = when (state) {
            is PaymentState.Completed -> "✓ Payment complete: ${state.receipt.id}"
            is PaymentState.Failed -> "✗ Failed (${state.error.code}). ${state.remainingAttempts} attempts left"
            is PaymentState.Processing -> "⟳ Processing ${state.transactionId}..."
            PaymentState.Idle -> "Waiting to start"
        }
        println("   $message")
    }
}
````

Helper function showing exhaustive when

````kotlin
fun <T> handleResult(result: Result<T>) {
    val message = when (result) {
        is Result.Success -> "✓ Got data: ${result.data}"
        is Result.Error -> "✗ Error: ${result.message}"
        Result.Loading -> "⟳ Loading..."
    }
    println("   $message")
}
````

Helper showing state rendering

````kotlin
fun renderLoginState(state: LoginUiState) {
    val description = when (state) {
        LoginUiState.Initial -> "Show empty form"
        LoginUiState.Loading -> "Show loading spinner"
        is LoginUiState.Success -> "Show dashboard for ${state.userId}"
        is LoginUiState.Error -> if (state.canRetry) "Show retry button" else "Show final error"
        is LoginUiState.ValidationError -> "Show field errors: ${state.fieldErrors}"
    }
    println("   $state -> $description")
}
````

## Interview Questions To Expect

Q: "When would you use a sealed class vs an enum?"
A: Sealed class when each state/type needs different data.
Enum when states are just constants without associated data.

Q: "What's the benefit of exhaustive when expressions?"
A: Compiler ensures you handle ALL cases. Adding a new subclass
causes compilation errors in all when expressions - forces updates.

Q: "How do sealed classes help with ViewModel state?"
A: Represent all UI states (Loading, Success, Error) as sealed types.
UI uses when(state) to render differently for each state.
Type-safe and compiler-checked.

Q: "Can sealed classes be extended outside the file?"
A: In Kotlin 1.5+: Yes, if declared in the same package and marked properly.
By default, subclasses must be in the same file (Kotlin 1.0-1.4).
Use 'sealed class Foo' in package, 'sealed interface' for more flexibility.
