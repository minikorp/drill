package sample

import mini.drill.Drill

@Drill
data class NestedClass(
    val nested: Nested = Nested("")
) {

    @Drill
    data class Nested(val x: String)
}


@Drill
data class NestedClassNullable(
    val nested: NestedClass.Nested? = NestedClass.Nested("")
)