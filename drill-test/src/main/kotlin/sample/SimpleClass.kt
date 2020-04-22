package sample

import mini.drill.Drill

@Drill
data class SimpleClass(val x: String)


@Drill
data class SimpleClassNullable(val x: String?)