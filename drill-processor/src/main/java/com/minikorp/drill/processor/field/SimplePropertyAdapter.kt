package com.minikorp.drill.processor.field

import com.minikorp.drill.processor.MutableClassModel
import com.minikorp.drill.processor.MutablePropertyModel
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

/**
 * Plain reference that just makes val -> var
 */
@KotlinPoetMetadataPreview
class SimplePropertyAdapter(sourceProp: MutablePropertyModel) : PropertyAdapter(sourceProp) {

    override val freezeExpression: CodeBlock
        get() = CodeBlock.of(sourceProp.name)

    override val isDirtyExpression: CodeBlock
        get() = CodeBlock.of("$refPropertyAccessor !== ${sourceProp.name}")

    override val stringExpression: String
        get() {
            val q = if (sourceProp.type == STRING) "\\\"" else ""
            return "$q\${$refPropertyAccessor}$q·->·$q\${${sourceProp.name}}$q"
        }

    override fun generate(builder: TypeSpec.Builder) {
        builder.addProperty(
            PropertySpec.builder(
                sourceProp.name,
                sourceProp.type
            ).mutable(true)
                .addKdoc(refPropertyKdoc)
                .initializer("${MutableClassModel.SOURCE_PROPERTY}.${sourceProp.name}")
                .setter(
                    FunSpec.setterBuilder()
                        .addParameter("value", sourceProp.type)
                        .addCode("field = value.also { markDirty() }")
                        .build()
                ).build()
        )
    }
}