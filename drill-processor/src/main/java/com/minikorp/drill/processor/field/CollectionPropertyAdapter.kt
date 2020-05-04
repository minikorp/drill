package com.minikorp.drill.processor.field

import com.minikorp.drill.DrillList
import com.minikorp.drill.DrillMap
import com.minikorp.drill.processor.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
class CollectionPropertyAdapter(sourceProp: MutablePropertyModel) : PropertyAdapter(sourceProp) {

    companion object {
        private val listGenerator = { from: ParameterizedTypeName, to: TypeName ->
            DrillList::class
                .asClassName()
                .parameterizedBy(
                    from.typeArguments[0],
                    to
                ).copy(nullable = from.isNullable)
        }

        private val mapGenerator = { from: ParameterizedTypeName, to: TypeName ->
            DrillMap::class
                .asClassName()
                .parameterizedBy(
                    from.typeArguments[0],
                    from.typeArguments[1],
                    to
                ).copy(nullable = from.isNullable)

        }

        val supportedAdapters = mapOf<TypeName, (ParameterizedTypeName, TypeName) -> TypeName>(
            LIST to listGenerator,
            MUTABLE_LIST to listGenerator,
            MUTABLE_MAP to mapGenerator,
            MAP to mapGenerator
        )

        fun supportsType(type: TypeName): Boolean {
            return type is ParameterizedTypeName && supportedAdapters.containsKey(type.rawType)
        }
    }

    private val recursiveType = generateRecursiveType(sourceProp.type)

    override val freezeExpression: CodeBlock
        get() = CodeBlock.of(
            "if ($backingPropertyName === %T) " +
                    "$refPropertyAccessor " +
                    "else ($backingPropertyName as %T)${call}freeze()",
            unsetClassName,
            recursiveType.type.copy(nullable = nullable)
        )

    override fun generate(builder: TypeSpec.Builder) {

        val accessorField = PropertySpec.builder(sourceProp.name, recursiveType.type)
            .addKdoc(refPropertyKdoc)
            .mutable(false)
            .getter(
                FunSpec.getterBuilder()
                    .addAnnotation(
                        suppressAnnotation(
                            UNCHECKED,
                            NAME_SHADOWING
                        )
                    )
                    .beginControlFlow(
                        "if (${backingPropertyName} === %T)",
                        unsetClassName
                    )
                    .addStatement("$backingPropertyName = ${refPropertyAccessor}.let { ")
                    .addStatement("val container = this")
                    .addCode(recursiveType.mutate).addCode(" }")
                    .endControlFlow()
                    .addStatement("return $backingPropertyName as %T", recursiveType.type)
                    .build()
            ).build()

        val setterFunction = FunSpec.builder("set" + sourceProp.name.capitalize())
            .addAnnotation(suppressAnnotation(NAME_SHADOWING))
            .addParameter(sourceProp.name, sourceProp.type)
            .addStatement("val param = ${sourceProp.name}")
            .addCode("$backingPropertyName = param.let { ")
            .addStatement("val container = this")
            .addCode(recursiveType.mutate)
            .addCode(" }\n")
            .addStatement("markDirty()")
            .build()

        builder.addFunction(setterFunction)
        builder.addProperty(backingField)
        builder.addProperty(accessorField)
    }

    private fun generateRecursiveType(type: TypeName): RecursiveType {
        val call = if (type.isNullable) "?." else "."

        val nestedModel = ProcessorState.findMutableClass(type)
        if (nestedModel != null) {
            val nestedTypeName = nestedModel.mutableClassType.copy(nullable = type.isNullable)
            return RecursiveType(
                type = nestedTypeName,
                mutate = CodeBlock.of("it${call}toMutable(container)"),
                freeze = CodeBlock.of("it${call}freeze()")
            )
        }

        if (type is ParameterizedTypeName) {
            val adapter = supportedAdapters[type.rawType]
            if (adapter != null) {
                val param = generateRecursiveType(type.typeArguments.last())
                val collectionType = adapter(type, param.type)
                return RecursiveType(
                    type = collectionType,
                    mutate = CodeBlock.of(
                        "it${call}toMutable(container, " +
                                "mutate={ container, it -> ${param.mutate} }," +
                                "freeze={ ${param.freeze} } )"
                    ),
                    freeze = CodeBlock.of("it${call}freeze()")
                )
            }
        }

        //Any other type that can't be made mutable or replace
        return RecursiveType(
            type = type,
            mutate = CodeBlock.of("it"),
            freeze = CodeBlock.of("it")
        ) //Done
    }

    data class RecursiveType(
        val type: TypeName,
        val mutate: CodeBlock,
        val freeze: CodeBlock
    )
}
