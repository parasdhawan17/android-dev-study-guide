# RecyclerView Optimisations

## What is RecyclerView?

RecyclerView is a flexible, efficient ViewGroup used to display large sets of data. It recycles (reuses) item views that are no longer visible on screen, which is more memory-efficient than traditional `ListView`.

---

## 1. ViewHolder Pattern

Always use a `ViewHolder` to cache references to item views. Avoid calling `findViewById()` repeatedly during binding.

```kotlin
class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val titleText: TextView = view.findViewById(R.id.title_text)
    val iconImage: ImageView = view.findViewById(R.id.icon_image)
}
```

Using `ViewBinding` or `DataBinding` further reduces boilerplate and prevents runtime lookup errors.

---

## 2. setHasFixedSize(true)

When the RecyclerView's layout size is not affected by its adapter content, enable this flag.

```kotlin
recyclerView.setHasFixedSize(true)
```

This tells RecyclerView that it does not need to recalculate its own size when the adapter changes, which can save layout passes.

---

## 3. Efficient Layout Managers

Choose the simplest layout manager that meets your use case:

- `LinearLayoutManager`: best for vertical or horizontal lists.
- `GridLayoutManager`: for grids; use `GridLayoutManager.SpanSizeLookup` carefully to avoid excessive recalculations.
- `StaggeredGridLayoutManager`: only when variable heights are truly required.

Avoid deeply nested layouts inside item rows. Prefer `ConstraintLayout` or `Flat Layout` hierarchies.

---

## 4. Avoid Expensive Work in onBindViewHolder()

`onBindViewHolder()` runs frequently during scrolls. Keep it lightweight:

- Do not allocate new objects.
- Avoid complex logic, string formatting, or regex.
- Defer heavy work such as image decoding or database queries to background threads.

```kotlin
override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
    val item = data[position]
    holder.titleText.text = item.title
    imageLoader.load(item.imageUrl).into(holder.iconImage) // background loading
}
```

---

## 5. Image Loading Optimisations

Loading images is a common cause of scroll jank. Use a dedicated image loading library such as Coil, Glide, or Picasso.

Best practices:

- Resize images to match the view dimensions.
- Use caching (memory and disk).
- Clear or pause requests when views are recycled.
- Avoid loading large images directly on the main thread.

```kotlin
coil.load(imageUrl) {
    crossfade(true)
    size(ViewSizeResolver(holder.imageView))
}
```

---

## 6. DiffUtil for Data Updates

Do not call `notifyDataSetChanged()` for every change. It forces a full rebind and relayout. Use `DiffUtil` to calculate the minimal set of changes.

```kotlin
val diffResult = DiffUtil.calculateDiff(MyDiffCallback(oldList, newList))
oldList.clear()
oldList.addAll(newList)
diffResult.dispatchUpdatesTo(adapter)
```

Benefits:

- Item animations happen automatically.
- Only changed items are rebound.
- Reduces main-thread work.

For large lists, calculate `DiffUtil` on a background thread using `DiffUtil.calculateDiff()` with a callback and `ListAdapter`.

---

## 7. ListAdapter + AsyncListDiffer

`ListAdapter` handles `DiffUtil` asynchronously on a background thread.

```kotlin
class MyListAdapter : ListAdapter<Item, ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder { ... }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(old: Item, new: Item) = old.id == new.id
        override fun areContentsTheSame(old: Item, new: Item) = old == new
    }
}
```

---

## 8. Nested RecyclerViews

Avoid nesting RecyclerViews when possible. If necessary:

- Use a shared `RecycledViewPool` so inner RecyclerViews can reuse views.
- Set `setInitialPrefetchItemCount()` on the inner `LinearLayoutManager` for better scrolling in horizontal lists.

```kotlin
val pool = RecyclerView.RecycledViewPool()
innerRecyclerView.setRecycledViewPool(pool)
innerRecyclerView.setHasFixedSize(true)

(innerRecyclerView.layoutManager as LinearLayoutManager)
    .initialPrefetchItemCount = 4
```

---

## 9. Paging for Large Datasets

For very large or network-backed datasets, use the Paging library. It loads data in chunks, reducing memory usage and improving perceived performance.

Key components:

- `PagingSource`: defines how data pages are loaded.
- `Pager`: constructs a stream of `PagingData`.
- `PagingDataAdapter`: a RecyclerView adapter that displays paginated data.

```kotlin
val pagingData = Pager(PagingConfig(pageSize = 20)) {
    MyPagingSource()
}.flow.cachedIn(viewModelScope)
```

---

## 10. RecyclerView Pool and Prefetching

- `RecycledViewPool`: lets multiple RecyclerViews share scrap views.
- `GapWorker`: RecyclerView pre-fetches items during idle time in `LinearLayoutManager` by default (API 21+). Keep item layouts simple so prefetch can finish on time.

---

## 11. Avoid Layout Invalidations

Frequent view invalidations hurt scrolling performance:

- Avoid `wrap_content` in RecyclerView item root layouts; prefer fixed or `match_parent` dimensions.
- Avoid updating views that are off-screen.
- Use `requestLayout()` sparingly inside item views.

---

## 12. Custom Animations Carefully

`ItemAnimator` adds visual polish but can be expensive. Use `DefaultItemAnimator` or a custom animator that is minimal and efficient.

To disable animations entirely when not needed:

```kotlin
recyclerView.itemAnimator = null
```

---

## 13. View Recycling & Payloads

When only part of an item changes, use payloads with `notifyItemChanged(position, payload)` and override `onBindViewHolder(holder, position, payloads)` to update only the changed view.

```kotlin
override fun onBindViewHolder(holder: ItemViewHolder, position: Int, payloads: MutableList<Any>) {
    if (payloads.isEmpty()) {
        super.onBindViewHolder(holder, position, payloads)
    } else {
        for (payload in payloads) {
            if (payload == UPDATE_LIKE) {
                holder.updateLikeButton()
            }
        }
    }
}
```

---

## 14. Avoid Data Binding in Scroll

DataBinding updates can cause layout passes. For complex lists, consider regular ViewBinding or manual binding with `executePendingBindings()` when using DataBinding.

---

## 15. Measure and Profile

Use Android Studio tools to find real bottlenecks:

- **Systrace**: identify skipped frames and layout/measure time.
- **Layout Inspector**: inspect view hierarchy depth.
- **Profiler**: detect memory allocations and image decoding overhead.
- **GPU Rendering Profile**: visualise frame timing.

---

## Quick Checklist

1. Use `ViewHolder` and `ViewBinding`.
2. Call `setHasFixedSize(true)` when possible.
3. Keep `onBindViewHolder()` fast and allocation-free.
4. Use DiffUtil / `ListAdapter` instead of `notifyDataSetChanged()`.
5. Load images with Coil/Glide and resize to view bounds.
6. Share `RecycledViewPool` for nested RecyclerViews.
7. Use Paging for large data sets.
8. Minimize view hierarchy depth in item layouts.
9. Avoid `wrap_content` in item root height.
10. Use payload-based updates for partial changes.
11. Profile with Systrace and GPU rendering tools.

---

## Summary

RecyclerView performance comes from efficient view recycling, minimal work in binding, incremental data updates, and careful resource management. Combine these practices with profiling to keep lists smooth and responsive.
