# Recycler View Performance

DAY 29-34: RecyclerView Performance

Goal: Understand why RecyclerView is fast and how to keep it fast.

## 1. View Recycling

RecyclerView does not create one View for every item.
It keeps enough ViewHolders for visible items plus cache, then reuses them.

Performance rule:
- onCreateViewHolder should inflate/create views only.
- onBindViewHolder should bind data quickly.
- Do not start expensive work repeatedly without cancellation.

````kotlin
class UserAdapter : ListAdapter<User, UserViewHolder>(UserDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            holder.bind(getItem(position))
        } else {
            holder.bindPartial(getItem(position), payloads)
        }
    }
}
````

## 2. Diffutil

DiffUtil calculates minimal changes between old and new lists.
Without it, notifyDataSetChanged() forces full rebinding and loses animations.

areItemsTheSame:
- Same logical item, usually same stable ID.

areContentsTheSame:
- Same visible content.

````kotlin
object UserDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem

    override fun getChangePayload(oldItem: User, newItem: User): Any? {
        val changes = mutableSetOf<UserPayload>()
        if (oldItem.name != newItem.name) changes += UserPayload.Name
        if (oldItem.avatarUrl != newItem.avatarUrl) changes += UserPayload.Avatar
        return changes.takeIf { it.isNotEmpty() }
    }
}

enum class UserPayload { Name, Avatar }
````

## 3. Payloads

Payloads allow partial rebinds.
Example: update name text without reloading image.
This matters when rows contain images, animations, or expensive subviews.

````kotlin
class UserViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(user: User) {
        bindName(user.name)
        bindAvatar(user.avatarUrl)
    }

    fun bindPartial(user: User, payloads: List<Any>) {
        val changes = payloads.filterIsInstance<Set<UserPayload>>().flatten().toSet()
        if (UserPayload.Name in changes) bindName(user.name)
        if (UserPayload.Avatar in changes) bindAvatar(user.avatarUrl)
    }

    private fun bindName(name: String) {}
    private fun bindAvatar(url: String) {}

    companion object { fun create(parent: ViewGroup) = UserViewHolder(View()) }
}
````

## 4. Image Loading

Use image libraries like Coil/Glide because they handle:
- Memory cache.
- Disk cache.
- Request cancellation on recycled views.
- Bitmap sizing.

Avoid decoding full-size bitmaps into ImageView.

## 5. Nested Recyclerviews

Nested RecyclerViews can be fine but need care:
- Share RecycledViewPool for rows of same type.
- Avoid measuring wrap_content huge lists.
- Use stable IDs where useful.
- Consider flattening data if nested scrolling is poor.

## Interview Questions

Q: Why is notifyDataSetChanged bad?
A: It invalidates the whole list, causing full rebinds and preventing RecyclerView from knowing
what changed. DiffUtil enables minimal updates and animations.

Q: What are payloads?
A: Payloads describe what changed for an item so ViewHolder can do partial binding.

````kotlin
data class User(val id: String, val name: String, val avatarUrl: String)
open class View
open class ViewGroup : View()
open class RecyclerView { open class ViewHolder(val itemView: View) }
abstract class ListAdapter<T, VH>(callback: DiffUtil.ItemCallback<T>)
object DiffUtil { abstract class ItemCallback<T> { abstract fun areItemsTheSame(oldItem: T, newItem: T): Boolean; abstract fun areContentsTheSame(oldItem: T, newItem: T): Boolean; open fun getChangePayload(oldItem: T, newItem: T): Any? = null } }
fun <T> List<Set<T>>.flatten(): List<T> = flatMap { it }
````
