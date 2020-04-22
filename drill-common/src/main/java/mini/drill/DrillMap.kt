package mini.drill

import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Suppress("UNCHECKED_CAST")
class DrillMap<K, Immutable, Mutable>(
    override var refDrill: Map<K, Immutable>,
    override val parentDrill: DrillType<*>?,
    private val mutate: (container: DrillType<*>, Immutable) -> Mutable,
    private val freeze: (Mutable) -> Immutable
) : MutableMap<K, Mutable>, DrillType<Map<K, Immutable>> {

    private inner class Entry(
        override val key: K,
        override var refDrill: Immutable
    ) : DrillType<Immutable>, MutableMap.MutableEntry<K, Mutable> {
        override val parentDrill = this@DrillMap
        var backing: Any? = UNSET_VALUE
        override var dirtyDrill = false

        override var value: Mutable
            get() {
                if (backing === UNSET_VALUE) {
                    backing = refDrill.run { mutate(this@Entry, this) }
                }
                return backing as Mutable
            }
            set(value) {
                backing = value
                markDirty()
            }

        override fun freeze(): Immutable {
            if (dirtyDrill) return freeze(value)
            return refDrill
        }

        override fun setValue(newValue: Mutable): Mutable {
            value = newValue
            dirtyDrill = true
            return newValue
        }

        override fun equals(other: Any?): Boolean {
            return Objects.equals(backing, other)
        }

        override fun hashCode(): Int {
            return backing.hashCode()
        }
    }

    override var dirtyDrill: Boolean = false

    private val items: HashMap<K, Entry> by lazy {
        refDrill.mapValuesTo(HashMap()) { Entry(it.key, it.value) }
    }

    override fun freeze(): Map<K, Immutable> {
        return if (dirtyDrill) {
            items.mapValuesTo(HashMap()) { it.value.freeze() }
        } else {
            return refDrill
        }
    }

    override fun containsKey(key: K): Boolean = items.containsKey(key)
    override fun containsValue(value: Mutable): Boolean = (items.values as List<Mutable>).contains(value)
    override fun get(key: K): Mutable? = items[key]?.value
    override fun isEmpty(): Boolean = items.isEmpty()
    override val size: Int get() = items.size
    override val entries: MutableSet<MutableMap.MutableEntry<K, Mutable>> get() = items.values.toMutableSet()
    override val keys: MutableSet<K> get() = items.keys
    override val values: MutableCollection<Mutable> get() = items.values.mapTo(ArrayList(items.size)) { it.value }
    override fun clear() = items.clear()

    override fun put(key: K, value: Mutable): Mutable? {
        items[key] = Entry(key, freeze(value))
        return value
    }

    operator fun set(key: K, value: Immutable): Unit {
        put(key, value)
    }

    fun put(key: K, value: Immutable) {
        items[key] = Entry(key, value)
        markDirty()
    }

    fun put(from: Map<out K, Immutable>) {
        from.mapValuesTo(items) { Entry(it.key, it.value) }
        markDirty()
    }

    override fun putAll(from: Map<out K, Mutable>) {
        from.mapValuesTo(items) { Entry(it.key, freeze(it.value)) }
        markDirty()
    }

    override fun remove(key: K): Mutable? {
        val e = items.remove(key)
        markDirty()
        return e?.value
    }
}

fun <K, Mutable, Immutable> Map<K, Immutable>.toMutable(
    parent: DrillType<*>? = null,
    mutate: (container: DrillType<*>, Immutable) -> Mutable,
    freeze: (Mutable) -> Immutable
): DrillMap<K, Immutable, Mutable> {
    return DrillMap(this, parent, mutate, freeze)
}