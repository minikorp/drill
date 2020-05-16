package sample

import com.minikorp.drill.Drill
import com.minikorp.drill.DrillProperty

@Drill
data class SimpleClass(
    val field: String,
    @DrillProperty(ignore = true)
    val ignoredField: String = ""
) {
    val outsideField = field + "_not_in_constructor"

    fun outsideMethod() {

    }
}

@Drill
data class SimpleClassNullable(
    val field: String? = null
)