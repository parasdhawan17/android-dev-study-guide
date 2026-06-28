/**
 * DAY 1-2: Core Kotlin Concepts - Null Safety
 * 
 * Kotlin's type system distinguishes nullable and non-nullable references.
 * "Null safety is not a feature, it's a lifestyle."
 */

// ============ 1. NULLABLE VS NON-NULLABLE TYPES ============

fun nullSafetyBasics() {
    var nonNull: String = "Cannot be null"  // Non-nullable type
    // nonNull = null  // COMPILE ERROR!
    
    var nullable: String? = "Can be null"   // Nullable type (?
    nullable = null  // OK!
}

// ============ 2. SAFE CALL OPERATOR ?. ============
fun safeCalls() {
    val name: String? = getNullableString()
    
    // Safe call - returns null if receiver is null, otherwise calls method
    val length: Int? = name?.length  // null if name is null, else length
    val uppercase: String? = name?.uppercase()
    
    // Chaining safe calls
    val city: String? = getUser()?.address?.city  // Any null returns null
    
    // Android example
    // val text = editText?.text?.toString() ?: ""
}

// ============ 3. ELVIS OPERATOR ?: ============
fun elvisOperator() {
    val name: String? = null
    
    // Provide default value if null
    val displayName: String = name ?: "Unknown"
    
    // Android pattern: get value or default
    val input = getNullableString() ?: ""
    val count = getNullableInt() ?: 0
    
    // Can chain with return/throw
    val value = getNullableString() ?: return
    val important = getNullableString() ?: throw IllegalArgumentException("Required!")
    
    // Elvis with safe call
    val length = getNullableString()?.length ?: 0
}

// ============ 4. NOT-NULL ASSERTION !! ============
fun notNullAssertion() {
    val name: String? = "Actually not null"
    
    // !! throws NPE if value is null
    val length: Int = name!!.length  // Risky! Avoid if possible
    
    // Use when YOU know better than the compiler
    // - After null check in different scope
    // - From platform types (Java interop)
    // - Test code where null shouldn't happen
    
    // Android example (avoid in production!)
    // val text = binding!!.textView.text  // Can crash - avoid!
}

// ============ 5. SAFE CAST as? ============
fun safeCasts() {
    val something: Any = "String"
    
    // Safe cast - returns null if cast fails
    val string: String? = something as? String
    val number: Int? = something as? Int  // null, not ClassCastException
    
    // Pattern: cast with elvis
    val definitelyString: String = something as? String ?: ""
}

// ============ 6. LET FUNCTION ============
fun letFunction() {
    val name: String? = getNullableString()
    
    // let executes block only if not null
    name?.let { safeName ->
        // safeName is smart-cast to non-nullable String here
        println("Length: ${safeName.length}")
        println("Uppercase: ${safeName.uppercase()}")
    }  // Only runs if name != null
    
    // Common Android pattern - execute if view exists
    // textView?.let { tv ->
    //     tv.text = "Hello"
    //     tv.visibility = View.VISIBLE
    // }
    
    // Alternative to if-not-null
    val length = name?.let { it.length } ?: 0
}

// ============ 7. PLATFORM TYPES (Java Interop) ============
fun platformTypes() {
    // Java methods return platform types (String!)
    // - Can be treated as nullable or non-nullable
    // - Compiler doesn't enforce null safety
    
    // val fromJava: String! = javaClass.getValue()
    // val safe: String? = fromJava  // OK
    // val risky: String = fromJava  // OK but might crash
    
    // Best practice: Treat all Java returns as nullable
}

// ============ 8. LATEINITIAL VAR ============
class LateInitDemo {
    lateinit var name: String  // Promise to initialize before use
    // lateinit var count: Int  // ERROR: only for reference types
    
    fun init() {
        name = "Initialized"
    }
    
    fun use() {
        if (::name.isInitialized) {  // Check if initialized
            println(name)
        }
        // println(name)  // Throws if not initialized
    }
    
    // Android use cases:
    // - lateinit var adapter: MyAdapter  (in Fragment/Activity)
    // - lateinit var viewModel: MyViewModel
    // - lateinit var binding: ActivityMainBinding
}

// ============ 9. LAZY DELEGATION ============
val expensiveValue: String by lazy {
    println("Computing expensive value...")
    "Result"
}

// ============ MAIN DEMONSTRATION ============
fun main() {
    println("=== NULL SAFETY DEMO ===\n")
    
    // 1. Basic null handling
    println("1. NULL HANDLING:")
    val maybeValue: String? = if (kotlin.random.Random.nextBoolean()) "Hello" else null
    
    println("   Value: $maybeValue")
    println("   Using ?.length: ${maybeValue?.length}")
    println("   Using ?: default: ${maybeValue ?: "DEFAULT"}")
    println()
    
    // 2. Smart casts
    println("2. SMART CASTS:")
    if (maybeValue != null) {
        // Automatically cast to non-nullable inside this block
        println("   Length (smart cast): ${maybeValue.length}")
        println("   Uppercase (smart cast): ${maybeValue.uppercase()}")
    }
    println()
    
    // 3. let block
    println("3. let() FUNCTION:")
    maybeValue?.let { value ->
        println("   Inside let: $value (length: ${value.length})")
    } ?: run {
        println("   Value was null, ran alternative block")
    }
    println()
    
    // 4. Android-style examples
    println("4. ANDROID PATTERNS:")
    
    // Pattern 1: Get string or default
    val editTextValue: String? = null  // Simulating user input
    val textToSave = editTextValue?.trim()?.takeIf { it.isNotEmpty() } ?: "Anonymous"
    println("   Input: '$editTextValue' -> Saved: '$textToSave'")
    
    // Pattern 2: Execute operation safely
    val userId: String? = "user123"
    userId?.let { id ->
        println("   Fetching data for user: $id")
    }
    
    // Pattern 3: Chain with early return
    fun processUserInput(input: String?): String {
        val cleanInput = input?.trim() ?: return "No input provided"
        return "Processing: $cleanInput"
    }
    println("   ${processUserInput("  hello  ")}")
    println("   ${processUserInput(null)}")
    println()
    
    // 5. TakeIf / TakeUnless
    println("5. takeIf / takeUnless:")
    val number = 42
    val evenOrNull = number.takeIf { it % 2 == 0 }
    val oddOrNull = number.takeUnless { it % 2 == 0 }
    println("   $number.takeIf { even } = $evenOrNull")
    println("   $number.takeUnless { even } = $oddOrNull")
    println()
    
    // 6. Common mistakes
    println("6. COMMON MISTAKES TO AVOID:")
    println("   BAD:  value!!.property  → Use ?: or ?.let instead")
    println("   BAD:  if (x != null) x!! else y  → Use x ?: y")
    println("   BAD:  x?.let { it } ?: y  → Use x ?: y")
    println("   GOOD: x?.property ?: default")
    println("   GOOD: x?.let { use(it) }")
    println()
    
    // 7. Lazy initialization
    println("7. LAZY INITIALIZATION:")
    println("   First access:")
    println("   $expensiveValue")
    println("   Second access (cached):")
    println("   $expensiveValue")
    println()
    
    // 8. Interview summary
    println("8. INTERVIEW SUMMARY:")
    println("   ?.   Safe call - returns null if receiver null")
    println("   ?:   Elvis - default value if null")
    println("   !!   Not-null assertion - throws NPE if null")
    println("   as?  Safe cast - null if cast fails")
    println("   ?.let{ } - Execute block if not null")
    println("   lateinit - Promise to initialize later")
    println("   by lazy - Initialize on first access")
}

// Helper functions for demo
data class User(val name: String?)
data class Address(val city: String?)
fun getUser(): User? = null
fun getNullableString(): String? = if (kotlin.random.Random.nextBoolean()) "value" else null
fun getNullableInt(): Int? = if (kotlin.random.Random.nextBoolean()) 42 else null

// ============ INTERVIEW QUESTIONS TO EXPECT ============
/*
Q: "What is the difference between ?., ?:, and !!?"
A:
- ?. (safe call): Calls method if not null, returns null if receiver null
- ?: (elvis): Returns left side if not null, right side if null
- !! (not-null assertion): Treats nullable as non-nullable, throws NPE if null

Q: "How does smart casting work?"
A: After null check (if (x != null)), compiler smart-casts to non-nullable.
Works for local vars, not mutable properties (could change between check and use).

Q: "When should you use !!?"
A: Rarely! Only when you're 100% certain it's not null but compiler doesn't know:
- Just checked in different scope
- Platform type from Java that's documented non-null
- Performance-critical code where null check overhead matters

Q: "What's a platform type?"
A: Type from Java code (String!). Can be used as nullable or non-nullable.
Compiler doesn't enforce null safety. Treat as nullable for safety.

Q: "lateinit vs lazy?"
A:
- lateinit var: Initialize later, manual. Check with ::prop.isInitialized.
  Used for DI, view binding, things set in lifecycle methods.
- lazy val: Initialize on first access, thread-safe by default.
  Used for expensive computations, singletons.

Q: "How to handle nullable chains?"
A: user?.address?.city ?: "Unknown"
If any part is null, returns default.

Q: "takeIf vs filter?"
A: takeIf on single value returns value or null.
filter on collection returns filtered collection.
*/
