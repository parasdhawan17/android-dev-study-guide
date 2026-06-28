# Higher Order Functions

DAY 1-2: Core Kotlin Concepts - Higher-Order Functions &amp; Lambdas

Higher-order functions: Functions that take functions as parameters or return functions.
Essential for: Functional programming, collection operations, coroutines, Compose UI

## 1. Function Types

Syntax: (ParameterType) -&gt; ReturnType

Simple function type

````kotlin
val multiply: (Int, Int) -> Int = { a, b -> a * b }
````

Function with no parameters

````kotlin
val getGreeting: () -> String = { "Hello, World!" }
````

Function returning Unit (void)

````kotlin
val printMessage: (String) -> Unit = { message -> println(message) }
````

Nullable function type

````kotlin
val maybeOperation: ((Int, Int) -> Int)? = null
````

## 2. Higher-Order Functions

Takes a function as parameter

````kotlin
fun calculate(a: Int, b: Int, operation: (Int, Int) -> Int): Int {
    return operation(a, b)
}
````

Returns a function

````kotlin
fun getMultiplier(factor: Int): (Int) -> Int {
    return { number -> number * factor }
}
````

Multiple function parameters

````kotlin
fun processData(
    data: List<String>,
    filter: (String) -> Boolean,
    transform: (String) -> String
): List<String> {
    return data.filter(filter).map(transform)
}
````

## 3. Common Higher-Order Functions (Standard Library)

````kotlin
fun main() {
    println("=== HIGHER-ORDER FUNCTIONS & LAMBDAS DEMO ===\n")
    
    // 1. Lambda syntax variations
    println("1. LAMBDA SYNTAX:")
    
    // Full syntax
    val sum1: (Int, Int) -> Int = { a: Int, b: Int -> a + b }
    // Type inference
    val sum2 = { a: Int, b: Int -> a + b }
    // Single parameter with implicit 'it'
    val square: (Int) -> Int = { it * it }
    // Multi-line lambda (last expression is return value)
    val calculate: (Int, Int) -> Int = { a, b ->
        val sum = a + b
        val doubled = sum * 2
        doubled  // Return value
    }
    
    println("   sum(5, 3) = ${sum1(5, 3)}")
    println("   square(4) = ${square(4)}")
    println("   calculate(2, 3) = ${calculate(2, 3)}")
    println()
    
    // 2. Using higher-order functions
    println("2. HIGHER-ORDER FUNCTIONS:")
    val result1 = calculate(10, 5) { a, b -> a + b }
    val result2 = calculate(10, 5) { a, b -> a - b }
    val result3 = calculate(10, 5) { a, b -> a * b }
    println("   calculate(10, 5, add) = $result1")
    println("   calculate(10, 5, subtract) = $result2")
    println("   calculate(10, 5, multiply) = $result3")
    println()
    
    // 3. Function returning function (closures!)
    println("3. FUNCTION RETURNING FUNCTION (CLOSURES):")
    val double = getMultiplier(2)
    val triple = getMultiplier(3)
    println("   double(5) = ${double(5)}")
    println("   triple(5) = ${triple(5)}")
    println("   Closure captures 'factor' from outer scope!")
    println()
    
    // 4. Collection operations - THE MOST IMPORTANT PART!
    println("4. COLLECTION OPERATIONS (Android Essential!):")
    val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    println("   Numbers: $numbers")
    
    // map: transform each element
    val doubled = numbers.map { it * 2 }
    println("   map { it * 2 } = $doubled")
    
    // filter: keep elements matching condition
    val evens = numbers.filter { it % 2 == 0 }
    println("   filter { it % 2 == 0 } = $evens")
    
    // reduce: combine to single value
    val sum = numbers.reduce { acc, n -> acc + n }
    println("   reduce { acc, n -> acc + n } = $sum")
    
    // fold: reduce with initial value
    val sumWithInitial = numbers.fold(100) { acc, n -> acc + n }
    println("   fold(100) { acc, n -> acc + n } = $sumWithInitial")
    
    // flatMap: map then flatten
    val nested = listOf(listOf(1, 2), listOf(3, 4), listOf(5, 6))
    val flattened = nested.flatMap { it }
    println("   flatMap: $nested -> $flattened")
    
    // groupBy: create map by key
    val byEvenOdd = numbers.groupBy { if (it % 2 == 0) "even" else "odd" }
    println("   groupBy { even/odd } = $byEvenOdd")
    
    // distinctBy: remove duplicates by key
    val items = listOf("apple", "banana", "apricot", "blueberry")
    val distinctByFirstLetter = items.distinctBy { it.first() }
    println("   distinctBy { first() } on $items = $distinctByFirstLetter")
    println()
    
    // 5. Chaining operations - Android pattern!
    println("5. CHAINING (Android API response processing):")
    data class User(val id: Int, val name: String, val age: Int)
    
    val users = listOf(
        User(1, "Alice", 25),
        User(2, "Bob", 17),
        User(3, "Charlie", 30),
        User(4, "Diana", 16)
    )
    
    val result = users
        .filter { it.age >= 18 }           // Only adults
        .sortedBy { it.name }               // Sort by name
        .map { it.name.uppercase() }         // Get uppercase names
        .take(2)                             // Take first 2
    
    println("   Users: $users")
    println("   Adults sorted, uppercase, take(2): $result")
    println()
    
    // 6. Common Android patterns
    println("6. ANDROID PATTERNS:")
    
    // Click listener replacement
    // view.setOnClickListener { doSomething() }
    
    // View binding with let
    // binding?.let { safeBinding ->
    //     safeBinding.textView.text = "Hello"
    // }
    
    // Coroutine launch
    // lifecycleScope.launch { loadData() }
    
    // Flow collection
    // viewModel.uiState.collect { state -> updateUi(state) }
    
    println("   setOnClickListener { } - lambda as callback")
    println("   let { } - execute block if not null")
    println("   run { } - execute block and return result")
    println("   apply { } - configure object, return same object")
    println("   also { } - side effect, return same object")
    println("   with(object) { } - operate on object, return result")
    println()
    
    // 7. Performance considerations
    println("7. PERFORMANCE NOTES:")
    println("   - Chained operations create intermediate collections")
    println("   - Use 'asSequence()' for large datasets (lazy evaluation)")
    println("   - Sequences avoid creating intermediate collections")
    println()
    
    // 8. COMPREHENSIVE COLLECTION OPERATORS REFERENCE
    comprehensiveCollectionOperators()
    
    // 9. Inline functions (covered in more detail later)
    println("9. INLINE FUNCTIONS:")
    println("   - 'inline' keyword removes lambda overhead")
    println("   - Standard library functions: let, run, apply, also, with, repeat")
    println("   - Use when function takes lambda parameter")
}
````

## 4. Comprehensive Collection Operators

````kotlin
fun comprehensiveCollectionOperators() {
    println("8. COMPREHENSIVE COLLECTION OPERATORS:\n")
    
    val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val words = listOf("apple", "banana", "cherry", "date", "elderberry")
    
    // ═══════════════════════════════════════════════════════════════
    // TRANSFORMATION OPERATORS (create new collections)
    // ═══════════════════════════════════════════════════════════════
    println("   A. TRANSFORMATION OPERATORS:")
    
    // map - transform each element
    val doubled = numbers.map { it * 2 }
    println("   • map { it * 2 }: $doubled")
    
    // mapIndexed - transform with index
    val withIndex = words.mapIndexed { index, word -> "$index: $word" }
    println("   • mapIndexed: $withIndex")
    
    // mapNotNull - transform, skip null results
    val parsedNumbers = listOf("1", "2", "abc", "4").mapNotNull { it.toIntOrNull() }
    println("   • mapNotNull toIntOrNull: $parsedNumbers")
    
    // flatMap - map then flatten (one-to-many transformation)
    val repeatPattern = listOf("A", "B").flatMap { char -> List(3) { char } }
    println("   • flatMap repeat: $repeatPattern")
    
    // flatten - flatten nested collections (without transformation)
    val nested = listOf(listOf(1, 2), listOf(3, 4), listOf(5, 6))
    val flat = nested.flatten()
    println("   • flatten: $nested → $flat")
    
    // zip - combine two lists into pairs
    val zipped = listOf("a", "b", "c").zip(listOf(1, 2, 3))
    println("   • zip: $zipped")
    
    // zipWithNext - pair adjacent elements
    val adjacent = numbers.zipWithNext { a, b -> a + b }
    println("   • zipWithNext { a + b }: $adjacent")
    println()
    
    // ═══════════════════════════════════════════════════════════════
    // FILTERING OPERATORS
    // ═══════════════════════════════════════════════════════════════
    println("   B. FILTERING OPERATORS:")
    
    // filter - keep matching elements
    val evens = numbers.filter { it % 2 == 0 }
    println("   • filter { even }: $evens")
    
    // filterNot - keep non-matching
    val notEvens = numbers.filterNot { it % 2 == 0 }
    println("   • filterNot { even }: $notEvens")
    
    // filterIndexed - filter with index
    val everyThird = numbers.filterIndexed { index, _ -> index % 3 == 0 }
    println("   • filterIndexed { index % 3 == 0 }: $everyThird")
    
    // filterIsInstance - filter by type
    val mixed = listOf(1, "two", 3, "four", 5.0)
    val stringsOnly = mixed.filterIsInstance<String>()
    println("   • filterIsInstance<String>: $stringsOnly")
    
    // filterNotNull - remove nulls
    val withNulls = listOf(1, null, 3, null, 5).filterNotNull()
    println("   • filterNotNull: $withNulls")
    
    // take - first N elements
    println("   • take(3): ${numbers.take(3)}")
    
    // takeLast - last N elements
    println("   • takeLast(3): ${numbers.takeLast(3)}")
    
    // takeWhile - take while condition true
    val takeWhileSmall = numbers.takeWhile { it < 5 }
    println("   • takeWhile { <5 }: $takeWhileSmall")
    
    // drop - skip first N
    println("   • drop(5): ${numbers.drop(5)}")
    
    // dropWhile - skip while condition true
    val dropWhileSmall = numbers.dropWhile { it < 5 }
    println("   • dropWhile { <5 }: $dropWhileSmall")
    
    // distinct - remove duplicates
    println("   • distinct on [1,1,2,2,3]: ${listOf(1, 1, 2, 2, 3).distinct()}")
    
    // distinctBy - remove duplicates by key
    val distinctByLength = listOf("a", "bb", "ccc", "dd", "e").distinctBy { it.length }
    println("   • distinctBy { length }: $distinctByLength")
    
    // slice - get specific indices
    println("   • slice(2..5): ${numbers.slice(2..5)}")
    println("   • slice(listOf(0, 2, 4)): ${numbers.slice(listOf(0, 2, 4))}")
    
    // chunk - split into sublists
    val chunked = numbers.chunked(3)
    println("   • chunked(3): $chunked")
    
    // windowed - sliding windows
    val windows = numbers.windowed(size = 3, step = 1)
    println("   • windowed(size=3, step=1): $windows")
    println()
    
    // ═══════════════════════════════════════════════════════════════
    // AGGREGATION OPERATORS (reduce to single value)
    // ═══════════════════════════════════════════════════════════════
    println("   C. AGGREGATION OPERATORS:")
    
    // count - number of matching elements
    println("   • count { even }: ${numbers.count { it % 2 == 0 }}")
    
    // sumOf - sum with selector
    val totalLength = words.sumOf { it.length }
    println("   • sumOf { word.length }: $totalLength")
    
    // average - arithmetic mean
    println("   • average: ${numbers.average()}")
    
    // sum - numeric sum
    println("   • sum: ${numbers.sum()}")
    
    // minOrNull / maxOrNull
    println("   • minOrNull: ${numbers.minOrNull()}, maxOrNull: ${numbers.maxOrNull()}")
    
    // minByOrNull / maxByOrNull
    val shortestWord = words.minByOrNull { it.length }
    val longestWord = words.maxByOrNull { it.length }
    println("   • minBy/maxBy { length }: shortest='$shortestWord', longest='$longestWord'")
    
    // minOfOrNull / maxOfOrNull
    val shortestLength = words.minOfOrNull { it.length }
    println("   • minOfOrNull { length }: $shortestLength")
    
    // reduce - combine all elements (no initial value, uses first element)
    val product = numbers.reduce { acc, n -> acc * n }
    println("   • reduce { acc * n } (1*2*3*...*10): $product")
    
    // reduceOrNull - returns null for empty lists
    val emptyReduce = emptyList<Int>().reduceOrNull { acc, n -> acc + n }
    println("   • reduceOrNull on empty: $emptyReduce")
    
    // fold - reduce with initial value
    val foldProduct = numbers.fold(1) { acc, n -> acc * n }
    println("   • fold(1) { acc * n }: $foldProduct")
    
    // foldIndexed - fold with index
    val foldIndexed = numbers.foldIndexed(0) { index, acc, n -> acc + (index * n) }
    println("   • foldIndexed: $foldIndexed")
    
    // runningFold - intermediate results (like scan in other languages)
    val runningSums = numbers.runningFold(0) { acc, n -> acc + n }
    println("   • runningFold (cumulative sums): $runningSums")
    
    // runningReduce - running reduce
    val runningProducts = numbers.runningReduce { acc, n -> acc * n }
    println("   • runningReduce (cumulative products): $runningProducts")
    
    // joinToString - join with separator
    val joined = words.joinToString(separator = " | ", prefix = "[", postfix = "]")
    println("   • joinToString: $joined")
    
    // joinTo - join into Appendable
    val sb = StringBuilder()
    words.joinTo(sb, separator = ", ", limit = 3, truncated = "...")
    println("   • joinTo StringBuilder: $sb")
    println()
    
    // ═══════════════════════════════════════════════════════════════
    // ELEMENT QUERY OPERATORS (return single element or boolean)
    // ═══════════════════════════════════════════════════════════════
    println("   D. ELEMENT QUERY OPERATORS:")
    
    // any - true if any element matches
    println("   • any { >5 }: ${numbers.any { it > 5 }}")
    
    // all - true if all elements match
    println("   • all { >0 }: ${numbers.all { it > 0 }}")
    
    // none - true if no elements match
    println("   • none { <0 }: ${numbers.none { it < 0 }}")
    
    // first - first element (throws if empty)
    println("   • first: ${numbers.first()}")
    
    // firstOrNull - first matching or null
    val firstEven = numbers.firstOrNull { it % 2 == 0 }
    println("   • firstOrNull { even }: $firstEven")
    
    // last / lastOrNull
    println("   • last: ${numbers.last()}")
    
    // find - alias for firstOrNull
    val findResult = numbers.find { it > 5 }
    println("   • find { >5 }: $findResult")
    
    // findLast - last matching
    val findLastResult = numbers.findLast { it % 3 == 0 }
    println("   • findLast { %3==0 }: $findLastResult")
    
    // single - exactly one element (throws otherwise)
    // println("   • single: ${listOf(42).single()}")
    
    // singleOrNull - one element or null
    println("   • singleOrNull on empty: ${emptyList<Int>().singleOrNull()}")
    println("   • singleOrNull on [1]: ${listOf(1).singleOrNull()}")
    println("   • singleOrNull on [1,2]: ${listOf(1, 2).singleOrNull()}")
    
    // getOrElse - get by index with default
    println("   • getOrElse(99) { -1 }: ${numbers.getOrElse(99) { -1 }}")
    
    // elementAtOrElse / elementAtOrNull
    println("   • elementAtOrNull(20): ${numbers.elementAtOrNull(20)}")
    
    // contains / in operator
    println("   • contains(5): ${numbers.contains(5)}, 5 in numbers: ${5 in numbers}")
    
    // indexOf / lastIndexOf
    println("   • indexOf(5): ${numbers.indexOf(5)}")
    
    // indexOfFirst / indexOfLast
    val firstLongIndex = words.indexOfFirst { it.length > 5 }
    println("   • indexOfFirst { length>5 }: $firstLongIndex")
    println()
    
    // ═══════════════════════════════════════════════════════════════
    // GROUPING OPERATORS
    // ═══════════════════════════════════════════════════════════════
    println("   E. GROUPING OPERATORS:")
    
    // groupBy - group by key selector
    val byLength = words.groupBy { it.length }
    println("   • groupBy { length }: $byLength")
    
    // groupBy with value transform
    val lengthToUppercase = words.groupBy(
        keySelector = { it.length },
        valueTransform = { it.uppercase() }
    )
    println("   • groupBy + transform: $lengthToUppercase")
    
    // groupingBy + aggregate (efficient for large collections)
    val countByLength = words.groupingBy { it.length }.eachCount()
    println("   • groupingBy().eachCount(): $countByLength")
    
    // fold on grouping
    val sumByLength = numbers.groupingBy { it % 3 }.fold(0) { acc, n -> acc + n }
    println("   • groupingBy().fold(): $sumByLength")
    
    // partition - split into two lists (matching / not matching)
    val (evensPart, oddsPart) = numbers.partition { it % 2 == 0 }
    println("   • partition { even }: evens=$evensPart, odds=$oddsPart")
    println()
    
    // ═══════════════════════════════════════════════════════════════
    // SORTING OPERATORS
    // ═══════════════════════════════════════════════════════════════
    println("   F. SORTING OPERATORS:")
    
    // sorted - natural order ascending
    println("   • sorted: ${listOf(3, 1, 4, 1, 5).sorted()}")
    
    // sortedDescending
    println("   • sortedDescending: ${listOf(3, 1, 4, 1, 5).sortedDescending()}")
    
    // sortedBy - sort by selector
    val byLength2 = words.sortedBy { it.length }
    println("   • sortedBy { length }: $byLength2")
    
    // sortedByDescending
    val byLengthDesc = words.sortedByDescending { it.length }
    println("   • sortedByDescending { length }: $byLengthDesc")
    
    // sortedWith - custom comparator
    val byLastLetter = words.sortedWith(compareBy { it.last() })
    println("   • sortedWith(compareBy { last() }): $byLastLetter")
    
    // reverse order comparator
    val reversedWords = words.sortedWith(compareByDescending<String> { it.length }.thenBy { it })
    println("   • thenBy (secondary sort): $reversedWords")
    
    // reversed - reverse existing (not sorting)
    println("   • reversed: ${numbers.reversed()}")
    
    // shuffled - random order
    println("   • shuffled: ${numbers.shuffled()}")
    println()
    
    // ═══════════════════════════════════════════════════════════════
    // UTILITY OPERATORS
    // ═══════════════════════════════════════════════════════════════
    println("   G. UTILITY OPERATORS:")
    
    // onEach - perform side effect, return original
    val sumOnEach = numbers.onEach { println("      Processing: $it") }.sum()
    println("   • onEach (side effect) + sum: $sumOnEach")
    
    // forEach - iterate (returns Unit)
    print("   • forEach: ")
    listOf("a", "b", "c").forEach { print("$it ") }
    println()
    
    // forEachIndexed
    print("   • forEachIndexed: ")
    words.forEachIndexed { i, w -> print("[$i:$w] ") }
    println()
    
    // toList / toMutableList / toSet
    val immutable = numbers.toList()
    val mutable = numbers.toMutableList()
    val set = numbers.toSet()
    println("   • toList/toMutableList/toSet: ${immutable::class.simpleName}, ${mutable::class.simpleName}, ${set::class.simpleName}")
    
    // toMap (from list of pairs)
    val pairList = listOf("a" to 1, "b" to 2, "c" to 3)
    val map = pairList.toMap()
    println("   • toMap: $map")
    
    // associate / associateBy / associateWith
    val charToLength = words.associate { it.first() to it.length }
    println("   • associate { first->length }: $charToLength")
    
    val byFirstChar = words.associateBy { it.first() }
    println("   • associateBy { first }: $byFirstChar")
    
    val toLengthMap = words.associateWith { it.length }
    println("   • associateWith { length }: $toLengthMap")
    println()
    
    // ═══════════════════════════════════════════════════════════════
    // REAL ANDROID USE CASES
    // ═══════════════════════════════════════════════════════════════
    println("   H. REAL ANDROID USE CASES:\n")
    
    // Use case 1: API response processing
    println("      1. API Response Processing:")
    data class UserResponse(val id: Int, val name: String, val isActive: Boolean)
    val apiResponses = listOf(
        UserResponse(1, "Alice", true),
        UserResponse(2, "Bob", false),
        UserResponse(3, "Charlie", true),
        UserResponse(4, "Diana", false)
    )
    
    val activeUserNames = apiResponses
        .filter { it.isActive }                    // Keep only active
        .sortedBy { it.name }                       // Sort alphabetically
        .map { it.name }                            // Extract names
        .take(10)                                   // Limit results
    println("         Active users sorted: $activeUserNames")
    
    // Use case 2: RecyclerView data preparation
    println("\n      2. RecyclerView Diffing Preparation:")
    data class Item(val id: String, val title: String, val timestamp: Long)
    val items = listOf(
        Item("1", "First", 1000),
        Item("2", "Second", 500),
        Item("3", "Third", 2000)
    )
    
    val sortedItems = items
        .distinctBy { it.id }                       // Remove duplicates
        .sortedByDescending { it.timestamp }        // Newest first
        .map { it.copy(title = it.title.uppercase()) } // Transform
    println("         Sorted & transformed: $sortedItems")
    
    // Use case 3: Search results filtering
    println("\n      3. Search Results Filtering:")
    val searchResults = listOf("Android", "android studio", "Kotlin", "java", "ANDROID")
    val query = "android"
    
    val matchingResults = searchResults
        .filter { it.contains(query, ignoreCase = true) }
        .distinctBy { it.lowercase() }              // Case-insensitive dedup
        .sortedWith(compareBy { 
            when {
                it.equals(query, ignoreCase = true) -> 0  // Exact match first
                it.startsWith(query, ignoreCase = true) -> 1 // Starts with second
                else -> 2
            }
        })
    println("         Search '$query': $matchingResults")
    
    // Use case 4: Analytics aggregation
    println("\n      4. Analytics Aggregation:")
    data class Event(val type: String, val userId: String, val duration: Int)
    val events = listOf(
        Event("click", "user1", 100),
        Event("view", "user1", 500),
        Event("click", "user2", 200),
        Event("click", "user1", 150)
    )
    
    val analytics = events
        .groupBy { it.type }
        .mapValues { (_, events) ->
            mapOf(
                "count" to events.count(),
                "totalDuration" to events.sumOf { it.duration },
                "avgDuration" to events.map { it.duration }.average().toInt(),
                "uniqueUsers" to events.distinctBy { it.userId }.count()
            )
        }
    println("         By type: " + analytics.map { "${it.key}=${it.value}" }.joinToString(", "))
    
    // Use case 5: Pagination
    println("\n      5. Pagination:")
    val allItems = (1..100).toList()
    val page = 3
    val pageSize = 10
    
    val paginatedItems = allItems
        .drop((page - 1) * pageSize)  // Skip previous pages
        .take(pageSize)                 // Take current page
    println("         Page $page: $paginatedItems")
    
    // Use case 6: Form validation
    println("\n      6. Form Validation:")
    data class Field(val name: String, val value: String?)
    val formFields = listOf(
        Field("email", "user@example.com"),
        Field("password", null),
        Field("name", "John")
    )
    
    val allFieldsPresent = formFields.all { !it.value.isNullOrBlank() }
    val missingFields = formFields
        .filter { it.value.isNullOrBlank() }
        .map { it.name }
    println("         All valid: $allFieldsPresent, Missing: $missingFields")
    println()
    
    // ═══════════════════════════════════════════════════════════════
    // PERFORMANCE COMPARISON: List vs Sequence
    // ═══════════════════════════════════════════════════════════════
    println("   I. PERFORMANCE: List vs Sequence:\n")
    
    println("      List (eager evaluation) - creates intermediate collections:")
    println("         list.filter { }.map { }.take(5)")
    println("         Creates: filtered list → mapped list → final list")
    println()
    
    println("      Sequence (lazy evaluation) - no intermediate collections:")
    println("         list.asSequence().filter { }.map { }.take(5).toList()")
    println("         Processes one element at a time through entire chain")
    println()
    
    println("      When to use Sequence:")
    println("         ✓ Large datasets (1000+ items)")
    println("         ✓ Chained operations (filter + map + take)")
    println("         ✓ Infinite/potentially large streams")
    println("         ✗ Small collections (overhead not worth it)")
    println("         ✗ Single operation (no benefit)")
    println()
}
````

## Scope Functions Explained

````kotlin
fun scopeFunctionsDemo() {
    val user = User(name = "John", email = "john@example.com", id = 0)
    
    // let: transform + null check
    val nameLength = user.name.let { 
        println("Processing: $it")
        it.length  // return value
    }
    
    // run: configure + compute result
    val greeting = user.run {
        "Hello, $name! Your email is $email"  // access directly
    }
    
    // apply: configure object, return same object (builder pattern)
    val configuredUser = user.apply {
        // this.name = "New Name"  // if mutable
        // side effects
    }
    
    // also: side effect, return same object (logging)
    val loggedUser = user.also {
        println("Created user: $it")
    }
    
    // with: operate on object (non-extension)
    val info = with(user) {
        "Name: $name, Email: $email"
    }
}
````

Helper for demo

````kotlin
data class User(val id: Int, val name: String, val email: String)
````

## Interview Questions To Expect

Q: "What's the difference between let, run, apply, also, with?"
A:
- let:     (T) -&gt; R,    uses 'it',     returns lambda result      → transform + null check
- run:     T.() -&gt; R,   uses 'this',   returns lambda result      → configure + compute
- apply:   T.() -&gt; T,    uses 'this',   returns receiver           → configure object
- also:    (T) -&gt; T,     uses 'it',     returns receiver           → side effects/logging
- with:    (T) -&gt; R,     uses 'this',   returns lambda result      → operate on object

Q: "When to use Sequence vs List operations?"
A: Sequences use lazy evaluation - good for large datasets.
List operations create intermediate collections at each step.

Q: "How do inline functions work?"
A: Compiler copies function body to call site, eliminating lambda overhead.
Used for performance-critical functions taking lambdas.

Q: "Explain closure in Kotlin."
A: Lambda captures variables from outer scope. Even if outer function returns,
the lambda still has access to captured variables.

Q: "Common collection operations in Android?"
A:
TRANSFORMATION: map, flatMap, flatten, zip, windowed, chunked
FILTERING: filter, take/drop, distinctBy, slice
AGGREGATION: reduce, fold, sumOf, min/max, count, average, joinToString
ELEMENTS: first/last/find, single, any/all/none, contains/in
GROUPING: groupBy, groupingBy, partition, associate
SORTING: sortedBy, sortedWith, reversed, shuffled
UTILITY: forEach, onEach, toList/Set/Map

Q: "map vs flatMap?"
A: map transforms each element one-to-one (List&lt;T&gt; → List&lt;R&gt;).
flatMap transforms each element to multiple elements, then flattens (List&lt;T&gt; → List&lt;R&gt; with flattening).
Example: listOf(1,2).map { listOf(it, it) } = [[1,1], [2,2]]
listOf(1,2).flatMap { listOf(it, it) } = [1,1,2,2]

Q: "filter vs partition?"
A: filter keeps matching elements, discards others.
partition splits into TWO lists: (matching, non-matching).
Example: val (evens, odds) = numbers.partition { it % 2 == 0 }

Q: "reduce vs fold?"
A: reduce uses first element as initial accumulator, throws on empty.
fold takes explicit initial value, works on empty lists.
reduce: [1,2,3] → 1+2+3 = 6 (starts with 1)
fold(0): [1,2,3] → 0+1+2+3 = 6 (starts with 0)

Q: "find vs firstOrNull?"
A: They are identical! find is alias for firstOrNull with predicate.
Both return first matching element or null.

Q: "groupBy vs groupingBy?"
A: groupBy creates a Map immediately (eager).
groupingBy returns Grouping object for efficient operations like eachCount(), fold(), reduce().
Use groupingBy for: large collections, counting, aggregations.

Q: "sortedBy vs sortedWith?"
A: sortedBy: single selector (shorter syntax)
sortedWith: custom Comparator (more control, multiple criteria)
Can chain with thenBy, thenByDescending for secondary sorts.

Q: "takeWhile vs filter?"
A: takeWhile takes elements UNTIL condition fails, then stops.
filter checks ALL elements, keeps ALL matching.
Example: [1,2,3,2,1].takeWhile { it &lt; 3 } = [1,2]
[1,2,3,2,1].filter { it &lt; 3 } = [1,2,2,1]

Q: "When to use Sequence?"
A: Use asSequence() for:
- Large collections (1000+ items)
- Multiple chained operations (filter + map + take)
- Avoiding intermediate collection creation
Don't use for:
- Small collections (overhead &gt; benefit)
- Single operations (no intermediate to avoid)

Q: "associate vs associateBy vs associateWith?"
A: associate: List&lt;Pair&lt;K,V&gt;&gt; → Map&lt;K,V&gt; (transform each to pair)
associateBy: List&lt;T&gt; → Map&lt;K,T&gt; (key from element, value = element)
associateWith: List&lt;K&gt; → Map&lt;K,V&gt; (key = element, value from element)
