package sample

import com.minikorp.drill.Drill

@Drill
data class MapItem(val text: String = "item")

@Drill
data class MapClassVariants(
    val mutableMap: MutableMap<String, MapItem> = HashMap(),
    val hashMap: HashMap<String, MapItem> = HashMap()
)

@Drill
data class MapClass(
    val map: Map<String, MapItem> = emptyMap()
)

@Drill
data class PrimitiveMap(
    val map: Map<String, Int>
)

@Drill
data class MapNullableClass(
    val map: Map<String, MapItem>?
)

@Drill
data class MapNullableTypeClass(
    val map: Map<String, MapItem?>
)

@Drill
data class NestedMapNullableTypeClass(
    val map: Map<String, Map<String, MapItem>?>,
    val primitive: Map<String, Map<String, Int?>>
)
