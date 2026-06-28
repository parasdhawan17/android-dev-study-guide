# Generics Variance

DAY 1-2: Core Kotlin Concepts - Generics and Variance

Variance: in (contravariant), out (covariant), invariant (default)
Essential for: Type-safe collections, API design, understanding Android SDK

## 1. Generics Basics

````kotlin
class Box<T>(val item: T) {
    fun get(): T = item
}
````

Generic function

````kotlin
fun <T> List<T>.firstOr(default: T): T = firstOrNull() ?: default
````

Multiple type parameters

````kotlin
interface Pair<K, V> {
    fun getKey(): K
    fun getValue(): V
}
````

## 2. Variance Types

INVARIANT (default): Box&lt;Cat&gt; is NOT Box&lt;Animal&gt;
Use when type is consumed AND produced

````kotlin
class InvariantBox<T>(var item: T) {
    fun get(): T = item
    fun set(value: T) { item = value }
}
````

OUT (covariant): Box&lt;Cat&gt; IS Box&lt;Animal&gt;
Use when type is ONLY produced (returned)
"Producer/Output" - out

````kotlin
interface Producer<out T> {
    fun produce(): T
    // Cannot accept T as parameter!
    // fun consume(item: T)  // COMPILE ERROR
}
````

IN (contravariant): Box&lt;Animal&gt; IS Box&lt;Cat&gt;
Use when type is ONLY consumed (accepted as parameter)
"Consumer/Input" - in

````kotlin
interface Consumer<in T> {
    fun consume(item: T)
    // Cannot return T!
    // fun produce(): T  // COMPILE ERROR
}
````

## 3. Practical Examples

OUT: Read-only list (produces T)

````kotlin
fun readFromList(animals: List<Animal>) {
    animals.forEach { println(it.name) }
}
````

List is defined as: interface List&lt;out E&gt;
So List&lt;Cat&gt; can be passed to List&lt;Animal&gt; parameter

IN: Write-only sink (consumes T)

````kotlin
fun writeToSink(cats: MutableList<in Cat>, cat: Cat) {
    cats.add(cat)
    // Can add Cat or subtype to List<in Cat>
}
````

MutableList accepts Cat, but we can pass MutableList&lt;Animal&gt;

## 4. Where Clauses (Type Constraints)

````kotlin
fun <T : Comparable<T>> sort(list: List<T>): List<T> {
    return list.sorted()
}

fun <T> copyWhenGreater(
    list: List<T>,
    threshold: T
): List<T> where T : Comparable<T>, T : CharSequence {
    return list.filter { it > threshold }
}
````

## 5. Star Projections

````kotlin
fun printList(list: List<*>) {
    // List<*> means "List of unknown type"
    // Can read as Any?
    list.forEach { println(it) }
    // Cannot write: list.add(something) - don't know the type!
}
````

## 6. Real Android Examples

LiveData is covariant: LiveData&lt;Cat&gt; can be observed as LiveData&lt;Animal&gt;
interface LiveData&lt;out T&gt;

Observer is contravariant: Observer&lt;Animal&gt; can observe LiveData&lt;Cat&gt;
interface Observer&lt;in T&gt;

Comparator is contravariant
interface Comparator&lt;in T&gt;

````kotlin
val animalComparator: Comparator<Animal> = Comparator { a, b ->
    a.name.compareTo(b.name)
}
````

Can use for Cat list: listOfCats.sortedWith(animalComparator)

## Type Hierarchy

````kotlin
open class Animal(val name: String)
open class Mammal(name: String) : Animal(name)
class Cat(name: String) : Mammal(name)
class Dog(name: String) : Mammal(name)
````

## Main Demonstration

````kotlin
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
````

## Interview Questions To Expect

Q: "Explain variance in Kotlin: in, out, and no modifier"
A:
- No modifier = INVARIANT: Box&lt;A&gt; is not related to Box&lt;B&gt; even if A extends B
- out = COVARIANT: Box&lt;Sub&gt; IS Box&lt;Super&gt; (produces T, read-only)
- in = CONTRAVARIANT: Box&lt;Super&gt; IS Box&lt;Sub&gt; (consumes T, write-only)

Q: "When to use 'out' vs 'in'?"
A:
- Use 'out' when class/interface only PRODUCES/RETURNS T (never accepts T)
- Use 'in' when class/interface only CONSUMES/ACCEPTS T (never returns T)
- Use no modifier (invariant) when both producing and consuming

Q: "Why is List covariant (List&lt;out E&gt;) but MutableList is invariant?"
A: List is read-only - only produces elements, safe for covariance.
MutableList both produces (get) and consumes (add), must be invariant.

Q: "Can you add to a List&lt;*&gt;?"
A: No! Star projection means "unknown type". Can't add because we don't
know what type is expected. Can only read as Any?.

Q: "What are type constraints (where clauses)?"
A: Limit type parameters to types with certain characteristics.
fun &lt;T : Comparable&lt;T&gt;&gt; means T must implement Comparable&lt;T&gt;.

Q: "Why does this not compile?"
fun &lt;T&gt; copy(source: MutableList&lt;T&gt;, dest: MutableList&lt;T&gt;) { ... }
copy(mutableListOf&lt;Animal&gt;(), mutableListOf&lt;Cat&gt;())
A: MutableList is invariant. Even though Cat is Animal, MutableList&lt;Cat&gt;
is not MutableList&lt;Animal&gt;. Solution: use MutableList&lt;out Animal&gt; or
MutableList&lt;in Cat&gt; appropriately.

Q: "PECS principle?"
A: Producer Extends (out), Consumer Super (in)
- If you produce T (return it), use covariant (out)
- If you consume T (accept it), use contravariant (in)
