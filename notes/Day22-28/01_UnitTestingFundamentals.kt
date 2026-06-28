/**
 * DAY 22-28: Unit Testing Fundamentals
 *
 * Goal: Write tests that document behavior and catch regressions without knowing implementation details.
 */

// ============ 1. ARRANGE - ACT - ASSERT ============
/*
Arrange: build inputs and dependencies.
Act: call the unit under test.
Assert: verify result or observable behavior.

A good test name reads like a requirement:
whenInvalidEmailSubmitted_returnsValidationError
*/

class EmailValidator {
    fun validate(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Invalid("Email is required")
            "@" !in email -> ValidationResult.Invalid("Email is invalid")
            else -> ValidationResult.Valid
        }
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

class EmailValidatorTest {
    private val validator = EmailValidator()

    @Test
    fun blankEmail_returnsRequiredError() {
        // Arrange
        val email = ""

        // Act
        val result = validator.validate(email)

        // Assert
        assertEquals(ValidationResult.Invalid("Email is required"), result)
    }
}

// ============ 2. TEST DOUBLES ============
/*
Dummy: passed around but not used.
Fake: working lightweight implementation.
Stub: returns predefined answers.
Mock: verifies interactions.
Spy: real object with observation/partial stubbing.

Interview tip:
Prefer fakes for domain/use case tests when possible. Mocks are useful for verifying important
side effects, but too many interaction tests become brittle.
*/

interface UserRepository { suspend fun getUser(id: String): User }
data class User(val id: String, val name: String)

class FakeUserRepository : UserRepository {
    val users = mutableMapOf<String, User>()
    override suspend fun getUser(id: String): User = users[id] ?: error("Not found")
}

class GetUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(id: String): Result<User> = runCatching {
        require(id.isNotBlank()) { "id is required" }
        repository.getUser(id)
    }
}

// ============ 3. PARAMETERIZED TESTS ============
/*
Parameterized tests reduce duplication for input/output pairs.
Good for validation, formatting, parsing, edge cases.
*/

/*
@RunWith(Parameterized::class)
class EmailValidatorParameterizedTest(
    private val input: String,
    private val expectedValid: Boolean
) {
    companion object {
        @JvmStatic
        @Parameters
        fun data() = listOf(
            arrayOf("", false),
            arrayOf("missing-at", false),
            arrayOf("valid@test.com", true)
        )
    }
}
*/

// ============ 4. WHAT TO TEST ============
/*
High-value unit tests:
- Branching business rules.
- Error handling.
- Edge cases and boundaries.
- Mapping logic between layers.
- Reducers and pure functions.

Low-value unit tests:
- Simple getters/setters.
- Framework behavior.
- Implementation details that change often.
*/

// ============ INTERVIEW QUESTIONS ============
/*
Q: Mock vs stub?
A: A stub provides canned data. A mock verifies interactions. A fake is a lightweight working
   implementation.

Q: What makes code testable?
A: Clear dependencies, pure functions where possible, small units, injected collaborators, and
   avoiding hidden global state.
*/

annotation class Test
fun assertEquals(expected: Any?, actual: Any?) {}
