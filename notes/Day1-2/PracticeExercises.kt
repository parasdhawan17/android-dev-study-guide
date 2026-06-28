/"""
DAY 1-2: Core Kotlin Concepts - Practice Exercises

Complete these exercises to master the concepts. Each section has 
questions ranging from easy to hard. Try to solve them before checking answers.
"""

// ═══════════════════════════════════════════════════════════════════════════════
// EXERCISE 1: DATA CLASSES
// ═══════════════════════════════════════════════════════════════════════════════

/*
TASK 1.1 (Easy): 
Create a data class representing a GitHub Repository with:
- id (Long)
- name (String)
- owner (String)
- stars (Int, default 0)
- isFork (Boolean, default false)
*/

// YOUR SOLUTION:
// data class Repository(...)

/*
TASK 1.2 (Medium):
Create a data class representing UI State for a search screen with:
- query (String)
- results (List<String>)
- isLoading (Boolean)
- error (String?, nullable)

Then write a function that updates this state immutably when:
a) User types new query
b) Search results arrive
c) Error occurs
d) Loading starts
*/

// YOUR SOLUTION:
// data class SearchUiState(...)
// fun updateQuery(currentState: SearchUiState, newQuery: String): SearchUiState = ...
// etc.

/*
TASK 1.3 (Hard):
Given a list of Users, find duplicates by email and return unique list
keeping the one with latest 'lastModified' timestamp.
Hint: Use groupBy, maxByOrNull
*/

data class User(
    val id: String,
    val email: String,
    val name: String,
    val lastModified: Long
)

fun removeDuplicateUsers(users: List<User>): List<User> {
    // YOUR SOLUTION HERE
    TODO()
}


// ═══════════════════════════════════════════════════════════════════════════════
// EXERCISE 2: SEALED CLASSES
// ═══════════════════════════════════════════════════════════════════════════════

/*
TASK 2.1 (Easy):
Create a sealed class called ApiResponse that represents:
- Loading
- Success with data (String)
- Error with message (String) and code (Int)
- NetworkError (object - no additional data)
*/

// YOUR SOLUTION:
// sealed class ApiResponse { ... }

/*
TASK 2.2 (Medium):
Write a function that takes ApiResponse and returns a user-friendly String:
- Loading: "Loading..."
- Success: "Loaded: ${data}"
- Error: "Error ${code}: ${message}"
- NetworkError: "No internet connection"

Use exhaustive when expression.
*/

// YOUR SOLUTION:
// fun formatResponse(response: ApiResponse): String = ...

/*
TASK 2.3 (Hard):
Create a state machine for a Payment flow using sealed classes.
States: Idle, Processing, Success, Failed

Each state has relevant data:
- Processing: transactionId
- Success: receiptNumber, amount
- Failed: error, canRetry, retryCount

Write a function canRetryPayment(state) that returns true only if 
state is Failed with canRetry=true and retryCount < 3
*/

// YOUR SOLUTION:
// sealed class PaymentState { ... }
// fun canRetryPayment(state: PaymentState): Boolean = ...


// ═══════════════════════════════════════════════════════════════════════════════
// EXERCISE 3: EXTENSION FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════════

/*
TASK 3.1 (Easy):
Write extension functions on String:
- isValidPhone(): Boolean - checks if string matches phone pattern
- truncate(maxLength: Int): String - truncates with "..." if too long
- toSlug(): String - "Hello World" → "hello-world"
*/

// YOUR SOLUTION:
// fun String.isValidPhone(): Boolean = ...
// fun String.truncate(maxLength: Int): String = ...
// fun String.toSlug(): String = ...

/*
TASK 3.2 (Medium):
Write extension functions for Android-style operations:
- Int.dpToPx(density: Float): Int - convert dp to pixels
- Long.toTimeAgo(): String - convert timestamp to "2 hours ago", "just now"
- List<T>.paginate(page: Int, pageSize: Int): List<T> - get specific page
*/

// YOUR SOLUTION:
// fun Int.dpToPx(density: Float): Int = ...
// fun Long.toTimeAgo(): String = ...
// fun <T> List<T>.paginate(page: Int, pageSize: Int): List<T> = ...

/*
TASK 3.3 (Hard):
Write extension functions for Result type operations:
- Result<T>.onSuccess(action: (T) -> Unit): Result<T>
- Result<T>.onFailure(action: (Throwable) -> Unit): Result<T>
- Result<T>.map(transform: (T) -> R): Result<R>
- Result<T>.flatMap(transform: (T) -> Result<R>): Result<R>

These are like the standard library but implement yourself!
*/

// YOUR SOLUTION:
// inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> = ...
// etc.


// ═══════════════════════════════════════════════════════════════════════════════
// EXERCISE 4: HIGHER-ORDER FUNCTIONS & LAMBDAS
// ═══════════════════════════════════════════════════════════════════════════════

/*
TASK 4.1 (Easy):
Write a higher-order function repeatAction(times: Int, action: () -> Unit)
that executes the action 'times' number of times.
*/

// YOUR SOLUTION:
// fun repeatAction(times: Int, action: () -> Unit) { ... }

/*
TASK 4.2 (Medium):
Write a function processList that takes:
- list: List<T>
- filter: (T) -> Boolean
- transform: (T) -> R
- sortBy: (R) -> Comparable<*>

Returns: List<R> that is filtered, transformed, then sorted.
*/

// YOUR SOLUTION:
// fun <T, R> processList(
//     list: List<T>,
//     filter: (T) -> Boolean,
//     transform: (T) -> R,
//     sortBy: (R) -> Comparable<*>
// ): List<R> = ...

/*
TASK 4.3 (Hard):
Implement a simple EventBus using higher-order functions:
- subscribe(eventType: Class<T>, handler: (T) -> Unit): Subscription
- publish(event: T)
- unsubscribe(subscription: Subscription)

Support multiple subscribers per event type.
*/

// YOUR SOLUTION:
// class EventBus { ... }


// ═══════════════════════════════════════════════════════════════════════════════
// EXERCISE 5: NULL SAFETY
// ═══════════════════════════════════════════════════════════════════════════════

/*
TASK 5.1 (Easy):
Rewrite these Java-style null checks as idiomatic Kotlin:

Java:
  String name = getName();
  int length;
  if (name != null) {
      length = name.length();
  } else {
      length = 0;
  }

Do it in ONE line using null safety operators.
*/

// YOUR SOLUTION:
// val length = ...

/*
TASK 5.2 (Medium):
Write a function fetchUserData(userId: String?): Result<User> that:
- Returns error result if userId is null or blank
- Returns error if user lookup fails
- Returns success with user data otherwise

Handle all null cases elegantly without if-null chains.
*/

data class User2(val id: String, val name: String)

// YOUR SOLUTION:
// fun fetchUserData(userId: String?): Result<User2> { ... }

/*
TASK 5.3 (Hard):
You're given a deeply nested data structure. Safely access the user's 
profile picture URL with all null checks:

Company → Department → Team → Member → Profile → avatarUrl

Each can be null. Return empty string if any step is null.
Do it in ONE expression.
*/

// YOUR SOLUTION:
// fun getAvatarUrl(company: Company?): String = ...


// ═══════════════════════════════════════════════════════════════════════════════
// EXERCISE 6: GENERICS & VARIANCE
// ═══════════════════════════════════════════════════════════════════════════════

/*
TASK 6.1 (Easy):
Create a generic class Cache<K, V> with:
- put(key: K, value: V)
- get(key: K): V?
- clear()

Use appropriate types and handle nulls.
*/

// YOUR SOLUTION:
// class Cache<K, V> { ... }

/*
TASK 6.2 (Medium):
Define these interfaces with proper variance:
- Reader<out T> - can read T
- Writer<in T> - can write T
- Store<T> - can read and write T (invariant)

Explain why each variance is appropriate.
*/

// YOUR SOLUTION:
// interface Reader<out T> { ... }
// interface Writer<in T> { ... }
// interface Store<T> { ... }

/*
TASK 6.3 (Hard):
Implement a type-safe Event system:
- Event<T> with data: T
- EventListener<in T> that handles Event<T>
- EventBus2 that registers listeners and dispatches events

Ensure that EventListener<Animal> can handle Event<Cat> events.
Hint: Use variance appropriately.
*/

// YOUR SOLUTION:
// class Event<T>(val data: T)
// interface EventListener<in T> { ... }
// class EventBus2 { ... }


// ═══════════════════════════════════════════════════════════════════════════════
// SOLUTIONS
// ═══════════════════════════════════════════════════════════════════════════════

// Uncomment to see solutions after attempting yourself

/*
// ─────────────────────────────────────────────────────────────────────────────
// SOLUTION 1.3: Remove duplicate users
fun removeDuplicateUsers(users: List<User>): List<User> {
    return users
        .groupBy { it.email }
        .map { (_, usersWithSameEmail) ->
            usersWithSameEmail.maxByOrNull { it.lastModified }!!
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// SOLUTION 3.1: String extensions
fun String.isValidPhone(): Boolean {
    return matches(Regex("^\\+?[1-9]\\d{1,14}$"))
}

fun String.truncate(maxLength: Int): String {
    return if (length <= maxLength) this else substring(0, maxLength - 3) + "..."
}

fun String.toSlug(): String {
    return lowercase()
        .replace(Regex("[^a-z0-9\\s]"), "")
        .replace(Regex("\\s+"), "-")
        .trim('-')
}

// ─────────────────────────────────────────────────────────────────────────────
// SOLUTION 5.1: Idiomatic null check
val length = getName()?.length ?: 0

// ─────────────────────────────────────────────────────────────────────────────
// SOLUTION 5.3: Deep null-safe access
fun getAvatarUrl(company: Company?): String {
    return company?.department?.team?.member?.profile?.avatarUrl ?: ""
}
// Or with safe let:
// return company?.let { c -> c.department?.let { d -> ... } } ?: ""
*/


fun main() {
    println("DAY 1-2: Core Kotlin Concepts - Practice Exercises")
    println("=" * 50)
    println()
    println("Instructions:")
    println("1. Try to solve each exercise yourself first")
    println("2. Write your code in the YOUR SOLUTION sections")
    println("3. Check the solutions at the bottom (commented out)")
    println("4. Uncomment the solutions to verify your answers")
    println()
    println("Topics covered:")
    println("- Data Classes")
    println("- Sealed Classes")
    println("- Extension Functions")
    println("- Higher-Order Functions")
    println("- Null Safety")
    println("- Generics & Variance")
}

// Helper for string repetition
operator fun String.times(n: Int): String = repeat(n)
