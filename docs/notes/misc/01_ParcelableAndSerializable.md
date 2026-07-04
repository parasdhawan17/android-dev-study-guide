# Parcelable vs Serializable

MISC: Two ways to serialize objects in Android for IPC, state saving, and data transfer.

`Parcelable` and `Serializable` both let you convert objects into a form that can be passed between Activities, Fragments, processes, or saved in a Bundle. They differ in speed, implementation effort, and use case.

````kotlin
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable
import kotlinx.parcelize.Parcelize
````

## 1. Serializable

`Serializable` is a Java standard interface. It relies on reflection to automatically serialize and deserialize the object graph.

````kotlin
data class UserSerializable(
    val id: String,
    val name: String,
    val age: Int
) : Serializable
````

**How it works:**
- Mark the class with `Serializable`
- Java runtime walks the object graph using reflection
- Writes data to an output stream and reads it back

**Pros:**
- Minimal implementation effort
- Works across Java and Kotlin code
- No Android-specific dependency

**Cons:**
- Slow due to reflection
- Creates many temporary objects
- Produces larger serialized output
- Not recommended for Android IPC or performance-critical paths

## 2. Parcelable

`Parcelable` is an Android-specific interface optimized for performance. You explicitly write and read each field to/from a `Parcel` in a fixed order.

Manual implementation:

````kotlin
data class UserParcelable(
    val id: String,
    val name: String,
    val age: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        name = parcel.readString() ?: "",
        age = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeInt(age)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<UserParcelable> {
        override fun createFromParcel(parcel: Parcel): UserParcelable {
            return UserParcelable(parcel)
        }

        override fun newArray(size: Int): Array<UserParcelable?> {
            return arrayOfNulls(size)
        }
    }
}
````

With the Kotlin Parcelize plugin:

````kotlin
@Parcelize
data class UserParcelable(
    val id: String,
    val name: String,
    val age: Int
) : Parcelable
````

**Pros:**
- Much faster than `Serializable`
- Smaller serialized size
- No reflection overhead
- Recommended for Android

**Cons:**
- More boilerplate without Parcelize
- Android-specific

## 3. Side By Side Comparison

| Feature | Serializable | Parcelable |
|---|---|---|
| Mechanism | Reflection | Explicit write/read |
| Speed | Slow | Fast |
| Serialized size | Larger | Smaller |
| Boilerplate | Minimal | Manual: high / Parcelize: minimal |
| Cross-platform | Yes | Android-only |
| Recommended for Android | No | Yes |

## 4. When to Use What

Use `Parcelable` when:
- Passing data between Activities or Fragments
- Saving state in a Bundle
- Inter-process communication (AIDL, Messenger, Binder)
- Sending objects in Intents

Use `Serializable` when:
- Prototyping or working with shared Java libraries
- Backward compatibility with existing Java APIs is required
- Performance is not a concern

## 5. Bundle Size and Security

Both approaches store data in a `Bundle`, which has a transaction buffer limit of roughly 1MB.

- Save only minimal state
- Avoid large lists or bitmaps
- Prefer IDs over full objects
- Use `@Parcelize` to remove manual boilerplate

## 6. Parcelable With Collections

Parcelable objects can contain other Parcelable objects, including collections.

````kotlin
@Parcelize
data class Order(
    val orderId: String,
    val items: List<Item>
) : Parcelable

@Parcelize
data class Item(
    val id: String,
    val name: String
) : Parcelable
````

## Interview Questions

Q: What is the difference between `Parcelable` and `Serializable`?
A: `Serializable` uses Java reflection and is slow. `Parcelable` is Android-specific, writes fields explicitly to a `Parcel`, and is much faster and smaller.

Q: Why is `Parcelable` faster than `Serializable`?
A: `Parcelable` avoids reflection by manually writing and reading fields from the `Parcel` in a known order.

Q: How do you implement `Parcelable` in Kotlin?
A: Use the Kotlin Parcelize plugin with the `@Parcelize` annotation, or implement `Parcelable` manually with `writeToParcel`, a constructor from `Parcel`, and a `CREATOR`.

Q: When would you use `Serializable` over `Parcelable`?
A: `Serializable` is simpler for cross-platform Java code or quick prototyping, but it should generally be avoided in Android for performance reasons.

Q: Can you pass a `Parcelable` object in an `Intent`?
A: Yes. Use `intent.putExtra("key", parcelableObject)` and retrieve it with `intent.getParcelableExtra("key")`.

Q: What is the transaction limit for a `Bundle`?
A: Approximately 1MB. Large objects should be persisted to disk instead of being saved in a Bundle.

Stub annotations

````kotlin
annotation class Parcelize
````
