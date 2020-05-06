package sample

import com.minikorp.drill.Drill
import com.minikorp.drill.DrillProperty

@Drill
data class NestedClass(
    val nested: Child = Child("nested"),
    @DrillProperty(asReference = true)
    val nested2: Child = Child("nested2"),
    @DrillProperty(asReference = true)
    val nested3: Child? = Child("nested3")

)

@Drill
data class NestedClassNullable(
    val nested: Child? = Child("")
)

@Drill
data class Child(val x: String = "child_x")
