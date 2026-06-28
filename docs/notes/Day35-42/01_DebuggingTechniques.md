# Debugging Techniques

DAY 35-42: Debugging Techniques

Goal: Show a systematic debugging process in interviews and real work.

## 1. Debugging Loop

1. Reproduce reliably.
2. Define expected vs actual behavior.
3. Narrow scope with evidence.
4. Form one hypothesis at a time.
5. Test hypothesis with logs, debugger, trace, or small experiment.
6. Fix root cause, not symptom.
7. Add regression test if possible.

## 2. Stack Trace Reading

How to read a crash:
- Start at exception type and message.
- Find first line in your app code.
- Walk upward to see who called it.
- Ignore framework frames until they explain lifecycle/context.

Example:
NullPointerException at ProfileFragment.render(ProfileFragment.kt:42)
Question: Why was the value null there? Was state invalid, lifecycle wrong, or mapping wrong?

## 3. Logging Strategy

Good logs:
- Include operation id/request id/user action id.
- Log state transitions, not every line.
- Redact PII/secrets.
- Use levels: debug/info/warn/error.

````kotlin
class DebugLogger {
    fun logStateTransition(screen: String, old: String, new: String) {
        println("screen=$screen old=$old new=$new")
    }
}
````

## 4. Debugger Features

Use:
- Conditional breakpoints: stop only when id == target.
- Evaluate expression: inspect derived values.
- Watches: track variables over stepping.
- Drop frame: rerun a function after changing breakpoint/logging.
- Thread view: identify deadlocks or wrong-thread calls.

## 5. Network Debugging

Check:
- Request URL, method, headers.
- Auth token present and not expired.
- Response code/body.
- Serialization errors.
- Retry behavior.
- Offline/timeout behavior.

## Interview Questions

Q: How do you debug a crash you cannot reproduce locally?
A: Collect crash logs, device/OS/app version, breadcrumbs, recent changes, feature flags, and
affected user path. Add targeted logging/metrics, reproduce with similar environment, then
fix and add tests/guards.

Q: How do you debug an ANR?
A: Inspect traces for main-thread blockage, look for disk/network/lock waits, correlate with
recent code and lifecycle events, then move/blocking work or fix lock ordering.
