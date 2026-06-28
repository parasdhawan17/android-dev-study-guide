/**
 * DAY 1-2: Core Kotlin Concepts - Generics and Variance
 * 
 * Variance: in (contravariant), out (covariant), invariant (default)
 * Essential for: Type-safe collections, API design, understanding Android SDK
 */

// ============ 1. GENERICS BASICS ============

class Box<T>(val item: T) {
    fun get(): T = item
}

// Generic function
fun <T> List<T>.firstOr(default: T): T = firstOrNull() ?: default

// Multiple type parameters
interface Pair<K, V> {
    fun getKey(): K
    fun getValue(): V
}

// ============ 2. VARIANCE TYPES ============

/**
 * INVARIANT (default): Box<Cat> is NOT Box<Animal>
 * Use when type is consumed AND produced
 */
class InvariantBox<T>(var item: T) {
    fun get(): T = item
    fun set(value: T) { item = value }
}

/**
 * OUT (covariant): Box<Cat> IS Box<Animal>
 * Use when type is ONLY produced (returned)
 * "Producer/Output" - out
 */
interface Producer<out T> {
    fun produce(): T
    // Cannot accept T as parameter!
    // fun consume(item: T)  // COMPILE ERROR
}

/**
 * IN (contravariant): Box<Animal> IS Box<Cat>
 * Use when type is ONLY consumed (accepted as parameter)
 * "Consumer/Input" - in
 */
interface Consumer<in T> {
    fun consume(item: T)
    // Cannot return T!
    // fun produce(): T  // COMPILE ERROR
}

// ============ 3. PRACTICAL EXAMPLES ============

// OUT: Read-only list (produces T)
fun readFromList(animals: List<Animal>) {
    animals.forEach { println(it.name) }
}
// List is defined as: interface List<out E>
// So List<Cat> can be passed to List<Animal> parameter

// IN: Write-only sink (consumes T)
fun writeToSink(cats: MutableList<in Cat>, cat: Cat) {
    cats.add(cat)
    // Can add Cat or subtype to List<in Cat>
}
// MutableList accepts Cat, but we can pass MutableList<Animal>

// ============ 4. WHERE CLAUSES (TYPE CONSTRAINTS) ============

fun <T : Comparable<T>> sort(list: List<T>): List<T> {
    return list.sorted()
}

fun <T> copyWhenGreater(
    list: List<T>,
    threshold: T
): List<T> where T : Comparable<T>, T : CharSequence {
    return list.filter { it > threshold }
}

// ============ 5. STAR PROJECTIONS ============

fun printList(list: List<*>) {
    // List<*> means "List of unknown type"
    // Can read as Any?
    list.forEach { println(it) }
    // Cannot write: list.add(something) - don't know the type!
}

// ============ 6. REAL ANDROID EXAMPLES ============

// LiveData is covariant: LiveData<Cat> can be observed as LiveData<Animal>
// interface LiveData<out T>

// Observer is contravariant: Observer<Animal> can observe LiveData<Cat>
// interface Observer<in T>

// Comparator is contravariant
// interface Comparator<in T>
val animalComparator: Comparator<Animal> = Comparator { a, b ->
    a.name.compareTo(b.name)
}
// Can use for Cat list: listOfCats.sortedWith(animalComparator)

// ============ TYPE HIERARCHY ============

open class Animal(val name: String)
open class Mammal(name: String) : Animal(name)
class Cat(name: String) : Mammal(name)
class Dog(name: String) : Mammal(name)

// ============ MAIN DEMONSTRATION ============

fun main() {
    println("=== GENERICS AND VARIANCE DEMO ===\n")
    
    // 1. Basic generics
    println("1. BASIC GENERICS:")
    val intBox = Box(42)
    val stringBox = Box("Hello")
    println("   Int box: ${intBox.get()}")
    println("   String box: ${stringBox.get()}")
    println()
    
    // 2. Invariance
    println("2. INVARIANCE (default):")
    val catBox = InvariantBox(Cat("Whiskers"))
    // val animalBox: InvariantBox<Animal> = catBox  // ERROR! Invariant
    println("   InvariantBox<Cat> cannot be assigned to InvariantBox<Animal>")
    println("   Even though Cat IS Animal, Box<Cat> is NOT Box<Animal>")
    println()
    
    // 3. Covariance (out)
    println("3. COVARIANCE (out):")
    val catProducer = object : Producer<Cat> {
        override fun produce(): Cat = Cat("Kitty")
    }
    val animalProducer: Producer<Animal> = catProducer  // OK! Covariant
    println("   Producer<Cat> can be used as Producer<Animal>")
    println("   Produced: ${animalProducer.produce().name}")
    println()
    
    // 4. Contravariance (in)
    println("4. CONTRAVARIANCE (in):")
    val animalConsumer = object : Consumer<Animal> {
        override fun consume(item: Animal) {
            println("   Consuming ${item.name}")
        }
    }
    val catConsumer: Consumer<Cat> = animalConsumer  // OK! Contravariant
    println("   Consumer<Animal> can be used as Consumer<Cat>")
    catConsumer.consume(Cat("Mittens"))
    println()
    
    // 5. Real example with List (covariant)
    println("5. LIST COVARIANCE (List is defined as List<out E>):")
    val cats: List<Cat> = listOf(Cat("A"), Cat("B"))
    processAnimals(cats)  // List<Cat> works as List<Animal>!
    println()
    
    // 6. Type constraints
    println("6. TYPE CONSTRAINTS:")
    val numbers = listOf(3, 1, 4, 1, 5)
    println("   Unsorted: $numbers")
    println("   Sorted: ${sort(numbers)}")
    println()
    
    // 7. Star projection
    println("7. STAR PROJECTION (*):")
    printList(listOf(1, 2, 3))
    printList(listOf("a", "b", "c"))
    println("   Works with any List type")
    println()
    
    // 8. Memory trick
    println("8. MEMORY TRICK:")
    println("   out (covariant): Producer → reads → Box<Sub> IS Box<Super>")
    println("   in (contravariant): Consumer → writes → Box<Super> IS Box<Sub>")
    println()
    println("   PECS Principle (Producer Extends, Consumer Super):")
    println("   - Producer (out) - produces T, can use subtype")
    println("   - Consumer (in) - consumes T, can use supertype")
    println()
    
    // 9. Android examples
    println("9. ANDROID EXAMPLES:")
    println("   - LiveData<out T> - you observe, it produces")
    println("   - Observer<in T> - it consumes what you observe")
    println("   - Comparator<in T> - compares, doesn't produce")
    println("   - List<out E> - read-only, produces elements")
    println("   - MutableList<E> - invariant (read and write)")
}

fun processAnimals(animals: List<Animal>) {
    animals.forEach { println("   Processing: ${it.name}") }
}

// ============ INTERVIEW QUESTIONS TO EXPECT ============
/*
Q: "Explain variance in Kotlin: in, out, and no modifier"
A:
- No modifier = INVARIANT: Box<A> is not related to Box<B> even if A extends B
- out = COVARIANT: Box<Sub> IS Box<Super> (produces T, read-only)
- in = CONTRAVARIANT: Box<Super> IS Box<Sub> (consumes T, write-only)

Q: "When to use 'out' vs 'in'?"
A:
- Use 'out' when class/interface only PRODUCES/RETURNS T (never accepts T)
- Use 'in' when class/interface only CONSUMES/ACCEPTS T (never returns T)
- Use no modifier (invariant) when both producing and consuming

Q: "Why is List covariant (List<out E>) but MutableList is invariant?"
A: List is read-only - only produces elements, safe for covariance.
MutableList both produces (get) and consumes (add), must be invariant.

Q: "Can you add to a List<*>?"
A: No! Star projection means "unknown type". Can't add because we don't
know what type is expected. Can only read as Any?.

Q: "What are type constraints (where clauses)?"
A: Limit type parameters to types with certain characteristics.
fun <T : Comparable<T>> means T must implement Comparable<T>.

Q: "Why does this not compile?"
  fun <T> copy(source: MutableList<T>, dest: MutableList<T>) { ... }
  copy(mutableListOf<Animal>(), mutableListOf<Cat>())
A: MutableList is invariant. Even though Cat is Animal, MutableList<Cat>
is not MutableList<Animal>. Solution: use MutableList<out Animal> or
MutableList<in Cat> appropriately.

Q: "PECS principle?"
A: Producer Extends (out), Consumer Super (in)
- If you produce T (return it), use covariant (out)
- If you consume T (accept it), use contravariant (in)
*/
