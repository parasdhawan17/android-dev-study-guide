# Day 8-14: Coroutines, Flow, Concurrency - Quick Reference

## Coroutines

| Concept | Meaning | Interview Signal |
|---|---|---|
| Coroutine | Lightweight asynchronous task | Not a thread; can suspend without blocking |
| Scope | Parent/lifecycle boundary for coroutines | Prevents leaks through structured concurrency |
| Job | Handle to coroutine lifecycle | Can cancel, join, inspect state |
| `launch` | Starts side-effect coroutine | Returns `Job` |
| `async` | Starts result-producing coroutine | Returns `Deferred<T>`; call `await()` |
| `withContext` | Switch dispatcher and return result | Common repository boundary pattern |

## Dispatchers

| Dispatcher | Use For | Avoid |
|---|---|---|
| `Main` | UI updates, collecting UI state | Blocking IO |
| `IO` | Database, files, blocking network | CPU-heavy loops |
| `Default` | CPU work, sorting, parsing | Long blocking IO |
| `Unconfined` | Rare tests/special cases | Normal app code |

## Flow

| Type | Best For | Key Property |
|---|---|---|
| `Flow` | Cold async streams | Starts when collected |
| `StateFlow` | UI state | Always has current value, replays latest |
| `SharedFlow` | Events | Configurable replay/buffer |
| `Channel` | Point-to-point messages | Queue-like communication |

## Key Flow Operators

- `map`: transform each emission.
- `filter`: keep matching emissions.
- `combine`: combine latest values from multiple flows.
- `flatMapLatest`: switch to latest inner flow; cancel previous.
- `debounce`: wait for quiet period, useful for search.
- `catch`: handle upstream exceptions.
- `onStart`: emit loading state before upstream begins.
- `collectLatest`: cancel previous collector block on new value.

## Thread Safety

| Tool | Use When |
|---|---|
| `synchronized` | Small thread-based critical section |
| `AtomicInteger` / `AtomicReference` | Simple atomic counters or references |
| `Mutex` | Protect shared state in coroutines |
| `Semaphore` | Limit concurrent work |
| Actor / Channel | One coroutine owns mutable state |

## Handler / Looper

```
Handler.post { work }
        -> MessageQueue
        -> Looper picks message
        -> Executes on target thread
```

Main thread has a Looper. UI work must run there.

## Common Interview Answers

**Structured concurrency:** Child coroutines are tied to parent scope. Parent cancellation cancels children, and parent completion waits for children.

**Coroutine cancellation:** Cooperative. Suspension points check cancellation. For CPU loops, call `ensureActive()` or `yield()`.

**StateFlow vs SharedFlow:** StateFlow is for state and always has a value. SharedFlow is for events and can replay zero values.

**Work on Main vs IO vs Default:** UI on Main, blocking IO on IO, CPU-heavy work on Default.
