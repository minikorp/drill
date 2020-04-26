package sample

import mini.drill.Drill

@Drill
data class ListItem(val text: String = "item")

@Drill
data class ListClass(
    val list: List<ListItem> = emptyList()
)

@Drill
data class ListNullableClass(
    val list: List<ListItem>?
)

@Drill
data class ListNullableTypeClass(
    val list: List<ListItem?>
)

@Drill
data class NestedListNullableTypeClass(
    val list: List<List<ListItem>?>,
    val primitive: List<List<Int>?>
)
