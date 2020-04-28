package mini.drill

import org.junit.jupiter.api.Test
import sample.ListItem
import sample.ListItem_Mutable
import sample.toMutable
import strikt.api.expectThat
import strikt.assertions.*

internal class DrillListTest {

    private inline fun <T : List<ListItem>> T.mutate(
        crossinline block: DrillList<ListItem, ListItem_Mutable>.() -> Unit
    ): List<ListItem> {
        val mutable = this.toMutable(
            parent = null,
            mutate = { container, it -> it.toMutable(container) },
            freeze = { it.freeze() }
        )
        mutable.block()
        return mutable.freeze()
    }

    private val sampleItem = ListItem("1")
    private val sampleItem2 = ListItem("2")

    private val emptyList = listOf<ListItem>()
    private val listWithItems = listOf(sampleItem, sampleItem2)

    @Test
    fun freeze() {
        sampleItem.toMutable()
    }

    @Test
    fun `getting by index returns the element`() {
        expectThat(listWithItems[0].text).isEqualTo(sampleItem.text)
    }

    @Test
    fun `clearing removes all elements`() {
        val mutated = listWithItems.mutate {
            clear()
        }
        expectThat(mutated).isEmpty()
    }

    @Test
    fun `elements are removed`() {
        val mutated = listWithItems.mutate {
            remove(get(0))
        }
        expectThat(mutated).hasSize(listWithItems.size - 1)
    }

    @Test
    fun `elements are retained`() {
        val mutated = listWithItems.mutate {
            retainAll { it.text == "impossible" }
        }
        expectThat(mutated).isEmpty()
    }

    @Test
    fun `elements are added`() {
        val mutated = listWithItems.mutate {
            add(get(0))
        }
        expectThat(mutated).hasSize(listWithItems.size + 1)
        expectThat(mutated.last()).isEqualTo(listWithItems.first())
    }

    @Test
    fun `contains find items`() {
        listWithItems.mutate {
            expectThat(this).contains(this[0])
        }
    }

    @Test
    fun `removing at index removes the value`() {
        val mutated = listWithItems.mutate {
            removeAt(0)
        }
        expectThat(mutated).hasSize(listWithItems.size - 1)
        expectThat(mutated.first()).isEqualTo(listWithItems[1])
    }

    @Test
    fun `setting by index updates value`() {
        val mutated = listWithItems.mutate {
            this[0] = this[1]
        }
        expectThat(mutated[0]).isEqualTo(mutated[1])
    }

    @Test
    fun `sublist mutations propagate`() {
        val mutated = listWithItems.mutate {
            val sublist = subList(0, 1)
            sublist[0].text = "mutated"
        }
        expectThat(mutated[0].text).isEqualTo("mutated")
    }

    @Test
    fun `iterator mutations propagate`() {
        val mutated = listWithItems.mutate {
            val iterator = iterator()
            iterator.next().text = "mutated"
            iterator.next()
            iterator.remove()
        }
        expectThat(mutated[0].text).isEqualTo("mutated")
        expectThat(mutated).hasSize(listWithItems.size - 1)
    }

    @Test
    fun `immutable elements are added`() {
        val mutated = emptyList.mutate {
            addElement(sampleItem)
        }
        expectThat(mutated[0]).isSameInstanceAs(sampleItem)
    }

    @Test
    fun `immutable elements are added at index`() {
        val mutated = emptyList.mutate {
            addElement(sampleItem)
            addElement(sampleItem)
            addElement(sampleItem)
            addElementAt(0, sampleItem2)
        }
        expectThat(mutated[0]).isSameInstanceAs(sampleItem2)
    }

    @Test
    fun `immutable elements are replaced`() {
        val mutated = listWithItems.mutate {
            this[0] = sampleItem2
        }
        expectThat(mutated[0]).isSameInstanceAs(sampleItem2)
    }

    @Test
    fun `immutable elements are retained`() {
        val mutated = listWithItems.mutate {
            this.retainAllElements(listOf(sampleItem))
        }
        expectThat(mutated).contains(sampleItem)
        expectThat(mutated).doesNotContain(sampleItem2)
    }

    @Test
    fun `immutable elements are removed`() {
        val mutated = listWithItems.mutate {
            this.removeElement(sampleItem)
        }
        expectThat(mutated).doesNotContain(sampleItem)
    }
}