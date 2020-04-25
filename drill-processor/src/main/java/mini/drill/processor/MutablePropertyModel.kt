package mini.drill.processor

import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import mini.drill.Drill
import javax.lang.model.element.Element

@KotlinPoetMetadataPreview
data class MutablePropertyModel(
    val container: MutableClassModel,
    val sourceProp: PropertySpec
) {
    companion object {
        val drillAnnotationClassName = Drill::class.asClassName()
    }

    /** Corresponding element from java APIs */
    val typeElement: Element = container.typeElement.enclosedElements.find {
        it.simpleName.toString() == sourceProp.name
    } ?: throw UnsupportedOperationException("Matching field for ${sourceProp.name} not found")

    private val drillAnnotation: Drill? = typeElement.getAnnotation(Drill::class.java)

    val ignore: Boolean get() = drillAnnotation?.ignore ?: false
    val asReference: Boolean get() = drillAnnotation?.asReference ?: false

    val type get() = sourceProp.type
    val name get() = sourceProp.name
}