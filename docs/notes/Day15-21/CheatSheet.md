# Day 15-21: Architecture & Design Patterns - Quick Reference

## MVVM

| Layer | Responsibility |
|---|---|
| View | Render state, forward user actions, collect effects |
| ViewModel | Own UI state, handle events, call use cases/repositories |
| Model | Domain/data objects and operations |

State/Event/Effect pattern:
- State: durable screen model (`StateFlow`).
- Event/Action: user input into ViewModel.
- Effect: one-time UI instruction (`SharedFlow`).

## MVI

```
Intent -> Processor -> Result -> Reducer -> State -> View
```

Use MVI when a screen has many transitions and you want explicit, testable state updates.
Use MVVM when the screen is straightforward and lower ceremony is valuable.

## Clean Architecture

| Layer | Contains | Should Avoid |
|---|---|---|
| Presentation | UI, ViewModel, UI models | Database/network details |
| Domain | Use cases, domain models, repository interfaces | Android framework, Retrofit, Room |
| Data | Repository implementations, DTOs, entities | UI logic |

Dependency rule: dependencies point inward toward domain.

## Dependency Injection

Benefits:
- Testability through fakes/mocks.
- Clear dependency graph.
- Centralized object creation.
- Scoping of shared instances.

Hilt concepts:
- `@Inject constructor`: construct class automatically.
- `@Provides`: manually create dependency.
- `@Binds`: bind interface to implementation.
- `@Singleton`, `@ViewModelScoped`: lifetime control.

## Common Interview Answers

**Why not put business logic in Activity/Fragment?** Views are lifecycle-heavy and hard to test. Business logic belongs in ViewModel/use cases.

**Repository pattern:** Provides a stable API to data, hiding whether data comes from network, database, cache, or multiple sources.

**Use case:** A named business operation that coordinates rules and repositories.

**MVI reducer:** Pure function from old state plus result/action to new state.
