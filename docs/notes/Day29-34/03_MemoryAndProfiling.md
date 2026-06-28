# Memory And Profiling

DAY 29-34: Memory Leaks, Profiling, and Bitmap Optimization

## 1. Memory Leaks

A memory leak happens when an object is no longer needed but is still strongly referenced.
Android common leaks:
- Holding Activity Context in singleton.
- Fragment binding not cleared in onDestroyView.
- Listeners/callbacks not unregistered.
- Handler delayed messages referencing Activity.
- Long-lived coroutine scope capturing View.

````kotlin
class SafeFragment : Fragment() {
    private var _binding: Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(): View {
        _binding = Binding()
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null // critical: Fragment may outlive its view
    }
}
````

## 2. Leakcanary

LeakCanary watches destroyed Activities/Fragments and reports retained objects.
When reading a leak trace:
- Start from GC root.
- Follow strong reference chain.
- Find the reference that should have been cleared.

## 3. Bitmap Optimization

Bitmaps can consume lots of memory.
Memory = width * height * bytesPerPixel.
ARGB_8888 = 4 bytes per pixel.
A 4000x3000 bitmap is about 48MB.

Fixes:
- Decode at target size.
- Use Coil/Glide.
- Prefer vector drawables where appropriate.
- Avoid keeping Bitmap references in static/singleton objects.

## 4. Android Profiler

Memory profiler:
- Watch allocation spikes.
- Capture heap dump.
- Inspect retained objects.

CPU profiler:
- Find expensive methods.
- Check main-thread work.

Network profiler:
- Verify request count, payload size, caching.

## Interview Questions

Q: How would you find a memory leak?
A: Reproduce the screen lifecycle, use LeakCanary/heap dump, inspect reference chain, identify
unexpected strong reference, then clear/unregister or change ownership.

Q: Why clear Fragment binding in onDestroyView?
A: Fragment instance can remain on back stack after its View is destroyed. Binding holds the view
tree, so not clearing it leaks the old views/context.

````kotlin
open class Fragment { open fun onCreateView(): View = View(); open fun onDestroyView() {} }
open class View
class Binding { val root = View() }
````
