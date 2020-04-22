package sample

import mini.drill.Drill

//@Drill
//data class ListClassPrimitive(
//    val list: List<Int>
//)

@Drill
data class ListClass(
    val list: List<SimpleClass>
)

@Drill
data class ListNullableClass(
    val list: List<SimpleClass>?
)

@Drill
data class ListNullableTypeClass(
    val list: List<SimpleClass?>
)

@Drill
data class NestedListNullableTypeClass(
    val list: List<List<SimpleClass>?>,
    val primitive: List<List<Int>?>
)
