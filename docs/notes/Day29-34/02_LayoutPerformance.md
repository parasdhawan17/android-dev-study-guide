# Layout Performance

DAY 29-34: Layout Performance

Goal: Reduce expensive measure/layout/draw work.

## 1. Rendering Pipeline

Each frame roughly has:
1. Input
2. Animation
3. Measure
4. Layout
5. Draw
6. GPU composition

At 60Hz you have about 16.67ms per frame.
At 120Hz you have about 8.33ms per frame.
Slow measure/layout/draw causes jank.

## 2. View Hierarchy Depth

Deep nested layouts increase measure/layout cost.
Old pattern: nested LinearLayouts.
Better: ConstraintLayout or Compose layouts that directly express constraints.

Bad XML shape:
LinearLayout
LinearLayout
LinearLayout
TextView
TextView

Better:
ConstraintLayout
TextView
TextView

## 3. Merge And Viewstub

`<merge>` removes unnecessary parent container when including layouts.
ViewStub lazily inflates rarely-used UI like empty states or error panels.

````kotlin
class ErrorPanelController(private val stub: ViewStub) {
    private var errorView: View? = null

    fun showError(message: String) {
        val view = errorView ?: stub.inflate().also { errorView = it }
        view.visibility = View.VISIBLE
    }
}
````

## 4. Overdraw

Overdraw happens when the same pixel is painted multiple times in one frame.
Causes:
- Stacked backgrounds.
- Full-screen parent backgrounds behind child backgrounds.
- Hidden views still drawing.

Fixes:
- Remove redundant backgrounds.
- Use clipping carefully.
- Avoid drawing behind opaque content.

## 5. Compose Performance Parallels

Compose concerns:
- Unnecessary recomposition.
- Unstable parameters causing recomposition.
- Missing keys in LazyColumn.
- Expensive work inside composables without remember.

Principle is same: reduce repeated work per frame.

````kotlin
@Composable
fun OptimizedList(items: List<Item>) {
    LazyColumn {
        items(items = items, key = { it.id }) { item ->
            ItemRow(item)
        }
    }
}
````

## Interview Questions

Q: How do you detect layout performance issues?
A: Layout Inspector for hierarchy, Profile GPU Rendering for frame time, System Trace for where
time is spent, and overdraw/debug GPU tools for drawing issues.

Q: When use ViewStub?
A: For rarely visible layout sections that are expensive or unnecessary during initial render.

````kotlin
open class View { var visibility = VISIBLE; companion object { const val VISIBLE = 0 } }
class ViewStub : View() { fun inflate(): View = View() }
data class Item(val id: String)
@Composable fun OptimizedListPreview() {}
@Composable fun LazyColumn(content: LazyListScope.() -> Unit) {}
interface LazyListScope { fun <T> items(items: List<T>, key: (T) -> Any, itemContent: @Composable (T) -> Unit) }
@Composable fun ItemRow(item: Item) {}
annotation class Composable
````
