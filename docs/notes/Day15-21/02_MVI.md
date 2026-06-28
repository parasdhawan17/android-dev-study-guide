# MVI

DAY 15-21: MVI Pattern

Goal: Learn unidirectional data flow and reducer-style state updates.

## 1. MVI Mental Model

MVI emphasizes a single loop:

User Intent -&gt; Processor -&gt; Result -&gt; Reducer -&gt; State -&gt; View

Intent:
- What user or system wants to do.

State:
- Single immutable screen model.

Reducer:
- Pure function: old state + result = new state.

Why teams use MVI:
- Predictable state transitions.
- Easier debugging: every change has a source.
- Works well with Compose and immutable state.

````kotlin
data class TransferState(
    val amount: String = "",
    val recipient: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val canSubmit: Boolean = false
)

sealed class TransferIntent {
    data class AmountChanged(val amount: String) : TransferIntent()
    data class RecipientChanged(val recipient: String) : TransferIntent()
    object SubmitClicked : TransferIntent()
}

sealed class TransferResult {
    data class AmountUpdated(val amount: String) : TransferResult()
    data class RecipientUpdated(val recipient: String) : TransferResult()
    object SubmitStarted : TransferResult()
    object SubmitSucceeded : TransferResult()
    data class SubmitFailed(val message: String) : TransferResult()
}

fun reduce(state: TransferState, result: TransferResult): TransferState {
    val next = when (result) {
        is TransferResult.AmountUpdated -> state.copy(amount = result.amount, error = null)
        is TransferResult.RecipientUpdated -> state.copy(recipient = result.recipient, error = null)
        TransferResult.SubmitStarted -> state.copy(isSubmitting = true, error = null)
        TransferResult.SubmitSucceeded -> state.copy(isSubmitting = false)
        is TransferResult.SubmitFailed -> state.copy(isSubmitting = false, error = result.message)
    }
    return next.copy(canSubmit = next.amount.isNotBlank() && next.recipient.isNotBlank() && !next.isSubmitting)
}

class TransferViewModel(private val repository: TransferRepository) : ViewModel() {
    private val _state = MutableStateFlow(TransferState())
    val state: StateFlow<TransferState> = _state.asStateFlow()

    fun accept(intent: TransferIntent) {
        when (intent) {
            is TransferIntent.AmountChanged -> dispatch(TransferResult.AmountUpdated(intent.amount))
            is TransferIntent.RecipientChanged -> dispatch(TransferResult.RecipientUpdated(intent.recipient))
            TransferIntent.SubmitClicked -> submit()
        }
    }

    private fun dispatch(result: TransferResult) {
        _state.value = reduce(_state.value, result)
    }

    private fun submit() {
        viewModelScope.launch {
            dispatch(TransferResult.SubmitStarted)
            try {
                repository.submit(_state.value.amount, _state.value.recipient)
                dispatch(TransferResult.SubmitSucceeded)
            } catch (e: Exception) {
                dispatch(TransferResult.SubmitFailed(e.message ?: "Transfer failed"))
            }
        }
    }
}
````

## 2. MVVM vs MVI

MVVM:
- Often simpler and less boilerplate.
- ViewModel methods can update state directly.
- Good for most CRUD and moderate screens.

MVI:
- More ceremony, but very explicit.
- Best for complex screens with many state transitions.
- Reducers make behavior easier to audit and test.

Interview answer:
Choose MVVM for straightforward screens. Use MVI when explicit state transitions and
unidirectional flow reduce complexity.

## 3. Testing Reducers

Reducers are pure functions, so tests are simple.

@Test
fun amountUpdated_enablesSubmitWhenRecipientExists() {
val initial = TransferState(recipient = "Alice")
val next = reduce(initial, TransferResult.AmountUpdated("100"))
assertTrue(next.canSubmit)
}

## Interview Questions

Q: What is unidirectional data flow?
A: Data moves in one direction: actions/intents enter ViewModel, state is reduced, UI renders
state, and new user input starts the loop again.

Q: What is a reducer?
A: A pure function that receives previous state plus a result/action and returns the next state.

````kotlin
interface TransferRepository { suspend fun submit(amount: String, recipient: String) }
open class ViewModel { val viewModelScope = CoroutineScope(Dispatchers.Main) }
````
