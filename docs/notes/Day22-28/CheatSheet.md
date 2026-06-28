# Day 22-28: Testing - Quick Reference

## Test Structure

```
Arrange -> Act -> Assert
```

Name tests by behavior:
`blankEmail_returnsRequiredError`

## Test Doubles

| Type | Meaning |
|---|---|
| Dummy | Required but unused object |
| Fake | Lightweight working implementation |
| Stub | Returns predefined values |
| Mock | Verifies interactions |
| Spy | Real object with observation/partial stubbing |

## Coroutine Testing

| Tool | Use |
|---|---|
| `runTest` | Run coroutine tests with virtual time |
| `StandardTestDispatcher` | Queued deterministic execution |
| `advanceUntilIdle()` | Run pending coroutine work |
| `MainDispatcherRule` | Replace `Dispatchers.Main` |
| Turbine | Assert Flow emissions in order |

## What To Test

High value:
- Business rules.
- Error paths.
- Edge cases.
- ViewModel state changes.
- Reducers and mappers.
- Repository source/caching logic.

Low value:
- Getters/setters.
- Framework internals.
- Implementation details that do not affect behavior.

## Common Interview Answers

**Mock vs stub:** Stub returns canned data. Mock verifies interactions.

**Why fakes?** They behave like small real implementations and often make tests less brittle.

**Testing ViewModel with coroutines:** Replace Main dispatcher, call ViewModel method, `advanceUntilIdle()`, assert StateFlow state or Flow emissions.

**Testing strategy:** Many unit tests, some integration tests, few end-to-end tests for critical journeys.
