package mini.drill


@Suppress("UNCHECKED_CAST")
class DrillList<Immutable, Mutable>(
    override var _ref: List<Immutable>,
    override val _parent: DrillType<*>?,
    private val mutate: (container: DrillType<*>, Immutable) -> Mutable,
    private val freeze: (Mutable) -> Immutable
) : MutableCollection<Mutable>, DrillType<List<Immutable>> {

    private inner class Entry(
        override var _ref: Immutable
    ) : DrillType<Immutable> {
        override val _parent = this@DrillList
        var backing: Any? = UNSET_VALUE
        override var _dirty = false
        var value: Mutable
            get() {
                if (backing === UNSET_VALUE) {
                    backing = _ref.run { mutate(this@Entry, this) }
                }
                return backing as Mutable
            }
            set(value) {
                backing = value
                markDirty()
            }

        override fun freeze(): Immutable {
            if (_dirty) return freeze(value)
            return _ref
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as DrillList<*, *>.Entry
            if (_ref != other._ref) return false
            if (backing != other.backing) return false
            return true
        }

        override fun hashCode(): Int {
            var result = _ref?.hashCode() ?: 0
            result = 31 * result + (backing?.hashCode() ?: 0)
            return result
        }
    }

    override var _dirty = false

    private val items: MutableList<Entry> by lazy(LazyThreadSafetyMode.NONE) {
        _ref.map { Entry(it) }.toMutableList()
    }

    override fun freeze(): List<Immutable> {
        return if (_dirty) {
            items.map { it.freeze() }
        } else {
            _ref
        }
    }

    operator fun get(index: Int): Mutable {
        return items[index].value
    }

    override val size: Int get() = items.size

    override fun isEmpty(): Boolean = items.isEmpty()

    override fun iterator(): MutableIterator<Mutable> = object : MutableIterator<Mutable> {
        var currentIndex = 0
        override fun hasNext(): Boolean = currentIndex != items.size
        override fun next(): Mutable {
            return get(currentIndex++)
        }

        override fun remove() {
            removeAt(currentIndex)
        }
    }

    operator fun set(index: Int, element: Immutable): Immutable {
        items[index].set(element)
        markDirty()
        return element
    }

    fun removeAt(index: Int): Mutable {
        val out = get(index)
        items.removeAt(index)
        markDirty()
        return out
    }

    override fun clear() = items.clear().apply { markDirty() }

    override fun remove(element: Mutable): Boolean {
        return items.removeIf { it.value == element }.apply { markDirty() }
    }

    fun remove(element: Immutable) {
        items.removeIf { it._ref == element }.apply { markDirty() }
    }

    override fun removeAll(elements: Collection<Mutable>): Boolean {
        val removed = elements.map { remove(it) }
        return removed.any()
    }

    override fun retainAll(elements: Collection<Mutable>): Boolean {
        return items.retainAll { elements.contains(it.value) }.apply { markDirty() }
    }

    fun retainAll(elements: Collection<Immutable>) {
        items.retainAll { elements.contains(it._ref) }.apply { markDirty() }
    }

    override fun add(element: Mutable): Boolean {
        return items.add(Entry(freeze(element)).apply { markDirty() })
    }

    fun add(element: Immutable) {
        items.add(Entry(element)).apply { markDirty() }
    }

    override fun addAll(elements: Collection<Mutable>): Boolean {
        var added = false
        elements.forEach {
            added = add(it) || added
        }
        return added
    }

    override fun contains(element: Mutable): Boolean {
        return items.find { it.backing == element } != null
    }

    override fun containsAll(elements: Collection<Mutable>): Boolean {
        return elements.all { contains(it) }
    }

    override fun toString(): String {
        return items.toString()
    }
}

fun <Immutable, Mutable> List<Immutable>.toMutable(
    parent: DrillType<*>? = null,
    mutate: (container: DrillType<*>, Immutable) -> Mutable,
    freeze: (Mutable) -> Immutable
): DrillList<Immutable, Mutable> {
    return DrillList(this, parent, mutate, freeze)
}
