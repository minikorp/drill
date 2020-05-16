package com.minikorp.drill

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.*

internal class DrillMapTest {

    private inline fun Map<String, CollectionItem>.mutate(
        crossinline block: DrillMap<Map<String, CollectionItem>, String, CollectionItem, CollectionItemMutable>.() -> Unit
    ): Map<String, CollectionItem> {
        val mutable = this.toMutable(
            parent = null,
            factory = { it },
            mutate = { container, it -> it.toMutable(container) },
            freeze = { it.freeze() }
        )
        mutable.block()
        return mutable.freeze()
    }

    private val emptyMap = emptyMap<String, CollectionItem>()
    private val mapWithItems = mapOf(
        "1" to sampleItem,
        "2" to sampleItem2
    )

    @Test
    fun `elements are added`() {
        val mutated = emptyMap.mutate {
            this["new"] = sampleItem
        }
        expectThat(mutated).containsKey("new")
        expectThat(mutated["new"]).isSameInstanceAs(sampleItem)
    }

    @Test
    fun `maps are added`() {
        val mutated = emptyMap.mutate {
            this.putAllElements(mapWithItems)
        }
        expectThat(mutated).isEqualTo(mapWithItems)
    }

    @Test
    fun `keys are removed`() {
        val mutated = mapWithItems.mutate {
            this.remove("1")
        }
        expectThat(mutated["1"]).isNull()
        expectThat(mutated.size).isEqualTo(mapWithItems.size - 1)
    }


    @Test
    fun `keys are cleared`() {
        val mutated = mapWithItems.mutate {
            keys.clear()
        }
        expectThat(mutated).isEmpty()
    }

    @Test
    fun `elements are removed`() {
        val mutated = mapWithItems.mutate {
            removeElement(sampleItem)
            removeElement(sampleItem2)
        }
        expectThat(mutated).isEmpty()
    }

    @Test
    fun `values are modified`() {
        val mutated = mapWithItems.mutate {
            values.forEach {
                it.text = "changed"
            }
        }
        expectThat(mutated.values.map { it.text }).all { isEqualTo("changed") }
    }

}