# Extension Functions

DAY 1-2: Core Kotlin Concepts - Extension Functions

Add new functions to existing classes without inheriting or modifying them.
SUPER USEFUL for: Android View extensions, String utilities, Collection helpers

## 1. Basic Extension Functions

String extensions - Android interview favorite!

````kotlin
fun String.isValidEmail(): Boolean {
    return this.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
}

fun String.capitalizeWords(): String {
    return this.split(" ")
        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}

fun String.maskPassword(): String {
    return "*".repeat(this.length)
}
````

Android View extensions

````kotlin
fun android.view.View.hide() {
    this.visibility = android.view.View.GONE
}

fun android.view.View.show() {
    this.visibility = android.view.View.VISIBLE
}

fun android.view.View.invisible() {
    this.visibility = android.view.View.INVISIBLE
}

fun android.view.View.isVisible(): Boolean {
    return this.visibility == android.view.View.VISIBLE
}
````

## 2. Collection Extensions

Filter duplicates by key

````kotlin
fun <T, K> List<T>.distinctByKey(keySelector: (T) -> K): List<T> {
    val seen = mutableSetOf<K>()
    return this.filter { seen.add(keySelector(it)) }
}
````

Safe get with default

````kotlin
fun <T> List<T>.getOrDefault(index: Int, default: T): T {
    return if (index in this.indices) this[index] else default
}
````

Chunk into pairs (for RecyclerView span count 2)

````kotlin
fun <T> List<T>.toPairs(): List<Pair<T, T?>> {
    return this.chunked(2).map { 
        Pair(it[0], it.getOrNull(1)) 
    }
}
````

## 3. Context Extensions (Android)

These would work in Android app:
fun Context.dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
fun Context.pxToDp(px: Float): Float = px / resources.displayMetrics.density
fun Context.showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

## 4. Generic Extensions

Nullable extensions

````kotlin
fun <T> T?.orDefault(default: T): T = this ?: default

fun <T> T?.takeIfNotNull(): T? = this
````

Result type extensions

````kotlin
fun <T> Result<T>.getOrElse(default: T): T {
    return if (this.isSuccess) this.getOrThrow() else default
}
````

## 5. Extension Properties

Add properties too!

````kotlin
val String.isPalindrome: Boolean
    get() = this == this.reversed()

val Int.isEven: Boolean
    get() = this % 2 == 0

val Int.isOdd: Boolean
    get() = !this.isEven

val List<*>.isNotEmptyOrNull: Boolean
    get() = this != null && this.isNotEmpty()

fun main() {
    println("=== EXTENSION FUNCTIONS DEMO ===\n")
    
    // 1. String extensions
    println("1. String Extensions:")
    val email1 = "test@example.com"
    val email2 = "invalid-email"
    println("   '$email1' is valid: ${email1.isValidEmail()}")
    println("   '$email2' is valid: ${email2.isValidEmail()}")
    
    val name = "john doe"
    println("   '$name'.capitalizeWords() = '${name.capitalizeWords()}'")
    
    val password = "secret123"
    println("   '$password'.maskPassword() = '${password.maskPassword()}'")
    println()
    
    // 2. Extension properties
    println("2. Extension Properties:")
    println("   'radar'.isPalindrome = ${"radar".isPalindrome}")
    println("   'hello'.isPalindrome = ${"hello".isPalindrome}")
    println("   42.isEven = ${42.isEven}")
    println("   43.isOdd = ${43.isOdd}")
    println()
    
    // 3. Collection extensions
    println("3. Collection Extensions:")
    val numbers = listOf(1, 2, 2, 3, 3, 3, 4)
    println("   List: $numbers")
    println("   distinctByKey { it % 2 } = ${numbers.distinctByKey { it % 2 }}")
    println("   getOrDefault(10, -1) = ${numbers.getOrDefault(10, -1)}")
    
    val items = listOf("A", "B", "C", "D", "E")
    println("   toPairs() = ${items.toPairs()}")
    println()
    
    // 4. Scope and receiver (this)
    println("4. SCOPE AND RECEIVER:")
    println("   Inside extension function, 'this' refers to receiver object")
    
    // 5. Android examples (commented - would work in Android app)
    println("5. COMMON ANDROID EXTENSIONS:")
    println("   view.hide() / view.show() / view.isVisible()")
    println("   Context.dpToPx() / Context.showToast()")
    println("   String.isValidEmail() / String.toSpannable()")
    println()
    
    // 6. Null handling with extensions
    println("6. NULLABLE EXTENSIONS:")
    val nullable: String? = null
    println("   null.orDefault(\"fallback\") = '${nullable.orDefault("fallback")}'")
    println("   'real'.orDefault(\"fallback\") = '${"real".orDefault("fallback")}'")
    println()
    
    // 7. Interview key points
    println("7. INTERVIEW KEY POINTS:")
    println("   - Extensions are STATIC DISPATCH (resolved at compile time)")
    println("   - Don't actually modify the class - they're static utility methods")
    println("   - Can access public/protected members of receiver")
    println("   - Can be declared on nullable types too (String?.ext())")
    println("   - Cannot access private members of extended class")
    println()
    
    // 8. Demonstrate static dispatch
    println("8. STATIC DISPATCH EXAMPLE:")
    demonstrateStaticDispatch()
}
````

Extension function on nullable type

````kotlin
fun String?.safeLength(): Int = this?.length ?: 0
````

Static dispatch demonstration

````kotlin
open class Animal
class Dog : Animal()

fun Animal.speak() = "Animal sound"
fun Dog.speak() = "Woof!"

fun demonstrateStaticDispatch() {
    val dog: Dog = Dog()
    val animal: Animal = dog
    
    println("   dog.speak() = ${dog.speak()}")       // Woof!
    println("   animal.speak() = ${animal.speak()}") // Animal sound
    println("   ☝️ Resolved at COMPILE TIME based on declared type!")
}
````

## Interview Questions To Expect

Q: "How do extension functions work under the hood?"
A: Compiled to static methods. "String.ext()" becomes "ExtKt.ext(String)".
Static dispatch - resolved at compile time based on declared type.

Q: "Can you override a member function with an extension?"
A: No! Extensions are static dispatch. Member functions always win.
Extension only called if no member function with same signature exists.

Q: "Can extension functions access private members?"
A: No, only public and protected members of the receiver class.

Q: "Extension vs Utility class in Java?"
A: Extensions are cleaner syntax - "string.isValidEmail()" vs "StringUtils.isValidEmail(string)".
Better IDE support, more discoverable, feels like natural part of class.

Q: "When to use extension functions in Android?"
A: - View visibility helpers (show/hide)
- Context utilities (dpToPx, toast)
- String validation (isValidEmail, isPhoneNumber)
- Collection helpers
- Fragment/Activity navigation helpers
