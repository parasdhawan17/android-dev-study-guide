/**
 * DAY 15-21: Clean Architecture
 *
 * Goal: Understand dependency direction and how layers protect business logic.
 */

// ============ 1. LAYERS ============
/*
Presentation:
- Activity, Fragment, Composable, ViewModel.
- Knows how to display state.

Domain:
- Business rules: use cases, domain models, repository interfaces.
- Should be plain Kotlin when possible.
- Should not depend on Android framework.

Data:
- Repository implementations, network, database, DataStore.
- Maps DTO/entity models to domain models.

Dependency Rule:
Outer layers depend inward. Domain should not know Retrofit, Room, or Android Views.
*/

// Domain model
data class Account(val id: String, val balance: Money)
data class Money(val cents: Long, val currency: String)

// Domain repository interface
interface AccountRepository {
    suspend fun getAccount(accountId: String): Account
    suspend fun transfer(from: String, to: String, amount: Money)
}

// Use case: business operation in one named place
class TransferMoneyUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(from: String, to: String, amount: Money): Result<Unit> {
        if (from == to) return Result.failure(IllegalArgumentException("Cannot transfer to same account"))
        if (amount.cents <= 0) return Result.failure(IllegalArgumentException("Amount must be positive"))

        return runCatching {
            repository.transfer(from, to, amount)
        }
    }
}

// Data DTO from API
data class AccountDto(val id: String, val balanceMinor: Long, val currency: String)

fun AccountDto.toDomain(): Account = Account(
    id = id,
    balance = Money(balanceMinor, currency)
)

class AccountRepositoryImpl(private val api: AccountApi) : AccountRepository {
    override suspend fun getAccount(accountId: String): Account {
        return api.getAccount(accountId).toDomain()
    }

    override suspend fun transfer(from: String, to: String, amount: Money) {
        api.transfer(TransferRequest(from, to, amount.cents, amount.currency))
    }
}

// ============ 2. WHY MAPPING MATTERS ============
/*
Avoid using DTOs directly in UI/domain.

Reasons:
- API shape can be awkward or unstable.
- Database entities may include persistence details.
- UI models may need display formatting.
- Domain should express business meaning, not transport details.
*/

// UI model tailored for rendering
data class AccountUiModel(val title: String, val formattedBalance: String)

fun Account.toUi(): AccountUiModel = AccountUiModel(
    title = "Account $id",
    formattedBalance = "${balance.currency} ${balance.cents / 100}.${balance.cents % 100}"
)

// ============ 3. USE CASES: WHEN TO CREATE ONE ============
/*
Use a use case when:
- Operation has business rules.
- Multiple repositories are coordinated.
- Logic is reused from multiple ViewModels.
- You want a testable domain boundary.

Maybe skip a use case when:
- ViewModel simply calls repository.getItems().
- Adding it would only pass-through without naming meaningful behavior.
*/

// ============ INTERVIEW QUESTIONS ============
/*
Q: What is the dependency rule?
A: Dependencies point inward. Presentation and data can depend on domain abstractions, but
   domain should not depend on Android, Retrofit, Room, or UI classes.

Q: Why use repository interfaces in domain?
A: They define what business logic needs without depending on how data is fetched. Data layer
   provides implementations.
*/

interface AccountApi { suspend fun getAccount(id: String): AccountDto; suspend fun transfer(request: TransferRequest) }
data class TransferRequest(val from: String, val to: String, val amountMinor: Long, val currency: String)
