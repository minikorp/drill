package com.minikorp.drill.processor

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
object ProcessorState {

    val DEBUG = "false".toBoolean()

    private val mutableClasses: MutableMap<TypeName, MutableClassModel> = hashMapOf()

    fun registerMutableClass(classModel: MutableClassModel) {
        mutableClasses[classModel.originalClassName.copy(nullable = false)] = classModel
    }

    fun findMutableClass(typeName: TypeName): MutableClassModel? {
        return mutableClasses[typeName.copy(nullable = false)]
    }
}