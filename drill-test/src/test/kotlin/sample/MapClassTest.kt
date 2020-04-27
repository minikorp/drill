package sample

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.*

internal class MapClassTest {

    @Test
    fun `empty mutation keeps reference`() {
        val source = MapClass(mapOf("a" to MapItem("123")))
        val mutated = source.mutate {
            this.map.entries.forEach {
                it.value //Just read
            }
        }
        expectThat(source.map).isSameInstanceAs(mutated.map)
    }

    @Test
    fun `primitive items are added`() {
        val source = PrimitiveMap(mapOf("a" to 10))
        val mutated = source.mutate {
            this.map["3"] = 44
        }
        expectThat(mutated.map).hasSize(2)
    }

    @Test
    fun `nested items are added`() {
        val source = MapClass()
        val newItem = MapItem("added")
        val mutated = source.mutate {
            this.map["a"] = newItem
        }
        expectThat(mutated.map).containsKey("a")
        expectThat(mutated.map["a"]).isSameInstanceAs(newItem)
    }

    @Test
    fun `mutating nested value updates the map`() {
        val source = MapClass(mapOf("a" to MapItem("123")))
        val mutated = source.mutate {
            this.map["a"]?.text = "mutated"
        }
        expectThat(mutated.map["a"]?.text).isEqualTo("mutated")
    }

    @Test
    fun `elements are removed`() {
        val source = MapClass(mapOf("a" to MapItem("123")))
        val mutated = source.mutate {
            map.remove("a")
        }
        expectThat(mutated.map).isEmpty()
    }

    @Test
    fun `elements are cleared`() {
        val source = MapClass(mapOf("a" to MapItem("123")))
        val mutated = source.mutate {
            map.clear()
        }
        expectThat(mutated.map).isEmpty()
    }

    @Test
    fun `mutable iterator deletes items`() {
        val source = MapClass(mapOf("a" to MapItem("123")))
        val mutated = source.mutate {
            with(map.iterator()) {
                while (hasNext()) {
                    next()
                    remove()
                }
            }
        }
        expectThat(mutated.map).isEmpty()
    }

    @Test
    fun `nested maps mutate`() {
        val source = NestedMapNullableTypeClass(
            map = mapOf(
                "a" to mapOf(
                    "a.1" to MapItem("a.1.text")
                )
            ),
            primitive = mapOf("b" to mapOf("b.1" to 30))
        )
        val mutated = source.mutate {
            map["a"]!!["a.1"]!!.text = "mutated"
            primitive["b"]!!["b.1"] = null
        }
        expectThat(mutated.map["a"]?.get("a.1")?.text).isEqualTo("mutated")
        expectThat(mutated.primitive["b"]?.get("b.1")).isNull()
    }
}