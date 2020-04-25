package sample

import mini.drill.Drill

@Drill
data class SimpleClass(
    val field: String,
    @Drill(ignore = true)
    val ignoredField: String
)


@Drill
data class SimpleClassNullable(
    val field: String? = null
)