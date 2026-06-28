# Practice Exercises

````kotlin
/"""
DAY 1-2: Core Kotlin Concepts - Practice Exercises

Complete these exercises to master the concepts. Each section has 
questions ranging from easy to hard. Try to solve them before checking answers.
"""
````

═══════════════════════════════════════════════════════════════════════════════
EXERCISE 1: DATA CLASSES
═══════════════════════════════════════════════════════════════════════════════

TASK 1.1 (Easy):
Create a data class representing a GitHub Repository with:
- id (Long)
- name (String)
- owner (String)
- stars (Int, default 0)
- isFork (Boolean, default false)

YOUR SOLUTION:
data class Repository(...)

TASK 1.2 (Medium):
Create a data class representing UI State for a search screen with:
- query (String)
- results (List&lt;String&gt;)
- isLoading (Boolean)
- error (String?, nullable)

Then write a function that updates this state immutably when:
a) User types new query
b) Search results arrive
c) Error occurs
d) Loading starts

YOUR SOLUTION:
data class SearchUiState(...)
fun updateQuery(currentState: SearchUiState, newQuery: String): SearchUiState = ...
etc.

TASK 1.3 (Hard):
Given a list of Users, find duplicates by email and return unique list
keeping the one with latest 'lastModified' timestamp.
Hint: Use groupBy, maxByOrNull

````kotlin
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
````

═══════════════════════════════════════════════════════════════════════════════
EXERCISE 2: SEALED CLASSES
═══════════════════════════════════════════════════════════════════════════════

TASK 2.1 (Easy):
Create a sealed class called ApiResponse that represents:
- Loading
- Success with data (String)
- Error with message (String) and code (Int)
- NetworkError (object - no additional data)

YOUR SOLUTION:
sealed class ApiResponse { ... }

TASK 2.2 (Medium):
Write a function that takes ApiResponse and returns a user-friendly String:
- Loading: "Loading..."
- Success: "Loaded: ${data}"
- Error: "Error ${code}: ${message}"
- NetworkError: "No internet connection"

Use exhaustive when expression.

YOUR SOLUTION:
fun formatResponse(response: ApiResponse): String = ...

TASK 2.3 (Hard):
Create a state machine for a Payment flow using sealed classes.
States: Idle, Processing, Success, Failed

Each state has relevant data:
- Processing: transactionId
- Success: receiptNumber, amount
- Failed: error, canRetry, retryCount

Write a function canRetryPayment(state) that returns true only if
state is Failed with canRetry=true and retryCount &lt; 3

YOUR SOLUTION:
sealed class PaymentState { ... }
fun canRetryPayment(state: PaymentState): Boolean = ...

═══════════════════════════════════════════════════════════════════════════════
EXERCISE 3: EXTENSION FUNCTIONS
═══════════════════════════════════════════════════════════════════════════════

TASK 3.1 (Easy):
Write extension functions on String:
- isValidPhone(): Boolean - checks if string matches phone pattern
- truncate(maxLength: Int): String - truncates with "..." if too long
- toSlug(): String - "Hello World" → "hello-world"

YOUR SOLUTION:
fun String.isValidPhone(): Boolean = ...
fun String.truncate(maxLength: Int): String = ...
fun String.toSlug(): String = ...

TASK 3.2 (Medium):
Write extension functions for Android-style operations:
- Int.dpToPx(density: Float): Int - convert dp to pixels
- Long.toTimeAgo(): String - convert timestamp to "2 hours ago", "just now"
- List&lt;T&gt;.paginate(page: Int, pageSize: Int): List&lt;T&gt; - get specific page

YOUR SOLUTION:
fun Int.dpToPx(density: Float): Int = ...
fun Long.toTimeAgo(): String = ...
fun &lt;T&gt; List&lt;T&gt;.paginate(page: Int, pageSize: Int): List&lt;T&gt; = ...

TASK 3.3 (Hard):
Write extension functions for Result type operations:
- Result&lt;T&gt;.onSuccess(action: (T) -&gt; Unit): Result&lt;T&gt;
- Result&lt;T&gt;.onFailure(action: (Throwable) -&gt; Unit): Result&lt;T&gt;
- Result&lt;T&gt;.map(transform: (T) -&gt; R): Result&lt;R&gt;
- Result&lt;T&gt;.flatMap(transform: (T) -&gt; Result&lt;R&gt;): Result&lt;R&gt;

These are like the standard library but implement yourself!

YOUR SOLUTION:
inline fun &lt;T&gt; Result&lt;T&gt;.onSuccess(action: (T) -&gt; Unit): Result&lt;T&gt; = ...
etc.

═══════════════════════════════════════════════════════════════════════════════
EXERCISE 4: HIGHER-ORDER FUNCTIONS &amp; LAMBDAS
═══════════════════════════════════════════════════════════════════════════════

TASK 4.1 (Easy):
Write a higher-order function repeatAction(times: Int, action: () -&gt; Unit)
that executes the action 'times' number of times.

YOUR SOLUTION:
fun repeatAction(times: Int, action: () -&gt; Unit) { ... }

TASK 4.2 (Medium):
Write a function processList that takes:
- list: List&lt;T&gt;
- filter: (T) -&gt; Boolean
- transform: (T) -&gt; R
- sortBy: (R) -&gt; Comparable&lt;*&gt;

Returns: List&lt;R&gt; that is filtered, transformed, then sorted.

YOUR SOLUTION:
fun &lt;T, R&gt; processList(
list: List&lt;T&gt;,
filter: (T) -&gt; Boolean,
transform: (T) -&gt; R,
sortBy: (R) -&gt; Comparable&lt;*&gt;
): List&lt;R&gt; = ...

TASK 4.3 (Hard):
Implement a simple EventBus using higher-order functions:
- subscribe(eventType: Class&lt;T&gt;, handler: (T) -&gt; Unit): Subscription
- publish(event: T)
- unsubscribe(subscription: Subscription)

Support multiple subscribers per event type.

YOUR SOLUTION:
class EventBus { ... }

═══════════════════════════════════════════════════════════════════════════════
EXERCISE 5: NULL SAFETY
═══════════════════════════════════════════════════════════════════════════════

TASK 5.1 (Easy):
Rewrite these Java-style null checks as idiomatic Kotlin:

Java:

```java
String name = getName();
int length;
if (name != null) {
    length = name.length();
} else {
    length = 0;
}
```

Do it in ONE line using null safety operators.

YOUR SOLUTION:

```kotlin
val length = ...
```

TASK 5.2 (Medium):
Write a function fetchUserData(userId: String?): Result&lt;User&gt; that:
- Returns error result if userId is null or blank
- Returns error if user lookup fails
- Returns success with user data otherwise

Handle all null cases elegantly without if-null chains.

````kotlin
data class User2(val id: String, val name: String)
````

YOUR SOLUTION:

```kotlin
fun fetchUserData(userId: String?): Result&lt;User2&gt; { ... }
```

TASK 5.3 (Hard):
You're given a deeply nested data structure. Safely access the user's
profile picture URL with all null checks:

Company → Department → Team → Member → Profile → avatarUrl

Each can be null. Return empty string if any step is null.
Do it in ONE expression.

YOUR SOLUTION:
fun getAvatarUrl(company: Company?): String = ...

═══════════════════════════════════════════════════════════════════════════════
EXERCISE 6: GENERICS &amp; VARIANCE
═══════════════════════════════════════════════════════════════════════════════

TASK 6.1 (Easy):
Create a generic class Cache&lt;K, V&gt; with:
- put(key: K, value: V)
- get(key: K): V?
- clear()

Use appropriate types and handle nulls.

YOUR SOLUTION:
class Cache&lt;K, V&gt; { ... }

TASK 6.2 (Medium):
Define these interfaces with proper variance:
- Reader&lt;out T&gt; - can read T
- Writer&lt;in T&gt; - can write T
- Store&lt;T&gt; - can read and write T (invariant)

Explain why each variance is appropriate.

YOUR SOLUTION:
interface Reader&lt;out T&gt; { ... }
interface Writer&lt;in T&gt; { ... }
interface Store&lt;T&gt; { ... }

TASK 6.3 (Hard):
Implement a type-safe Event system:
- Event&lt;T&gt; with data: T
- EventListener&lt;in T&gt; that handles Event&lt;T&gt;
- EventBus2 that registers listeners and dispatches events

Ensure that EventListener&lt;Animal&gt; can handle Event&lt;Cat&gt; events.
Hint: Use variance appropriately.

YOUR SOLUTION:
class Event&lt;T&gt;(val data: T)
interface EventListener&lt;in T&gt; { ... }
class EventBus2 { ... }

═══════════════════════════════════════════════════════════════════════════════
SOLUTIONS
═══════════════════════════════════════════════════════════════════════════════

Uncomment to see solutions after attempting yourself

```kotlin
// ─────────────────────────────────────────────────────────────────────────────
// SOLUTION 1.3: Remove duplicate users
fun removeDuplicateUsers(users: List&lt;User&gt;): List&lt;User&gt; {
return users
.groupBy { it.email }
.map { (_, usersWithSameEmail) -&gt;
usersWithSameEmail.maxByOrNull { it.lastModified }!!
}
}

// ─────────────────────────────────────────────────────────────────────────────
// SOLUTION 3.1: String extensions
fun String.isValidPhone(): Boolean {
return matches(Regex("^\\+?[1-9]\\d{1,14}$"))
}

fun String.truncate(maxLength: Int): String {
return if (length &lt;= maxLength) this else substring(0, maxLength - 3) + "..."
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
// return company?.let { c -&gt; c.department?.let { d -&gt; ... } } ?: ""
```

````kotlin
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
````

Helper for string repetition

````kotlin
operator fun String.times(n: Int): String = repeat(n)
````
