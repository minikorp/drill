package com.minikorp.drill

import java.util.function.Predicate

@Suppress("UNCHECKED_CAST")
class DrillMap<K, Immutable, Mutable>(
    parent: DrillType<*>?,
    ref: Map<K, Immutable>,
    private val mutate: (container: DrillType<*>, Immutable) -> Mutable,
    private val freeze: (Mutable) -> Immutable
) : MutableMap<K, Mutable>, DefaultDrillType<Map<K, Immutable>>(ref, parent) {

    private inner class Entry(
        override val key: K, ref: Immutable
    ) : DefaultDrillType<Immutable>(ref, this), MutableMap.MutableEntry<K, Mutable> {
        var backing: Any? = UNSET_VALUE

        override var value: Mutable
            get() {
                if (backing === UNSET_VALUE) {
                    backing = ref().run { mutate(this@Entry, this) }
                }
                return backing as Mutable
            }
            set(value) {
                backing = value
                markDirty()
            }

        override fun freeze(): Immutable {
            if (dirty()) return freeze(value)
            return ref()
        }

        override fun setValue(newValue: Mutable): Mutable {
            value = newValue.also { markDirty() }
            return newValue
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as DrillMap<*, *, *>.Entry
            if (ref() != other.ref()) return false
            if (backing != other.backing) return false
            return true
        }

        override fun hashCode(): Int {
            var result = ref()?.hashCode() ?: 0
            result = 31 * result + (backing?.hashCode() ?: 0)
            return result
        }
    }

    private var backingItems: Any = UNSET_VALUE
    private var items: MutableMap<K, Entry>
        get() {
            if (backingItems === UNSET_VALUE) {
                backingItems = ref().mapValuesTo(LinkedHashMap()) { Entry(it.key, it.value) }
            }
            return backingItems as MutableMap<K, Entry>
        }
        set(value) {
            backingItems = value
        }

    override fun freeze(): Map<K, Immutable> {
        return if (dirty()) {
            items.mapValuesTo(LinkedHashMap()) { it.value.freeze() }
        } else {
            return ref()
        }
    }

    override fun containsKey(key: K): Boolean = items.containsKey(key)
    override fun containsValue(value: Mutable): Boolean = (items.values as List<Mutable>).contains(value)
    override fun get(key: K): Mutable? = items[key]?.value
    override fun isEmpty(): Boolean = items.isEmpty()
    override val size: Int get() = items.size
    override val keys: MutableSet<K> get() = items.keys
    override val values: MutableCollection<Mutable> get() = items.values.mapTo(ArrayList(items.size)) { it.value }
    override fun clear() = items.clear().also { markDirty() }

    override fun put(key: K, value: Mutable): Mutable? {
        items[key] = Entry(key, freeze(value))
        markDirty()
        return value
    }

    operator fun set(key: K, value: Immutable) {
        put(key, value)
    }

    fun put(key: K, value: Immutable) {
        items[key] = Entry(key, value).also { markDirty() }
    }

    fun put(from: Map<out K, Immutable>) {
        from.mapValuesTo(items) { Entry(it.key, it.value) }.also { markDirty() }
    }

    override fun putAll(from: Map<out K, Mutable>) {
        from.mapValuesTo(items) { Entry(it.key, freeze(it.value)) }.also { markDirty() }
    }

    override fun remove(key: K): Mutable? {
        val e = items.remove(key).also { markDirty() }
        return e?.value
    }

    override fun toString(): String {
        return entries.toString()
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, Mutable>>
        get() {
            return DelegateEntries(items.entries as MutableSet<MutableMap.MutableEntry<K, Mutable>>)
        }

    private inner class DelegateEntries(
        private val actualSet: MutableSet<MutableMap.MutableEntry<K, Mutable>>
    ) : MutableSet<MutableMap.MutableEntry<K, Mutable>> by actualSet {
        override fun add(element: MutableMap.MutableEntry<K, Mutable>): Boolean =
            actualSet.add(element).also { if (it) markDirty() }

        override fun addAll(elements: Collection<MutableMap.MutableEntry<K, Mutable>>): Boolean =
            actualSet.addAll(elements).also { if (it) markDirty() }

        override fun remove(element: MutableMap.MutableEntry<K, Mutable>): Boolean =
            actualSet.remove(element).also { if (it) markDirty() }

        override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, Mutable>>): Boolean =
            actualSet.removeAll(elements).also { if (it) markDirty() }

        override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, Mutable>>): Boolean =
            actualSet.retainAll(elements).also { if (it) markDirty() }

        override fun removeIf(filter: Predicate<in MutableMap.MutableEntry<K, Mutable>>): Boolean =
            actualSet.removeIf(filter).also { if (it) markDirty() }

        override fun clear() {
            if (actualSet.isNotEmpty()) {
                actualSet.clear().also { markDirty() }
            }
        }

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, Mutable>> {
            return DelegateIterator(actualSet.iterator())
        }
    }

    private inner class DelegateIterator(
        val actualIterator: MutableIterator<MutableMap.MutableEntry<K, Mutable>>
    ) : MutableIterator<MutableMap.MutableEntry<K, Mutable>> by actualIterator {
        override fun remove() {
            actualIterator.remove().also { markDirty() }
        }
    }
}


fun <K, Mutable, Immutable> Map<K, Immutable>.toMutable(
    parent: DrillType<*>? = null,
    mutate: (container: DrillType<*>, Immutable) -> Mutable,
    freeze: (Mutable) -> Immutable
): DrillMap<K, Immutable, Mutable> {
    return DrillMap(
        parent = parent,
        ref = this,
        mutate = mutate,
        freeze = freeze
    )
}
