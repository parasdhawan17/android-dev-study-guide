# Testable Code Design

DAY 22-28: Testable Code Design and Integration Strategy

## 1. Test Pyramid For Android

Many fast unit tests:
- Use cases, reducers, validators, mappers, ViewModels.

Some integration tests:
- Repository with fake server and in-memory DB.
- Navigation flows and important screen behavior.

Few end-to-end tests:
- Critical user journeys only.

## 2. Design For Testability

Prefer:
- Constructor injection.
- Interfaces at meaningful boundaries.
- Pure functions for transformation.
- Dispatcher injection for coroutine code.
- Clock/time provider injection for time-dependent behavior.

Avoid:
- Static singletons for business logic.
- Hard-coded Dispatchers.IO in hard-to-test places.
- Reading system time directly deep in logic.
- Large methods mixing UI, network, and business rules.

````kotlin
interface Clock { fun nowMillis(): Long }
class SystemClock : Clock { override fun nowMillis(): Long = System.currentTimeMillis() }

class SessionValidator(private val clock: Clock) {
    fun isExpired(expiresAtMillis: Long): Boolean = clock.nowMillis() >= expiresAtMillis
}

class FakeClock(var now: Long) : Clock { override fun nowMillis() = now }
````

## 3. Integration Testing

Integration tests check that multiple real components work together.
Examples:
- Repository + Room in-memory database + fake API.
- ViewModel + use case + fake repository.
- Compose screen + fake ViewModel/state.

Keep integration tests focused: one behavior, not the entire app.

## 4. Test Maintainability

Signs of brittle tests:
- Test fails when internal implementation changes but behavior is same.
- Too many mocks verify every method call.
- Huge setup repeated in many files.

Improve with:
- Test data builders.
- Fakes over mocks when possible.
- Assert behavior visible to caller/user.

````kotlin
data class UserBuilder(
    var id: String = "1",
    var name: String = "Ada"
) {
    fun withName(value: String) = apply { name = value }
    fun build() = User(id, name)
}

data class User(val id: String, val name: String)
````

## Interview Questions

Q: What is your testing strategy?
A: Unit test business rules and ViewModels heavily, add integration tests around repositories and
important flows, and reserve end-to-end tests for critical user journeys.

Q: How do you make coroutine code testable?
A: Use runTest, replace Dispatchers.Main, inject dispatchers where needed, and avoid real delays.
