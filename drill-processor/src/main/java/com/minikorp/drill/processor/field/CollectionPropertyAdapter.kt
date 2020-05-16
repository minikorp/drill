package com.minikorp.drill.processor.field

import com.minikorp.drill.DrillList
import com.minikorp.drill.DrillMap
import com.minikorp.drill.processor.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
class CollectionPropertyAdapter(sourceProp: MutablePropertyModel) : PropertyAdapter(sourceProp) {

    class Generator(
        val factory: (from: ParameterizedTypeName, to: TypeName) -> CodeBlock,
        val drillTypeGenerator: (from: ParameterizedTypeName, to: TypeName) -> ParameterizedTypeName
    )

    companion object {
        private val listGenerator = { listType: ParameterizedTypeName,
                                      mutableType: TypeName ->
            DrillList::class
                .asClassName()
                .parameterizedBy(
                    listType.copy(nullable = false), //ListType
                    listType.typeArguments[0], //Immutable
                    mutableType //Mutable
                ).copy(nullable = listType.isNullable) as ParameterizedTypeName
        }
        private val mapGenerator = { mapType: ParameterizedTypeName, to: TypeName ->
            DrillMap::class
                .asClassName()
                .parameterizedBy(
                    mapType.copy(nullable = false),
                    mapType.typeArguments[0],
                    mapType.typeArguments[1],
                    to
                ).copy(nullable = mapType.isNullable) as ParameterizedTypeName
        }

        val supportedAdapters = mapOf<TypeName, Generator>(
            LIST to Generator(
                factory = { from, to -> CodeBlock.of("it.toList()") },
                drillTypeGenerator = listGenerator
            ),
            MUTABLE_LIST to Generator(
                factory = { from, to -> CodeBlock.of("it.toMutableList()") },
                drillTypeGenerator = listGenerator
            ),
            ArrayList::class.asClassName() to Generator(
                factory = { from, to ->
                    CodeBlock.of(
                        "%T().apply { addAll(it) }",
                        ArrayList::class.asClassName().parameterizedBy(from.typeArguments[0])
                    )
                },
                drillTypeGenerator = listGenerator
            ),
            MAP to Generator(
                factory = { from, to -> CodeBlock.of("it") },
                drillTypeGenerator = mapGenerator
            ),
            MUTABLE_MAP to Generator(
                factory = { from, to -> CodeBlock.of("it.toMutableMap()") },
                drillTypeGenerator = mapGenerator
            ),
            HashMap::class.asClassName() to Generator(
                factory = { from, to ->
                    CodeBlock.of(
                        "%T(it)",
                        HashMap::class.asClassName()
                    )
                },
                drillTypeGenerator = mapGenerator
            )

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
            val generator = supportedAdapters[type.rawType]
            if (generator != null) {
                val param = generateRecursiveType(type.typeArguments.last())
                val collectionType = generator.drillTypeGenerator(type, param.type)

                return RecursiveType(
                    type = collectionType,
                    mutate = CodeBlock.builder()
                        .add("it${call}toMutable(container,\n")
                        .indent()
                        .add("factory={ ").add(generator.factory(type, param.type)).add(" },\n")
                        .add("mutate={ container, it -> ${param.mutate} },\n")
                        .add("freeze={ ${param.freeze} })")
                        .unindent()
                        .build(),
                    freeze = CodeBlock.of("it${call}freeze()")
                )
            }
        }

        //Any other type that can't be made mutable
        return RecursiveType(
            type = type,
            mutate = CodeBlock.of("it"),
            freeze = CodeBlock.of("it")
        )
    }

    data class RecursiveType(
        val type: TypeName,
        val mutate: CodeBlock,
        val freeze: CodeBlock
    )
}
