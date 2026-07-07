# Compose vs XML Performance

MISC: A deeper look at how Jetpack Compose and the legacy XML View system differ in performance, and when to prefer one over the other.

````kotlin
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
````

## 1. Core Rendering Model

### XML View System (Imperative)

XML layouts are parsed at runtime into a tree of View objects. Each view is a real Java object, inflated via `LayoutInflater` from XML files.

**Typical lifecycle of a screen update:**

1.  **Inflation** (XML → View objects) on first layout.
2.  **Measure pass** (top-down): every View asks its children how big they want to be.
3.  **Layout pass** (top-down): every View positions its children.
4.  **Draw pass** (top-down): every View renders itself to the canvas.
5.  **Mutation** (findViewById / setText / setVisibility) on state change, then re-run 2–4 for affected Views.

Costs to watch:
- Inflation can be expensive for deep hierarchies.
- Repeated `findViewById` calls can be slow (mitigated by ViewBinding).
- Deep view trees multiply the cost of every measure/layout/draw pass.

### Jetpack Compose (Declarative)

Compose uses a declarative UI model. It runs Kotlin functions to build a tree of UI nodes, then diffing it against the previous tree to determine the minimum changes needed.

**Typical lifecycle of a screen update:**

1.  **Composition** (Kotlin functions run to describe UI).
2.  **Layout** (single pass): measure and position nodes.
3.  **Draw** (render to the canvas).
4.  **Recomposition** on state change: Compose re-runs only the relevant `@Composable` functions and emits only the changed nodes.

Key ideas:
- No XML parsing at runtime.
- No separate measure/layout pass unless a node actually needs it.
- Recomposition is skipped when inputs are stable or unchanged.

## 2. Inflation Cost

| System | Inflation | What happens at runtime |
|--------|-----------|-------------------------|
| XML Views | XML resource is parsed and turned into a View instance tree | Reflection / pulling attributes / style resolution |
| Compose | No runtime XML parsing | Compiler generates code from `@Composable` functions and calls them |

Example of a deep XML hierarchy that can be slow to inflate:

```xml
<LinearLayout>
    <LinearLayout>
        <RelativeLayout>
            <TextView />
            <ImageView />
        </RelativeLayout>
    </LinearLayout>
</LinearLayout>
```

Compose equivalent can be a single Column with fewer layout nodes, because Compose layouts are not real View objects.

## 3. Recomposition vs Manual Update

### XML View System: Manual Mutations

```kotlin
val textView: TextView = findViewById(R.id.titleText)
textView.text = newTitle
```

Pros:
- You touch exactly what changed, so overhead is very low for a single small change.
- No framework diffing step.

Cons:
- Easy to forget to update related UI (e.g., visibility, error state).
- Updates to lists require manual `notifyDataSetChanged` or DiffUtil.
- View holders must be recycled carefully to keep scroll smooth.

### Compose: Recomposition

```kotlin
@Composable
fun Title(title: String) {
    Text(title)
}
```

When `title` changes, Compose re-runs `Title` and schedules a draw for the new text. Recomposition is skipped if the title is unchanged.

Pros:
- UI is always consistent with the state.
- Lists use `LazyColumn` with smart recompositions based on item keys.
- Less hand-written boilerplate for updating dependent UI.

Cons:
- Recomposition can be expensive if not controlled (e.g., recomposing the whole screen on a tiny state change).
- Need to use `remember`, `derivedStateOf`, and stable keys to keep it fast.

## 4. List Performance

### RecyclerView with XML

XML-based RecyclerView requires a ViewHolder for every item type and explicit view recycling.

````kotlin
class UserAdapter : ListAdapter<User, UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.nameText)

        fun bind(user: User) {
            nameText.text = user.name
        }
    }
}

class UserDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(old: User, new: User) = old.id == new.id
    override fun areContentsTheSame(old: User, new: User) = old == new
}
````

### LazyColumn with Compose

Compose uses a virtualized list similar to RecyclerView but recomposes items rather than binding ViewHolders.

````kotlin
@Composable
fun UserList(users: List<User>) {
    LazyColumn {
        items(
            items = users,
            key = { it.id }
        ) { user ->
            UserRow(user)
        }
    }
}

@Composable
fun UserRow(user: User) {
    Text(user.name)
}
````

## 4.1 Side-by-Side Performance Comparison

| Aspect | RecyclerView (XML) | LazyColumn (Compose) |
|--------|--------------------|----------------------|
| Inflation | `LayoutInflater` parses XML and creates View objects | No XML parsing; composable functions create layout nodes |
| Recycling | Physical `ViewHolder` pool (`RecyclerView.RecycledViewPool`) | Composition slot pool; items are re-composed, not re-bound |
| Binding | `onBindViewHolder` mutates existing Views | Recomposition of item content lambda |
| Diffing | `DiffUtil` or `ListAdapter` calculates item moves/updates | `key` parameter handles identity and move detection |
| Measure/Layout | Two-pass measure + layout on View tree | Single-pass layout on Compose node tree |
| Scroll/Fling | Mature and usually smooth on low-end devices | Smooth with stable keys and lightweight items; can lag on very heavy item composables |
| Off-screen work | ViewHolders bound only when attached | Items are composed ahead of viewport for predictive gestures |
| Threading | UI thread only for measure/layout/draw | Composition can run off the main thread (runtime configurable) |
| Best for | Heavy, stable item layouts; third-party View libraries | Dynamic, state-driven lists; animations; conditional item UI |

## 4.2 Allocation and Recycling Cost

RecyclerView:
- Allocates one `View` instance per item type the first time it appears.
- Re-uses `ViewHolder` instances during scroll; only `onBindViewHolder` re-runs.
- Allocation spikes happen when a new item type enters the viewport.

LazyColumn:
- Does not allocate View objects; it maintains slots in the composition.
- Composition can re-run when data changes, but recomposition is skipped for unchanged items.
- Without a stable `key`, Compose can recompose more items than necessary when the list changes.
- Heavy allocations inside a composable (e.g., creating new objects every composition) can hurt scroll performance.

## 4.3 Item Updates and Diffing

RecyclerView updates the whole item row when `areContentsTheSame` returns `false`:

````kotlin
class UserDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(old: User, new: User) = old.id == new.id
    override fun areContentsTheSame(old: User, new: User) = old == new
}
````

Compose updates at the composable level. If only one field inside an item changes, you can structure the item so that only the changed field recomposes:

````kotlin
@Composable
fun UserRow(user: User) {
    Row {
        // Avatar will only recompose if avatarUrl changes
        UserAvatar(user.avatarUrl)
        // Name will only recompose if name changes
        Text(user.name)
    }
}
````

## 4.4 Scroll and Fling Behavior

RecyclerView:
- `RecyclerView` has years of optimizations and generally behaves well even with complex ViewHolders.
- `setHasFixedSize(true)` removes layout request overhead when adapter changes do not affect size.
- `RecycledViewPool` can be shared across multiple lists.

LazyColumn:
- `LazyColumn` uses a predictive prefetching model similar to RecyclerView.
- For very fast flings, item composition time becomes the bottleneck.
- Keep item composables small and stable to maintain 60/120 fps.
- Use `LazyListState` to observe scroll state and throttle expensive work.

## 4.5 Common Mistakes That Hurt Performance

RecyclerView:
- Calling `notifyDataSetChanged()` instead of `DiffUtil`.
- Creating views inside `onBindViewHolder` instead of `onCreateViewHolder`.
- Heavy work (image decoding, JSON parsing) inside `onBindViewHolder`.
- Deep item XML layouts increasing measure cost.

LazyColumn:
- Forgetting `key` so every item recomposes on any list change.
- Passing unstable classes as item parameters.
- Doing expensive work (sorting, filtering) inside the item composable.
- Using `Column` + `forEach` instead of `LazyColumn` for large lists.
- Allocating a new `Modifier` object on every recomposition.

## 4.6 When to Prefer Which

Prefer RecyclerView when:
- Items are built from third-party View libraries (e.g., custom chart, video player, ad view).
- Item layout is heavy and stable, and re-binding Views is cheaper than recomposing.
- You need `RecycledViewPool` sharing or `ItemTouchHelper` features that Compose does not yet match.
- You are adding a list to a mostly XML screen and want minimal interop overhead.

Prefer LazyColumn when:
- Item content is highly dynamic or conditional.
- You want built-in animations for item insert/delete/move.
- Items depend on app state that changes frequently.
- You want to keep the UI tree flat and avoid nested ViewGroups.
- You are building a fully Compose screen and can leverage stable keys and `remember`.

## 5. Stable vs Unstable Inputs

Compose recomposition is skipped when input parameters are stable or unchanged. Stability matters a lot for performance.

Stable classes (built-in or annotated):
- `String`, `Int`, `Boolean`, `List`, `Set`, `Map` (immutable collections).
- Data classes with only stable types.
- Classes annotated with `@Stable` or `@Immutable`.

Unstable classes that trigger recomposition every time:
- Mutable objects with observable state (e.g., `ArrayList`, `MutableState` inside a class).
- Classes from libraries Compose cannot mark as stable.
- Custom classes with `var` properties.

Example of a stability problem:

```kotlin
class UiState {
    var title: String = ""
    var items: List<Item> = emptyList()
}
```

Better:

```kotlin
@Immutable
data class UiState(
    val title: String = "",
    val items: List<Item> = emptyList()
)
```

## 6. Expensive Calculations in Compose

Use `remember` and `derivedStateOf` to avoid recomputing work on every recomposition.

```kotlin
@Composable
fun SortedList(items: List<Item>) {
    // Only re-sorts when items changes, not on every recomposition
    val sortedItems by remember(items) {
        mutableStateOf(items.sortedBy { it.priority })
    }

    LazyColumn {
        items(sortedItems, key = { it.id }) { item ->
            ItemRow(item)
        }
    }
}
```

Use `derivedStateOf` when reading a state that is derived from other state:

```kotlin
@Composable
fun FilteredList(items: List<Item>, query: String) {
    val filtered by remember {
        derivedStateOf {
            items.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    LazyColumn {
        items(filtered, key = { it.id }) { item ->
            ItemRow(item)
        }
    }
}
```

## 7. Avoiding Recomposition with `key` and Stable Parameters

```kotlin
@Composable
fun Dashboard(cards: List<Card>) {
    Column {
        cards.forEach { card ->
            // key gives Compose a stable identity for each card
            key(card.id) {
                CardView(card)
            }
        }
    }
}
```

Without `key`, the whole Column can recompose if the list reference changes, even if the content is identical.

## 8. XML Performance Tips

1. **Flatten hierarchies**: replace nested `LinearLayout` chains with `ConstraintLayout`.
2. **Use `ViewBinding` or `findViewById` once** in `onCreateViewHolder` / ViewHolder.
3. **Avoid `wrap_content` chains**: they cause multiple measure passes.
4. **Use `RecyclerView` with `DiffUtil`** instead of `notifyDataSetChanged`.
5. **Re-use views**: custom views that allocate objects every draw can cause jank.
6. **Profile with Layout Inspector**, Systrace, and GPU rendering bars.

## 9. Compose Performance Tips

1. **Use `LazyColumn` / `LazyRow`** for large lists; do not use `Column` + `forEach` for hundreds of items.
2. **Use stable keys** (`key = { ... }`) in lists.
3. **Make data classes stable** with `@Immutable` / `@Stable` if they are passed as parameters.
4. **Cache expensive work** with `remember` and `derivedStateOf`.
5. **Avoid recomposing the whole screen**; split into small, focused `@Composable` functions.
6. **Pass only the data a composable needs** (state hoisting and granular parameters).
7. **Use `Profileable` and `Composition Tracing`** to see recomposition counts in Android Studio.
8. **Avoid `Modifier` allocation in every recomposition** if possible; re-use modifiers where reasonable.

## 10. Compose Compiler Optimizations

The Compose compiler does several things at build time to keep runtime fast:

- **Composer code generation**: wraps state reads so Compose can track them.
- **Restartable composables**: detects when a composable needs recomposition.
- **Skippable composables**: skips recomposition if all parameters are stable and unchanged.
- **Movable / replaceable groups**: inserts markers in the UI tree so diffing is faster.

You can inspect recomposition behavior in Android Studio by enabling the **Compose Compiler Metrics** option.

## 11. When XML Is Still Faster

XML Views are still a solid choice in some cases:

- Very simple static screens with no dynamic updates: minimal overhead, no recomposition engine.
- Heavy custom drawing or games: `SurfaceView` / `TextureView` / custom `View.onDraw` are already imperative and fast.
- Video or camera previews: the legacy View system has well-tested `SurfaceView` integration.
- Extremely low-end devices where Compose runtime and library size are concerns.
- Apps already using large third-party View libraries that are not available in Compose.

## 12. When Compose Is Faster or Easier to Optimize

Compose usually wins when:

- UI changes frequently based on state (lists, forms, animations).
- The same screen has many conditional branches.
- You want a flat layout tree without deeply nested ViewGroups.
- You need reusable UI components (composables) with lower boilerplate than custom Views.
- You want to measure recomposition counts and optimize declaratively.

## 13. Memory and Binary Size

| Aspect | XML Views | Compose |
|--------|-----------|---------|
| Runtime objects | One `View` object per element | Composable function objects + layout nodes |
| APK size | Smaller base, many XML files | Compose compiler and runtime add initial weight |
| Method count | Depends on how many widgets you use | Compose adds some methods, but you may use fewer custom Views |
| Runtime memory | Deep view trees can consume more | Lazy composition can allocate less for off-screen items |

In practice, Compose can be smaller or larger depending on the app size and how many widgets are used. For large apps, the Compose runtime cost is amortized.

## 14. Practical Decision Guide

| Scenario | Recommendation |
|----------|----------------|
| New project / new screen | Compose |
| Small static screen with no updates | Either; XML can be slightly lighter |
| Complex list with frequent updates | Compose + `LazyColumn` + stable keys |
| Heavy custom drawing / game | Custom View or `SurfaceView` |
| Existing app with XML | Migrate gradually using `ComposeView` |
| Performance-critical animations | Compose animations are good, but benchmark against custom Views |
| Third-party View-only libraries | Wrap in `AndroidView` or keep the screen in XML |

## 15. Interview Questions

Q: What is the main performance difference between Compose and XML Views?  
A: XML requires runtime inflation and measure/layout/draw passes over a tree of View objects. Compose compiles UI to a composition tree and uses recomposition to update only the changed parts, avoiding XML parsing and often flattening the layout tree.

Q: How does Compose avoid unnecessary work?  
A: Compose skips recompositions when parameters are stable and unchanged, caches expensive calculations with `remember` and `derivedStateOf`, and uses stable `key` values to move list items efficiently.

Q: When is XML View system better than Compose?  
A: For very simple static screens, heavy custom drawing, video/camera surfaces, apps with many third-party View libraries, or low-end devices where the Compose runtime overhead matters.

Q: How do you optimize list performance in Compose?  
A: Use `LazyColumn`/`LazyRow`, provide stable `key`s for items, keep item composables lightweight and stable, and avoid recomposing the entire list by passing only the data each item needs.

Q: What does stability mean in Compose?  
A: A stable type is one whose value Compose can trust not to change without notification. Stable types can skip recomposition when unchanged. Mutable classes with `var` fields are usually unstable and force recomposition.

Q: What tools can you use to profile Compose?  
A: Android Studio Layout Inspector with recomposition counts, Compose Compiler metrics, Systrace, Macrobenchmark, and JankStats for measuring frame drops.

Q: What is recomposition?  
A: Recomposition is the process of re-running affected `@Composable` functions when state changes so the UI tree can be updated to match the new state.

Q: What is the cost of a deep XML view hierarchy?  
A: Each level of depth can cause additional measure and layout passes, and inflation time grows as more XML is parsed. Deep hierarchies can lead to jank and slow startup.

Q: Can Compose be slower than XML?  
A: Yes, if the screen is poorly composed: large recompositions, unstable parameters, no stable keys, or expensive work done inside composables without caching.

Q: What are the Compose compiler metrics useful for?  
A: They report which composables are restartable, skippable, or unstable, helping you find performance hotspots and fix them by adding stability annotations or restructuring code.

Stub classes

````kotlin
data class User(val id: String, val name: String)
data class Item(val id: String, val name: String, val priority: Int)
data class Card(val id: String, val title: String)

@Composable
fun ItemRow(item: Item) {
    Text(item.name)
}

@Composable
fun CardView(card: Card) {
    Text(card.title)
}

@Composable
fun UserAvatar(avatarUrl: String) {}

@Composable
fun Row(content: @Composable () -> Unit) {}

annotation class Immutable
annotation class Stable
````
