/**
 * DAY 15-21: MVVM Pattern
 *
 * Goal: Understand responsibility boundaries, not just class names.
 */

// ============ 1. MVVM MENTAL MODEL ============
/*
MVVM splits screen code into:

View:
- Activity, Fragment, or Composable.
- Renders state and forwards user actions.
- Should contain minimal business logic.

ViewModel:
- Holds UI state.
- Handles UI events.
- Calls use cases/repositories.
- Survives configuration changes.

Model:
- Domain/data layer objects and operations.
- Repository, use cases, database, network.
*/

// ============ 2. STATE, EVENT, EFFECT ============
/*
A strong ViewModel API usually separates:

State:
- Persistent representation of the screen.
- Loading, data, error, selected tab.
- Use StateFlow.

Event/Action:
- User input sent from UI to ViewModel.
- Search changed, retry clicked, item selected.

Effect:
- One-time instruction to UI.
- Navigate, show snackbar, request permission.
- Use SharedFlow or Channel.
*/

data class ProfileState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

sealed class ProfileAction {
    object RetryClicked : ProfileAction()
    data class UserNameChanged(val value: String) : ProfileAction()
}

sealed class ProfileEffect {
    data class ShowSnackbar(val message: String) : ProfileEffect()
    data class NavigateToDetails(val userId: String) : ProfileEffect()
}

class ProfileViewModel(private val getProfile: GetProfileUseCase) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ProfileEffect>()
    val effects: SharedFlow<ProfileEffect> = _effects.asSharedFlow()

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.RetryClicked -> load()
            is ProfileAction.UserNameChanged -> updateName(action.value)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = ProfileState(isLoading = true)
            getProfile().fold(
                onSuccess = { _state.value = ProfileState(user = it) },
                onFailure = {
                    _state.value = ProfileState(error = it.message)
                    _effects.emit(ProfileEffect.ShowSnackbar("Could not load profile"))
                }
            )
        }
    }

    private fun updateName(value: String) {
        _state.value = _state.value.copy(user = _state.value.user?.copy(name = value))
    }
}

// ============ 3. VIEW SHOULD BE THIN ============
/*
Good View responsibilities:
- Collect state with lifecycle awareness.
- Render based on state.
- Forward clicks/text changes.
- Collect effects and perform UI-only operations.

Bad View responsibilities:
- Calling API directly.
- Deciding business rules.
- Transforming domain data in complex ways.
*/

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProfileContent(
        state = state,
        onRetry = { viewModel.onAction(ProfileAction.RetryClicked) },
        onNameChanged = { viewModel.onAction(ProfileAction.UserNameChanged(it)) }
    )

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProfileEffect.ShowSnackbar -> showSnackbar(effect.message)
                is ProfileEffect.NavigateToDetails -> navigate(effect.userId)
            }
        }
    }
}

// ============ 4. COMMON PITFALLS ============
/*
Pitfall: Exposing MutableStateFlow directly.
Fix: Keep MutableStateFlow private, expose StateFlow.

Pitfall: Putting Context in ViewModel.
Fix: Inject application-safe abstractions, repositories, resource providers if needed.

Pitfall: StateFlow for one-time navigation.
Fix: Use SharedFlow/Channel effects.

Pitfall: ViewModel knows about Composables or Views.
Fix: ViewModel exposes plain Kotlin state and events.
*/

// ============ INTERVIEW QUESTIONS ============
/*
Q: What belongs in a ViewModel?
A: UI state, event handling, orchestration of use cases/repositories, and transformation from
   domain result into UI state. It should not know concrete Views.

Q: How do you handle one-time events?
A: Use a separate effect stream such as SharedFlow with replay = 0. Do not store navigation as
   durable UI state unless the screen should re-navigate after recreation.
*/

// Stubs
data class User(val id: String, val name: String)
fun interface GetProfileUseCase { suspend operator fun invoke(): Result<User> }
open class ViewModel { val viewModelScope = CoroutineScope(Dispatchers.Main) }
@Composable fun ProfileContent(state: ProfileState, onRetry: () -> Unit, onNameChanged: (String) -> Unit) {}
@Composable fun ProfileScreenPreview() {}
fun showSnackbar(message: String) {}
fun navigate(userId: String) {}
