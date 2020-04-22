package mini.drill.processor

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
object ProcessorState {
    val mutableClasses: MutableMap<String, MutableClassModel> = hashMapOf()

    fun findMutableModel(typeName: TypeName): MutableClassModel? {
        return mutableClasses[typeName.copy(nullable = false).toString()]
    }
}