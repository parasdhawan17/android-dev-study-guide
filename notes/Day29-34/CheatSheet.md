# Day 29-34: UI Optimization & Performance - Quick Reference

## RecyclerView

| Topic | Key Point |
|---|---|
| ViewHolder | Reuses views instead of creating all rows |
| DiffUtil | Calculates minimal list updates |
| ListAdapter | AsyncListDiffer wrapper around DiffUtil |
| Payloads | Partial row updates |
| Image loading | Use Coil/Glide; avoid full-size bitmap decoding |
| Nested lists | Share RecycledViewPool, use stable keys/IDs |

## Layout Performance

- Flatten deep hierarchies.
- Use ConstraintLayout for complex view constraints.
- Use `<merge>` to avoid redundant parents.
- Use `ViewStub` for rarely shown UI.
- Remove redundant backgrounds to reduce overdraw.

## Memory

Common leaks:
- Activity context in singleton.
- Fragment binding not cleared.
- Listeners not unregistered.
- Delayed Handler callbacks.
- Long-lived scopes capturing Views.

Bitmap memory formula:
`width * height * bytesPerPixel`

## Startup / Jank / ANR

Startup improvements:
- Keep `Application.onCreate()` light.
- Defer SDK initialization.
- Avoid disk/network on main thread.
- Optimize first meaningful frame.

ANR prevention:
- Move blocking work off main thread.
- Keep BroadcastReceiver quick.
- Avoid main-thread locks.
- Use StrictMode in debug builds.

## Common Interview Answers

**RecyclerView performance:** DiffUtil, stable IDs where useful, payloads, efficient binding, image caching/cancellation, avoid heavy work in bind.

**Detect memory leaks:** LeakCanary, heap dump, inspect reference chain from GC root, clear or unregister the leaking reference.

**Debug jank:** Profile GPU Rendering, System Trace, CPU profiler, Layout Inspector, measure before and after.
