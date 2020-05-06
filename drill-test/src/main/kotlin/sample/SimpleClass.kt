package sample

import com.minikorp.drill.Drill
import com.minikorp.drill.DrillProperty

@Drill
data class SimpleClass(
    val field: String,
    @DrillProperty(ignore = true)
    val ignoredField: String = ""
) {
    val outsideField = field + "outside"

    fun outsideMethod() {

    }
}

@Drill
data class SimpleClassNullable(
    val field: String? = null
)