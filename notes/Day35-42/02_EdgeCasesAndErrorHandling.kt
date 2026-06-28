/**
 * DAY 35-42: Edge Cases and Error Handling
 */

// ============ 1. EDGE CASE CHECKLIST ============
/*
Inputs:
- null / missing / blank
- empty collections
- one item
- very large values
- duplicates
- invalid format
- boundary numbers: 0, 1, max, min

Android/system:
- rotation/config change
- process death
- offline/slow network
- permission denied
- low storage
- background restrictions
- time zone/language changes
*/

// ============ 2. RESULT PATTERN ============
/*
Throw exceptions for exceptional conditions at low levels.
At UI/domain boundaries, convert to explicit Result/Error state so UI can render.
*/

sealed class AppError {
    object NetworkUnavailable : AppError()
    object Unauthorized : AppError()
    data class Server(val code: Int) : AppError()
    data class Unknown(val message: String) : AppError()
}

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Failure(val error: AppError) : AppResult<Nothing>()
}

suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> {
    return try {
        AppResult.Success(block())
    } catch (e: UnauthorizedException) {
        AppResult.Failure(AppError.Unauthorized)
    } catch (e: NetworkException) {
        AppResult.Failure(AppError.NetworkUnavailable)
    } catch (e: Exception) {
        AppResult.Failure(AppError.Unknown(e.message ?: "Unknown"))
    }
}

// ============ 3. RETRY WITH BACKOFF ============
/*
Retry only transient failures.
Do not retry validation errors or unauthorized forever.
Use exponential backoff to avoid hammering services.
*/

suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 500,
    block: suspend () -> T
): T {
    var delayMs = initialDelayMs
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (!e.isTransient()) throw e
            delay(delayMs)
            delayMs *= 2
        }
    }
    return block()
}

fun Exception.isTransient(): Boolean = this is NetworkException || this is TimeoutException

// ============ 4. GRACEFUL DEGRADATION ============
/*
Examples:
- Show cached content when refresh fails.
- Disable unavailable action with explanation.
- Let user retry.
- Queue work for later with WorkManager.
- Show partial content instead of blank screen.
*/

// ============ INTERVIEW QUESTIONS ============
/*
Q: How do you identify edge cases?
A: Walk through inputs, boundaries, lifecycle/system conditions, network states, permissions,
   and user behavior like rapid taps or navigation away mid-request.

Q: When should you retry?
A: Retry transient failures like network timeouts. Do not blindly retry validation, auth, or
   deterministic server errors.
*/

class UnauthorizedException : Exception()
class NetworkException : Exception()
class TimeoutException : Exception()
