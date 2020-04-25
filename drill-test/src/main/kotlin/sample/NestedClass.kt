package sample

import mini.drill.Drill

@Drill
data class NestedClass(
    val nested: Child = Child("nested"),
    @Drill(asReference = true)
    val nested2: Child = Child("nested2")
)

@Drill
data class NestedClassNullable(
    val nested: Child? = Child("")
)

@Drill
data class Child(val x: String = "child_x")
