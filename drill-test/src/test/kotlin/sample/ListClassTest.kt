package sample

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

internal class ListClassTest {

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
        val element = ListItem("second")
        val mutated = source.mutate {
            list.add(element)
        }
        expectThat(mutated.list[0]).isSameInstanceAs(element)
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
}