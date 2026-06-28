# View Model And Coroutine Testing

DAY 22-28: ViewModel and Coroutine Testing

Goal: Test async state changes deterministically.

## 1. Why Coroutine Testing Needs Special Tools

Normal coroutine delays and dispatchers use real time/threads.
Tests need virtual time so they are fast and deterministic.

Tools:
- runTest: test coroutine scope with virtual time.
- StandardTestDispatcher: queued execution; call advanceUntilIdle().
- UnconfinedTestDispatcher: starts eagerly; useful but can hide ordering issues.
- MainDispatcherRule: replaces Dispatchers.Main in tests.

````kotlin
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
````

## 2. Viewmodel Test Example

````kotlin
data class ProfileState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

class ProfileViewModel(private val repository: UserRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            _state.value = ProfileState(isLoading = true)
            try {
                val user = repository.getUser(id)
                _state.value = ProfileState(user = user)
            } catch (e: Exception) {
                _state.value = ProfileState(error = e.message)
            }
        }
    }
}

class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_success_updatesStateToUser() = runTest {
        val repository = FakeUserRepository(User("1", "Ada"))
        val viewModel = ProfileViewModel(repository)

        viewModel.load("1")
        advanceUntilIdle()

        assertEquals(ProfileState(user = User("1", "Ada")), viewModel.state.value)
    }
}
````

## 3. Testing Flow Emissions

Options:
- Direct state.value after advanceUntilIdle() for StateFlow final state.
- Turbine library for ordered emissions.

Turbine example:
viewModel.state.test {
assertEquals(Initial, awaitItem())
viewModel.load()
assertEquals(Loading, awaitItem())
assertEquals(Success(data), awaitItem())
}

## 4. Common Pitfalls

Pitfall: Forgetting advanceUntilIdle() with StandardTestDispatcher.
Pitfall: Testing implementation order instead of externally visible behavior.
Pitfall: Not replacing Dispatchers.Main.
Pitfall: Injecting dispatchers poorly; prefer DispatcherProvider for repositories/use cases.

````kotlin
interface DispatcherProvider { val io: CoroutineDispatcher; val default: CoroutineDispatcher; val main: CoroutineDispatcher }
class TestDispatchers(private val dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io = dispatcher
    override val default = dispatcher
    override val main = dispatcher
}
````

Stubs

````kotlin
interface UserRepository { suspend fun getUser(id: String): User }
class FakeUserRepository(private val user: User) : UserRepository { override suspend fun getUser(id: String) = user }
data class User(val id: String, val name: String)
open class ViewModel { val viewModelScope = CoroutineScope(Dispatchers.Main) }
annotation class Test
annotation class Rule
annotation class get Rule
open class TestWatcher { open fun starting(description: Description) {}; open fun finished(description: Description) {} }
class Description
fun assertEquals(expected: Any?, actual: Any?) {}
````
