# Use Case And Repository Testing

DAY 22-28: Use Case and Repository Testing

Goal: Test business rules separately from Android/framework details.

## 1. Use Case Testing

Use cases are ideal unit-test targets:
- Plain Kotlin.
- Business rules are explicit.
- Dependencies can be faked.

````kotlin
data class Money(val cents: Long, val currency: String)
interface TransferRepository { suspend fun transfer(from: String, to: String, amount: Money) }

class TransferMoneyUseCase(private val repository: TransferRepository) {
    suspend operator fun invoke(from: String, to: String, amount: Money): Result<Unit> {
        if (from == to) return Result.failure(IllegalArgumentException("Same account"))
        if (amount.cents <= 0) return Result.failure(IllegalArgumentException("Invalid amount"))
        return runCatching { repository.transfer(from, to, amount) }
    }
}

class FakeTransferRepository : TransferRepository {
    val calls = mutableListOf<Triple<String, String, Money>>()
    var shouldFail = false

    override suspend fun transfer(from: String, to: String, amount: Money) {
        if (shouldFail) error("Network failed")
        calls += Triple(from, to, amount)
    }
}
````

## 2. Repository Testing

Repository tests verify orchestration:
- Reads from cache vs network.
- Writes remote response to local DB.
- Maps DTO/entity to domain.
- Handles errors and fallback behavior.

Do not unit test Retrofit or Room internals. Test your logic around them.

````kotlin
data class UserDto(val id: String, val displayName: String)
data class UserEntity(val id: String, val name: String)
data class User(val id: String, val name: String)

fun UserDto.toEntity() = UserEntity(id, displayName)
fun UserEntity.toDomain() = User(id, name)

interface UserApi { suspend fun getUser(id: String): UserDto }
interface UserDao { suspend fun getUser(id: String): UserEntity?; suspend fun insert(entity: UserEntity) }

class UserRepositoryImpl(private val api: UserApi, private val dao: UserDao) {
    suspend fun getUser(id: String, forceRefresh: Boolean): User {
        if (!forceRefresh) {
            dao.getUser(id)?.let { return it.toDomain() }
        }
        val remote = api.getUser(id)
        dao.insert(remote.toEntity())
        return remote.toEntity().toDomain()
    }
}
````

## 3. Mapping Tests

Mapping bugs are common and cheap to test.
Include nulls, missing fields, currency/amount conversion, date parsing, enum fallback.

## Interview Questions

Q: How do you test a use case?
A: Provide fake repositories, exercise success/error/edge cases, and assert returned domain result
plus important repository interactions.

Q: What should repository tests cover?
A: Cache policy, source selection, mapping, persistence side effects, and error fallback behavior.
