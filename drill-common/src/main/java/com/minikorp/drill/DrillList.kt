package com.minikorp.drill


@Suppress("UNCHECKED_CAST")
class DrillList<Immutable, Mutable>(
    parent: DrillType<*>?,
    ref: List<Immutable>,
    private val mutate: (container: DrillType<*>, Immutable) -> Mutable,
    private val freeze: (Mutable) -> Immutable
) : MutableList<Mutable>, DefaultDrillType<List<Immutable>>(ref, parent) {

    constructor(other: DrillList<Immutable, Mutable>) : this(
        parent = other.parent(),
        ref = other.ref(),
        mutate = other.mutate,
        freeze = other.freeze
    )

    private inner class Entry(
        ref: Immutable
    ) : DefaultDrillType<Immutable>(ref, this) {
        var backing: Any? = UNSET_VALUE
        var value: Mutable
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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as DrillList<*, *>.Entry
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
    private var items: MutableList<Entry>
        get() {
            if (backingItems === UNSET_VALUE) {
                backingItems = ref().mapTo(ArrayList()) { Entry(it) }
            }
            return backingItems as MutableList<Entry>
        }
        set(value) {
            backingItems = value
        }

    override fun freeze(): List<Immutable> {
        return if (dirty()) items.map { it.freeze() } else ref()
    }

    override operator fun get(index: Int): Mutable {
        return items[index].value
    }

    override val size: Int get() = items.size

    override fun isEmpty(): Boolean = items.isEmpty()

    override fun clear() = items.clear().also { markDirty() }

    override fun remove(element: Mutable): Boolean {
        return items.removeIf { it.value == element }.also { markDirty() }
    }

    override fun removeAll(elements: Collection<Mutable>): Boolean {
        val removed = elements.map { remove(it) }
        return removed.any()
    }

    override fun retainAll(elements: Collection<Mutable>): Boolean =
        items.retainAll { elements.contains(it.value) }.also { markDirty() }

    override fun add(element: Mutable): Boolean = items.add(Entry(freeze(element)).also { markDirty() })

    override fun addAll(elements: Collection<Mutable>): Boolean = elements.map { add(it) }.any()

    override fun contains(element: Mutable): Boolean = items.find { it.backing == element } != null

    override fun containsAll(elements: Collection<Mutable>): Boolean = elements.all { contains(it) }

    override fun removeAt(index: Int): Mutable = items.removeAt(index).value.also { markDirty() }

    override fun indexOf(element: Mutable): Int = items.indexOfFirst { it.value == element }

    override fun lastIndexOf(element: Mutable): Int = items.indexOfLast { it.value == element }

    override fun add(index: Int, element: Mutable) = items.add(index, Entry(freeze(element))).also { markDirty() }

    override fun addAll(index: Int, elements: Collection<Mutable>): Boolean =
        items.addAll(index, elements.map { Entry(freeze(it)) }).also { markDirty() }

    override fun set(index: Int, element: Mutable): Mutable {
        items.set(index, Entry(freeze(element))).also { markDirty() }
        return element
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Mutable> {
        val clone = DrillList(this)
        clone.items = this.items.subList(fromIndex, toIndex)
        return clone
    }

    override fun iterator(): MutableIterator<Mutable> = listIterator()
    override fun listIterator(): MutableListIterator<Mutable> = listIterator(0)
    override fun listIterator(index: Int): MutableListIterator<Mutable> {
        val iterator = items.listIterator(index)
        return object : MutableListIterator<Mutable> {
            override fun hasPrevious(): Boolean = iterator.hasPrevious()
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): Mutable = iterator.next().value
            override fun previous(): Mutable = iterator.next().value
            override fun nextIndex(): Int = iterator.nextIndex()
            override fun previousIndex(): Int = iterator.previousIndex()
            override fun add(element: Mutable) = iterator.add(Entry(freeze(element))).also { markDirty() }
            override fun remove() = iterator.remove().also { markDirty() }
            override fun set(element: Mutable) = iterator.set(Entry(freeze(element))).also { markDirty() }
        }
    }

    // Methods from immutable types

    operator fun set(index: Int, element: Immutable) {
        setElement(index, element)
    }

    /** Equivalent of [add]  */
    fun addElement(element: Immutable): Boolean = addElementAt(items.size, element)

    /** Equivalent of [add] */
    fun addElementAt(index: Int, element: Immutable): Boolean {
        items.add(index, Entry(element)).also { markDirty() }
        return true
    }

    /** Equivalent of [set] */
    fun setElement(index: Int, element: Immutable): Immutable {
        items[index].set(element).also { markDirty() }
        return element
    }

    /** Equivalent of [retainAll] */
    fun retainAllElements(elements: Collection<Immutable>) {
        items.retainAll { elements.contains(it.ref()) }.also { markDirty() }
    }

    /** Equivalent of [remove] */
    fun removeElement(element: Immutable) {
        items.removeIf { it.ref() == element }.also { markDirty() }
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
    return DrillList(
        parent = parent,
        ref = this,
        mutate = mutate,
        freeze = freeze
    )
}