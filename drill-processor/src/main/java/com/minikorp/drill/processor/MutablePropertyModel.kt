package com.minikorp.drill.processor

import com.minikorp.drill.DrillProperty
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import javax.lang.model.element.Element

@KotlinPoetMetadataPreview
data class MutablePropertyModel(
    val container: MutableClassModel,
    val sourceProp: PropertySpec
) {
    /** Corresponding element from java APIs */

    val typeElement: Element = container.typeElement.enclosedElements.find {
        it.simpleName.toString() == sourceProp.name
    } ?: throw UnsupportedOperationException("Matching field for ${sourceProp.name} not found")

    private val annotation: DrillProperty? = typeElement.getAnnotation(
        DrillProperty::class.java
    )

    val ignore: Boolean get() = annotation?.ignore ?: false
    val asReference: Boolean get() = annotation?.asReference ?: false

    val type get() = sourceProp.type
    val name get() = sourceProp.name
}