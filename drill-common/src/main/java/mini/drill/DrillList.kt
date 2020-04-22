package mini.drill


@Suppress("UNCHECKED_CAST")
class DrillList<Immutable, Mutable>(
    override var refDrill: List<Immutable>,
    override val parentDrill: DrillType<*>?,
    private val mutate: (container: DrillType<*>, Immutable) -> Mutable,
    private val freeze: (Mutable) -> Immutable
) : MutableCollection<Mutable>, DrillType<List<Immutable>> {

    private inner class Entry(
        override var refDrill: Immutable
    ) : DrillType<Immutable> {
        override val parentDrill = this@DrillList
        var backing: Any? = UNSET_VALUE
        override var dirtyDrill = false
        var value: Mutable
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
    }

    override var dirtyDrill = false

    private val items: ArrayList<Entry> by lazy {
        refDrill.mapTo(ArrayList()) { Entry(it) }
    }

    override fun freeze(): List<Immutable> {
        return if (dirtyDrill) {
            items.map { it.freeze() }
        } else {
            refDrill
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
        return element
    }

    fun removeAt(index: Int): Mutable {
        val out = get(index)
        items.removeAt(index)
        return out
    }

    override fun clear() = items.clear()

    override fun remove(element: Mutable): Boolean {
        return items.removeIf { it.value == element }.apply { markDirty() }
    }

    fun remove(element: Immutable) {
        items.removeIf { it.refDrill == element }.apply { markDirty() }
    }

    override fun removeAll(elements: Collection<Mutable>): Boolean {
        val removed = elements.map { remove(it) }
        return removed.any()
    }

    override fun retainAll(elements: Collection<Mutable>): Boolean {
        return items.retainAll { elements.contains(it.value) }.apply { markDirty() }
    }

    fun retainAll(elements: Collection<Immutable>) {
        items.retainAll { elements.contains(it.refDrill) }.apply { markDirty() }
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
