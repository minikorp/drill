package sample

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.*

internal class ListClassTest {

    @Test
    fun `empty mutation keeps reference`() {
        val source = ListClass(listOf(ListItem("not mutated")))
        val mutated = source.mutate {
            this.list.forEach { it.text } //Just read
        }
        expectThat(mutated.list).isSameInstanceAs(source.list)
    }

    @Test
    fun `primitive items are immutable`() {
        val source = PrimitiveList(listOf(0, 1, 2))
        val mutated = source.mutate {
            list.add(30)
            list.addElementAt(0, 999)
            expectThat(list[0]).isEqualTo(999)
            expectThat(list.last()).isEqualTo(30)
        }
        expectThat(mutated.list).hasSize(5)
    }

    @Test
    fun `mutating at index changes the value`() {
        val source = ListClass(listOf(ListItem("not mutated")))
        val mutated = source.mutate {
            this.list[0].text = "mutated"
        }
        expectThat(mutated.list[0].text).isEqualTo("mutated")
    }

    @Test
    fun `elements are added`() {
        val source = ListClass()
        val addedItem = ListItem("element")
        val mutated = source.mutate {
            list.addElement(addedItem)
        }
        expectThat(mutated.list) {
            hasSize(1)
            elementAt(0).isSameInstanceAs(addedItem)
        }
    }

    @Test
    fun `elements at index are added`() {
        val source = ListClass(list = listOf(ListItem()))
        val addedItem = ListItem("first")
        val mutated = source.mutate {
            list.addElementAt(0, addedItem)
        }
        expectThat(mutated.list) {
            hasSize(2)
            elementAt(0).isSameInstanceAs(addedItem)
        }
    }

    @Test
    fun `elements are removed`() {
        val source = ListClass(listOf(ListItem("not mutated")))
        val mutated = source.mutate {
            list.removeAt(0)
        }
        expectThat(mutated.list).isEmpty()
    }

    @Test
    fun `elements are cleared`() {
        val source = ListClass(listOf(ListItem("not mutated")))
        val mutated = source.mutate {
            list.clear()
        }
        expectThat(mutated.list).isEmpty()
    }

    @Test
    fun `elements are retained`() {
        val source = ListClass(listOf(ListItem("1"), ListItem("2")))
        val mutated = source.mutate {
            this.list.retainAll { it.text == "1" }
        }
        expectThat(mutated.list[0]).isSameInstanceAs(source.list[0])
        expectThat(mutated.list).hasSize(1)
    }

    @Test
    fun `mutable iterator deletes items`() {
        val source = ListClass(listOf(ListItem("1"), ListItem("2")))
        val mutated = source.mutate {
            with(list.iterator()) {
                while (hasNext()) {
                    next()
                    remove()
                }
            }
        }
        expectThat(mutated.list).isEmpty()
    }
}