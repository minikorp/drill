package sample

import mini.drill.Drill

//@Drill
//data class MapClassPrimitive(
//    val Map: Map<Int>
//)

@Drill
data class MapClass(
    val list: List<SimpleClass>,
    val map: Map<String, SimpleClass>,
    val crazyThing: List<Map<String, Map<String, SimpleClass>>>
)

@Drill
data class MapNullableClass(
    val map: Map<String, SimpleClass>?
)

@Drill
data class MapNullableTypeClass(
    val map: Map<String, SimpleClass?>
)

@Drill
data class NestedMapNullableTypeClass(
    val map: Map<String, Map<String, SimpleClass>?>,
    val primitive: Map<String, Map<String?, Int>?>
)
