package com.minikorp.drill.processor.field

import com.minikorp.drill.processor.MutablePropertyModel
import com.minikorp.drill.processor.ProcessorState
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
class ReferencePropertyAdapter(sourceProp: MutablePropertyModel) : PropertyAdapter(sourceProp) {

    companion object {
        fun supportsType(type: TypeName): Boolean {
            return ProcessorState.findMutableClass(type) != null
        }
    }

    private val mutableClassTypeName = kotlin.run {
        val nestedMutable = ProcessorState.findMutableClass(sourceProp.type)
        nestedMutable!!.mutableClassType.copy(nullable = sourceProp.type.isNullable)
    }

    override val freezeExpression =
        CodeBlock.of(
            "if ($backingPropertyName === %T) " +
                    "$refPropertyAccessor " +
                    "else ($backingPropertyName as %T)${call}freeze()",
            unsetClassName,
            mutableClassTypeName
        )

    override fun generate(builder: TypeSpec.Builder) {
        val accessorField =
            PropertySpec.builder(sourceProp.name, mutableClassTypeName)
                .addKdoc(refPropertyKdoc)
                .mutable(false)
                .getter(
                    FunSpec.getterBuilder()
                        .beginControlFlow(
                            "if (${backingPropertyName} === %T)",
                            unsetClassName
                        )
                        .addStatement("$backingPropertyName = ${refPropertyAccessor}${call}toMutable(this)")
                        .endControlFlow()
                        .addStatement("return $backingPropertyName as %T", mutableClassTypeName)
                        .build()
                )
                .build()

        val setterFunction = FunSpec.builder("set" + sourceProp.name.capitalize())
            .addParameter(sourceProp.name, sourceProp.type)
            .addStatement("$backingPropertyName = ${sourceProp.name}${call}toMutable(this)")
            .addStatement("markDirty()")
            .build()

        //Public mutable class accessor
        builder.addFunction(setterFunction)
        builder.addProperties(listOf(backingField, accessorField))
    }
}