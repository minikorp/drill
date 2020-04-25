package sample

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotSameInstanceAs
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs
import kotlin.reflect.full.memberProperties


internal class SimpleClassTest {

    @Test
    fun `values mutate`() {
        val source = SimpleClass(field = "1", ignoredField = "2")
        val mutated = source.mutate {
            field = "mutated"
            println(this)
        }
        expectThat(mutated.field).isEqualTo("mutated")
        expectThat(mutated.ignoredField).isEqualTo(source.ignoredField)
    }

    @Test
    fun `ignore fields are ignored`() {
        val source = SimpleClass(field = "1", ignoredField = "2")
        val mutated = source.mutate {
            val ignoredField = this::class.memberProperties.find {
                it.name == SimpleClass::ignoredField.name
            }
            expectThat(ignoredField).isNull() //Make sure property doesn't exist in generated code
        }
        expectThat(mutated.ignoredField).isSameInstanceAs(source.ignoredField)
    }

    @Test
    fun `non mutated class maintains reference`() {
        val source = SimpleClass(field = "1", ignoredField = "2")
        val mutated = source.mutate {}
        expectThat(source).isSameInstanceAs(mutated)
    }

    @Test
    fun `mark dirty creates a new reference`() {
        val source = SimpleClass(field = "1", ignoredField = "2")
        val mutated = source.mutate { markDirty() }
        expectThat(source).isNotSameInstanceAs(mutated)
    }

    @Test
    fun `nullability is preserved`() {
        val source = SimpleClassNullable(field = "not_null")
        val mutated = source.mutate {
            this.field = null
        }
        expectThat(mutated.field).isNull()
    }
}