package sample

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

internal class NestedClassTest {

    @Test
    fun `internal mutable class propagates changes`() {
        val source = NestedClass(
            nested = Child("nested")
        )
        val mutated = source.mutate {
            nested.x = "mutated"
        }
        expectThat(mutated.nested.x).isEqualTo("mutated")
        expectThat(mutated.nested2).isSameInstanceAs(source.nested2)
    }

    @Test
    fun `reference marked fields are generated as references`() {
        val child = Child("new_child")
        val source = NestedClass(
            nested = Child("nested")
        )
        val mutated = source.mutate {
            this.nested2 = child
        }
        expectThat(mutated.nested2).isSameInstanceAs(child)
    }

    @Test
    fun `nullability is preserved`() {
        val source = NestedClassNullable(
            nested = Child()
        )
        val mutated = source.mutate {
            this.setNested(null)
        }
        expectThat(mutated.nested).isNull()
    }
}