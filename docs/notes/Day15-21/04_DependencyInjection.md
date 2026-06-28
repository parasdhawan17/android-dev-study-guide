# Dependency Injection

DAY 15-21: Dependency Injection

Goal: Learn how DI makes dependencies explicit, replaceable, and testable.

## 1. What Is Di?

Dependency Injection means a class receives its dependencies from the outside instead of
constructing them internally.

Bad:
class ViewModel { val repo = RealRepository(RetrofitApi()) }

Good:
class ViewModel(private val repo: Repository)

Benefits:
- Easier testing: pass fake repository.
- Clear dependencies.
- Centralized object creation.
- Supports scopes: singleton, activity, ViewModel.

````kotlin
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    suspend fun login(email: String, password: String): Result<User> {
        return authRepository.login(email, password)
    }
}
````

## 2. Manual Di

Manual DI is often enough for small apps.
Create dependencies in an application container and pass them down.

````kotlin
class AppContainer {
    private val api = AuthApi(baseUrl = "https://api.example.com")
    private val authRepository = AuthRepositoryImpl(api)

    fun loginViewModel(): LoginViewModel = LoginViewModel(authRepository)
}

class MyApplication : Application() {
    val container = AppContainer()
}
````

## 3. Hilt Concepts

@Inject constructor:
- Tells Hilt how to create a class.

@Module + @Provides:
- Used when Hilt cannot construct something itself (interfaces, builders, Retrofit).

@Binds:
- Binds interface to implementation.

Scopes:
- @Singleton: one instance for app.
- @ActivityRetainedScoped: survives config change, tied to activity retained graph.
- @ViewModelScoped: one per ViewModel.

@HiltViewModel
class LoginViewModel @Inject constructor(
private val authRepository: AuthRepository
) : ViewModel()

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
@Binds
abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
@Provides
@Singleton
fun provideAuthApi(): AuthApi = Retrofit.Builder()
.baseUrl("https://api.example.com")
.build()
.create(AuthApi::class.java)
}

## 4. Testing With Di

DI makes tests simple because production dependencies can be replaced.

class FakeAuthRepository : AuthRepository {
override suspend fun login(email: String, password: String) = Result.success(User("1"))
}

@Test
fun login_success() = runTest {
val viewModel = LoginViewModel(FakeAuthRepository())
assertTrue(viewModel.login("a", "b").isSuccess)
}

## Interview Questions

Q: What problem does DI solve?
A: It decouples object creation from object behavior, making code easier to test, configure,
and reason about.

Q: Manual DI vs Hilt?
A: Manual DI is explicit and simple for small graphs. Hilt reduces boilerplate and manages
scopes for larger Android apps.

Q: What is a scope?
A: A scope controls how long a provided instance lives and which consumers share it.

````kotlin
interface AuthRepository { suspend fun login(email: String, password: String): Result<User> }
class AuthRepositoryImpl(private val api: AuthApi) : AuthRepository { override suspend fun login(email: String, password: String) = Result.success(User("1")) }
class AuthApi(val baseUrl: String)
data class User(val id: String)
open class Application
open class ViewModel
````
