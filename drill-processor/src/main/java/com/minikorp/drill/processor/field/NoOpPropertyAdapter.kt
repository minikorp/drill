package com.minikorp.drill.processor.field

import com.minikorp.drill.processor.MutablePropertyModel
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

/**
 * Plain reference that just makes val -> var
 */
@KotlinPoetMetadataPreview
class NoOpPropertyAdapter(sourceProp: MutablePropertyModel) : PropertyAdapter(sourceProp) {

    override val freezeExpression: CodeBlock
        get() = CodeBlock.of(refPropertyAccessor)

    override fun generate(builder: TypeSpec.Builder) {

    }
}