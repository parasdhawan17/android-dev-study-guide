# Android Technical Interview Study Plan

> Based on Revolut Technical Conversation Interview Preparation Guide

---

## Overview

**Interview Format:** 1-hour in-depth technical discussion with 1-2 engineers  
**Components:** Technical discussion + short coding exercise  
**Focus Areas:** Computer science fundamentals, data structures, algorithms, Android-specific knowledge

---

## Interview Structure Breakdown

| Section | Topics Covered | Estimated Time |
|---------|---------------|----------------|
| 1. Introduction | Self-introduction, experience overview | 5-10 min |
| 2. Kotlin & Android Fundamentals | Core Kotlin, Android lifecycles | 10-15 min |
| 3. Asynchronous Programming & Concurrency | Coroutines, Flow, thread safety | 10-15 min |
| 4. Architecture & Design Patterns | MVVM, MVI, Clean Architecture, DI | 10-15 min |
| 5. Testing | Unit testing, ViewModel testing, maintainable code | 5-10 min |
| 6. UI Optimization & Performance | RecyclerView, layout performance | 5-10 min |
| 7. Problem-Solving & Autonomy | Debugging, edge cases, error handling | 5-10 min |
| 8. Wrap-up & Questions | Your questions for interviewers | 5 min |

---

## Detailed Study Plan

### Week 1: Kotlin & Android Fundamentals

#### Day 1-2: Core Kotlin Concepts
**Topics:**
- Data classes (properties, copy(), equals(), hashCode())
- Sealed classes (exhaustive when expressions, state management)
- Extension functions (scope, visibility, use cases)
- Higher-order functions and lambdas
- Null safety (?, !!, ?., ?:, let, run, apply, also)
- Type system (generics, variance: in/out)

**Practice Tasks:**
- [ ] Write 5 data classes with different use cases
- [ ] Create a sealed class hierarchy for UI states (Loading, Success, Error)
- [ ] Build 3 extension functions for String, List, and custom class
- [ ] Solve 10 Kotlin Koans

**Resources:**
- Kotlin Documentation: https://kotlinlang.org/docs/basic-syntax.html
- Kotlin Koans: https://play.kotlinlang.org/koans/overview

#### Day 3-4: Android Component Lifecycles
**Topics:**
- Activity lifecycle (onCreate, onStart, onResume, onPause, onStop, onDestroy)
- Fragment lifecycle and transaction management
- LifecycleOwner, LifecycleObserver, Lifecycle-aware components
- ViewModel lifecycle and configuration changes
- SavedStateHandle for process death
- Common lifecycle challenges (memory leaks, context handling)

**Practice Tasks:**
- [ ] Draw complete Activity & Fragment lifecycle diagrams
- [ ] Identify 5 common memory leak scenarios and fixes
- [ ] Implement a LifecycleObserver that logs all lifecycle events
- [ ] Write code handling configuration changes properly

**Resources:**
- Android Lifecycle Guide: https://developer.android.com/guide/components/activities/activity-lifecycle
- ViewModel Overview: https://developer.android.com/topic/libraries/architecture/viewmodel

#### Day 5-7: Practical Application
**Topics:**
- Jetpack Compose lifecycle (if applicable)
- Legacy View system vs Compose considerations
- Process death and restoration
- Background execution limits (WorkManager, Services)

**Practice Tasks:**
- [ ] Build a sample app demonstrating lifecycle awareness
- [ ] Implement state saving/restoration after process death
- [ ] Mock interview: Explain Activity lifecycle with diagram

---

### Week 2: Asynchronous Programming & Concurrency

#### Day 1-2: Kotlin Coroutines Fundamentals
**Topics:**
- CoroutineScope and structured concurrency
- Job lifecycle (start, cancel, join, await)
- CoroutineContext and Dispatchers (Main, IO, Default, Unconfined)
- Suspend functions and suspension points
- Coroutine builders (launch, async, runBlocking, withContext)
- Coroutine cancellation and cooperative cancellation

**Practice Tasks:**
- [ ] Write code using all 4 Dispatchers with explanations
- [ ] Implement proper coroutine cancellation patterns
- [ ] Convert callback-based API to suspend functions
- [ ] Build a coroutine scope hierarchy diagram

**Resources:**
- Coroutines Guide: https://kotlinlang.org/docs/coroutines-guide.html
- Coroutines on Android: https://developer.android.com/kotlin/coroutines

#### Day 3-4: Flow and Reactive Programming
**Topics:**
- Flow vs LiveData comparison
- Cold vs Hot flows (StateFlow, SharedFlow)
- Flow operators (map, filter, flatMapLatest, combine, zip)
- Flow lifecycle (onStart, onEach, onCompletion, catch)
- Collecting flows in UI (lifecycle-aware collection)
- Backpressure handling

**Practice Tasks:**
- [ ] Compare Flow vs LiveData with code examples
- [ ] Implement StateFlow for UI state management
- [ ] Create a Flow pipeline with multiple operators
- [ ] Handle errors in Flow with catch operator

**Resources:**
- Flow Documentation: https://kotlinlang.org/docs/flow.html
- StateFlow and SharedFlow: https://developer.android.com/kotlin/flow/stateflow-and-sharedflow

#### Day 5-6: Thread Safety and Synchronization
**Topics:**
- Thread safety concepts (race conditions, deadlocks)
- Synchronization strategies (synchronized, ReentrantLock, Semaphore)
- Concurrent collections (ConcurrentHashMap, CopyOnWriteArrayList)
- Coroutine synchronization (Mutex, Semaphore, Channel)
- Atomic operations (AtomicInteger, AtomicReference)
- Actor pattern

**Practice Tasks:**
- [ ] Implement thread-safe singleton with different approaches
- [ ] Create a concurrent data structure wrapper
- [ ] Solve producer-consumer problem with Channels
- [ ] Build a concurrent cache with expiration

**Resources:**
- Java Concurrency in Practice (Book)
- Concurrent Programming: https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html

#### Day 7: Multithreading Deep Dive
**Topics:**
- Handler, Looper, MessageQueue
- Thread pools and executors
- Android main thread restrictions
- ANR prevention strategies

**Practice Tasks:**
- [ ] Mock interview: Explain Coroutines vs Threads
- [ ] Implement custom Looper/Handler pattern
- [ ] Profile and fix a multithreading bug

---

### Week 3: Architecture & Design Patterns

#### Day 1-2: MVVM Pattern
**Topics:**
- MVVM components (View, ViewModel, Model)
- Data binding (One-way, Two-way)
- ViewModel responsibilities and scope
- UI state management in ViewModel
- UI events handling (Event, State, Effect pattern)

**Practice Tasks:**
- [ ] Build a complete MVVM app with proper separation
- [ ] Implement sealed class for UI events
- [ ] Handle configuration changes with ViewModel

**Resources:**
- Guide to App Architecture: https://developer.android.com/guide/components/fundamentals
- MVVM Pattern: https://developer.android.com/topic/libraries/architecture/viewmodel

#### Day 3-4: MVI Pattern
**Topics:**
- MVI components (Model, View, Intent)
- Unidirectional data flow
- State reduction patterns
- MVI vs MVVM trade-offs
- MVI in Compose

**Practice Tasks:**
- [ ] Convert MVVM app to MVI
- [ ] Implement a StateReducer
- [ ] Compare pros/cons of MVI vs MVVM

**Resources:**
- MVI Architecture for Android: https://proandroiddev.com/mvi-architecture-for-android-1b272d237552

#### Day 5-6: Clean Architecture
**Topics:**
- Layer separation (Presentation, Domain, Data)
- Dependency Rule (dependencies point inward)
- Use Cases (Interactors) responsibilities
- Repository pattern abstraction
- Data Source separation (Local, Remote)
- Mapping between layers

**Practice Tasks:**
- [ ] Design Clean Architecture for a real app
- [ ] Define Use Cases for a feature
- [ ] Implement Repository pattern with multiple sources
- [ ] Draw Clean Architecture diagram

**Resources:**
- Clean Architecture by Robert C. Martin (Book)
- Android Clean Architecture: https://developer.android.com/topic/architecture

#### Day 7: Dependency Injection
**Topics:**
- DI principles (Inversion of Control, Dependency Inversion)
- Manual DI vs Framework DI
- Hilt/Dagger basics (Modules, Components, Scopes)
- Koin as alternative
- Testing with DI

**Practice Tasks:**
- [ ] Implement DI without framework (Service Locator, Factory)
- [ ] Set up Hilt in a sample app
- [ ] Create test doubles with DI
- [ ] Mock interview: Explain DI benefits with examples

**Resources:**
- Hilt Documentation: https://developer.android.com/training/dependency-injection/hilt-android

---

### Week 4: Testing

#### Day 1-2: Unit Testing Fundamentals
**Topics:**
- JUnit 4/5 basics (annotations, assertions, lifecycle)
- Test structure (Arrange-Act-Assert)
- Parameterized tests
- Test doubles (Mocks, Stubs, Spies, Fakes)
- Mockito basics (when, verify, any, argument captors)
- MockK for Kotlin (every, verify, relaxed mocks)

**Practice Tasks:**
- [ ] Write 10 unit tests for different scenarios
- [ ] Use Mockito to mock dependencies
- [ ] Practice MockK syntax with Kotlin
- [ ] Create test doubles manually

**Resources:**
- Testing Fundamentals: https://developer.android.com/training/testing/fundamentals
- MockK Documentation: https://mockk.io/

#### Day 3-4: ViewModel Testing
**Topics:**
- InstantTaskExecutorRule
- Coroutines testing (TestDispatcher, runTest)
- Testing LiveData/StateFlow
- Testing coroutines in ViewModel
- Testing state changes

**Practice Tasks:**
- [ ] Write comprehensive ViewModel tests
- [ ] Test coroutine operations with TestDispatcher
- [ ] Verify state updates in tests
- [ ] Handle testing LiveData emissions

**Code Pattern:**
```kotlin
@get:Rule
val instantTaskExecutorRule = InstantTaskExecutorRule()

@get:Rule
val mainDispatcherRule = MainDispatcherRule()

@Test
fun `when loadData called, state changes to success`() = runTest {
    // Arrange
    whenever(repository.getData()).thenReturn(expectedData)
    
    // Act
    viewModel.loadData()
    advanceUntilIdle()
    
    // Assert
    assertEquals(expectedState, viewModel.uiState.value)
}
```

#### Day 5: Use Case Testing
**Topics:**
- Testing business logic in Use Cases
- Repository mocking in Use Cases
- Testing error scenarios
- Testing with coroutines

**Practice Tasks:**
- [ ] Write tests for 3 different Use Cases
- [ ] Test success, error, and edge cases
- [ ] Verify repository interactions

#### Day 6-7: Integration and Practice
**Topics:**
- Test coverage strategies
- Testable code design
- TDD approach

**Practice Tasks:**
- [ ] Achieve 80%+ coverage on a feature
- [ ] Refactor code to be more testable
- [ ] Mock interview: Explain testing strategy

---

### Week 5: UI Optimization & Performance

#### Day 1-2: RecyclerView Performance
**Topics:**
- ViewHolder pattern and recycling
- DiffUtil for efficient updates
- ListAdapter implementation
- Payloads for partial updates
- View recycling optimization
- Image loading optimization (Glide, Coil)
- Nested RecyclerViews

**Practice Tasks:**
- [ ] Implement RecyclerView with DiffUtil
- [ ] Add payloads for partial binding
- [ ] Optimize image loading with placeholders
- [ ] Profile RecyclerView performance

**Resources:**
- RecyclerView Performance: https://developer.android.com/topic/performance/rendering/optimizing-view-hierarchies

#### Day 3-4: Layout Performance
**Topics:**
- View hierarchy depth reduction
- ConstraintLayout optimization
- Flattening layouts
- Merge and ViewStub usage
- Custom View considerations
- Overdraw detection and reduction
- Layout Inspector usage

**Practice Tasks:**
- [ ] Convert nested layouts to ConstraintLayout
- [ ] Use Layout Inspector to find issues
- [ ] Fix overdraw issues in an app
- [ ] Implement ViewStub for conditional layouts

**Resources:**
- Optimizing Layout Hierarchies: https://developer.android.com/topic/performance/rendering/optimizing-view-hierarchies
- ConstraintLayout: https://developer.android.com/develop/ui/views/layout/constraint-layout

#### Day 5-6: General Performance
**Topics:**
- Memory leaks detection (LeakCanary)
- Memory profiling (Profiler)
- Bitmap optimization
- ANR causes and prevention
- Jank detection (Profile GPU Rendering)
- Startup time optimization

**Practice Tasks:**
- [ ] Profile an app with Android Profiler
- [ ] Find and fix a memory leak
- [ ] Optimize app startup time
- [ ] Mock interview: Debug a performance issue

---

### Week 6: Problem-Solving & Debugging + Final Preparation

#### Day 1-2: Debugging Techniques
**Topics:**
- Systematic debugging approach
- Log analysis
- Debugger advanced features (conditional breakpoints, evaluate expression)
- Stack trace analysis
- Memory dump analysis
- Network request debugging

**Practice Tasks:**
- [ ] Debug 5 different types of bugs (crashes, ANRs, logic errors)
- [ ] Practice explaining debugging thought process aloud
- [ ] Use advanced debugger features

#### Day 3-4: Edge Cases & Error Handling
**Topics:**
- Identifying edge cases (null, empty, boundary conditions)
- Network failure handling (retry, exponential backoff)
- Data validation strategies
- Exception handling patterns
- Result/Either pattern for error propagation
- Graceful degradation

**Practice Tasks:**
- [ ] Write code handling 10 different error scenarios
- [ ] Implement retry mechanism with backoff
- [ ] Practice "what-if" analysis for features

#### Day 5-6: Coding Interview Practice
**Topics:**
- Data structures review (Arrays, Lists, Maps, Sets, Trees, Graphs)
- Algorithm patterns (Two pointers, Sliding window, BFS/DFS, DP)
- Time/Space complexity analysis (Big O)
- Kotlin-specific implementations
- Code readability and organization

**Practice Tasks:**
- [ ] Solve 5 LeetCode medium problems in Kotlin
- [ ] Practice explaining solutions aloud
- [ ] Review Big O analysis for common operations
- [ ] Time yourself solving problems

**Resources:**
- LeetCode: https://leetcode.com/
- Pramp (Mock Interviews): https://www.pramp.com/

#### Day 7: Final Mock Interview
**Practice Tasks:**
- [ ] Full mock interview with friend or tool
- [ ] Review all previous weeks' notes
- [ ] Prepare questions for interviewers
- [ ] Review your own projects for discussion

---

## Key Takeaways Checklist

### Technical Knowledge
- [ ] Master Kotlin fundamentals (data classes, sealed classes, extensions, null safety)
- [ ] Understand Android lifecycles thoroughly
- [ ] Can explain Coroutines: Dispatchers, suspend functions, Job lifecycle, structured concurrency
- [ ] Know Flow vs LiveData differences and use cases
- [ ] Understand thread safety and synchronization strategies
- [ ] Can describe MVVM, MVI, and Clean Architecture with layer responsibilities
- [ ] Know DI principles and benefits
- [ ] Can write unit tests for ViewModels and Use Cases
- [ ] Understand RecyclerView and layout performance optimization

### Soft Skills
- [ ] Can explain thought process clearly and concisely
- [ ] Practice active listening and clarifying questions
- [ ] Show proactive communication about edge cases
- [ ] Demonstrate autonomy in problem-solving
- [ ] Can discuss trade-offs and make reasonable assumptions

### Interview Tips
- [ ] **Time Management:** Balance coverage across topics
- [ ] **Ask Questions:** Clarify requirements before jumping in
- [ ] **Think Aloud:** Share reasoning process
- [ ] **Handle Edge Cases:** Proactively mention and handle them
- [ ] **Correctness First:** Then optimize for performance
- [ ] **Stay Collaborative:** Show you can work in a team

---

## Sample Questions to Expect

### Kotlin
1. "What's the difference between `let`, `run`, `apply`, `also`, and `with`?"
2. "Explain sealed classes and when you'd use them"
3. "How do extension functions work in Kotlin?"
4. "Explain reified types in Kotlin"

### Android
1. "Walk me through the Activity lifecycle"
2. "How does ViewModel survive configuration changes?"
3. "What's the difference between onCreate and onStart?"
4. "How would you handle process death?"

### Coroutines
1. "What's structured concurrency?"
2. "How does coroutine cancellation work?"
3. "What's the difference between launch and async?"
4. "Explain the different Dispatchers and when to use each"

### Architecture
1. "Compare MVVM and MVI - when would you use each?"
2. "What are the layers in Clean Architecture?"
3. "What are the benefits of Dependency Injection?"
4. "How do you handle UI state management?"

### Testing
1. "How do you test ViewModels with coroutines?"
2. "What's the difference between a mock and a stub?"
3. "How do you make code more testable?"
4. "What's your approach to testing in Android?"

### Performance
1. "How do you optimize RecyclerView performance?"
2. "What causes overdraw and how do you fix it?"
3. "How do you detect memory leaks?"
4. "What tools do you use for performance profiling?"

---

## Questions to Ask Interviewers

Prepare 3-5 questions to ask at the end:

1. "What does a typical day look like for an Android engineer here?"
2. "How do you approach technical debt and refactoring?"
3. "What's the biggest challenge the Android team is currently facing?"
4. "How do you balance shipping features vs code quality?"
5. "What technologies and architecture is the team using?"
6. "How does the team handle code reviews?"
7. "What opportunities are there for learning and growth?"

---

## Resources Summary

### Official Documentation
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Android Developer Guide](https://developer.android.com/guide)
- [Architecture Components](https://developer.android.com/topic/libraries/architecture)
- [Coroutines on Android](https://developer.android.com/kotlin/coroutines)

### Books
- "Kotlin in Action" by Dmitry Jemerov and Svetlana Isakova
- "Clean Architecture" by Robert C. Martin
- "Java Concurrency in Practice" by Brian Goetz

### Practice Platforms
- [LeetCode](https://leetcode.com/) - Algorithms and data structures
- [Kotlin Koans](https://play.kotlinlang.org/koans/overview) - Kotlin practice
- [Pramp](https://www.pramp.com/) - Free mock interviews

### Tools
- Android Studio (Profiler, Layout Inspector, Debugger)
- LeakCanary (Memory leak detection)
- Flipper (Debugging platform)

---

## Daily Study Schedule Template

| Time | Activity |
|------|----------|
| Morning (1 hour) | Theory review (read articles, watch videos) |
| Afternoon (1-2 hours) | Hands-on coding practice |
| Evening (30 min) | Flashcards/review notes |
| Weekend | Mock interview practice |

---

## Progress Tracker

| Week | Topic | Completion Date | Confidence (1-5) |
|------|-------|-----------------|------------------|
| 1 | Kotlin & Android Fundamentals | | |
| 2 | Asynchronous Programming & Concurrency | | |
| 3 | Architecture & Design Patterns | | |
| 4 | Testing | | |
| 5 | UI Optimization & Performance | | |
| 6 | Problem-Solving & Final Prep | | |

---

## Good Luck! 🚀

Remember:
- **Preparation is key** - Consistent daily practice beats cramming
- **Communicate clearly** - The interview is about your thought process, not just the answer
- **Show your passion** - Enthusiasm for Android development goes a long way
- **Be honest** - If you don't know something, say so and explain how you'd find out

**You've got this!**
