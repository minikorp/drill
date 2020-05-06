package com.minikorp.drill

/**
 * Dummy to test Drill collection types without processor
 */
data class CollectionItem(val text: String) {
    fun toMutable(parent: DrillType<*>? = null): CollectionItemMutable {
        return CollectionItemMutable(this, parent)
    }
}

class CollectionItemMutable(ref: CollectionItem, parent: DrillType<*>?) :
    DefaultDrillType<CollectionItem>(ref, parent) {
    var text: String = ref().text
        set(value) {
            field = value
            markDirty()
        }

    override fun freeze(): CollectionItem {
        return CollectionItem(text = text)
    }
}


val sampleItem = CollectionItem("1")
val sampleItem2 = CollectionItem("2")