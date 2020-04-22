package mini.drill.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import mini.drill.DrillList
import mini.drill.DrillMap

@KotlinPoetMetadataPreview
class MutableCollectionFieldModel(
    container: MutableClassModel,
    sourceProp: PropertySpec
) : MutableFieldModel(container, sourceProp) {

    private val recursiveType = generateRecursiveType(sourceProp.type)

    override val freezeExpression: CodeBlock
        get() = CodeBlock.of(
            "if ($backingFieldName === %T) " +
                    "$sourceMemberName " +
                    "else ($backingFieldName as %T)${call}freeze()",
            unsetClassName,
            recursiveType.type.copy(nullable = nullable)
        )

    override fun generate(builder: TypeSpec.Builder) {
        val mutateBlock = recursiveType.mutate
        val accessorField = PropertySpec.builder(sourceProp.name, recursiveType.type)
            .mutable(false)
            .getter(
                FunSpec.getterBuilder()
                    .addAnnotation(UNCHECKED)
                    .beginControlFlow(
                        "if (${backingFieldName} === %T)", unsetClassName
                    )
                    .addStatement("$backingFieldName = ${sourceMemberName}.let { ")
                    .addStatement("val container = this")
                    .addCode(mutateBlock).addCode(" }")
                    .endControlFlow()
                    .addStatement("return $backingFieldName as %T", recursiveType.type)
                    .build()
            ).build()

        val setterFunction = FunSpec.builder("set" + sourceProp.name.capitalize())
            .addParameter(sourceProp.name, sourceProp.type)
            .addStatement("val param = ${sourceProp.name}")
            .addCode("$backingFieldName = param.let { ")
            .addStatement("val container = this")
            .addCode(mutateBlock)
            .addCode(" }\n")
            .addStatement("markDirty()")
            .build()

        builder.addFunction(setterFunction)
        builder.addProperty(backingField)
        builder.addProperty(accessorField)
    }

    private fun generateRecursiveType(type: TypeName): RecursiveType {
        val nestedModel = ProcessorState.findMutableModel(type)

        val call = if (type.isNullable) "?." else "."

        if (nestedModel != null) {
            val nestedTypeName = nestedModel.mutableClassType.copy(nullable = type.isNullable)
            return RecursiveType(
                type = nestedTypeName,
                mutate = CodeBlock.of("it${call}toMutable(container)"),
                freeze = CodeBlock.of("it${call}freeze()")
            )
        }

        if (type is ParameterizedTypeName) {
            if (type.rawType == LIST) {
                val param = generateRecursiveType(type.typeArguments[0])
                val listType = DrillList::class
                    .asClassName()
                    .parameterizedBy(
                        type.typeArguments[0],
                        param.type
                    ).copy(nullable = type.isNullable)

                return RecursiveType(
                    type = listType,
                    mutate = CodeBlock.of(
                        "it${call}toMutable(container, " +
                                "mutate={ container, it -> ${param.mutate} }," +
                                "freeze={ ${param.freeze} } )"
                    ),
                    freeze = CodeBlock.of("it${call}freeze()")
                )
            }

            if (type.rawType == MAP) {
                val param = generateRecursiveType(type.typeArguments[1])
                val mapType = DrillMap::class
                    .asClassName()
                    .parameterizedBy(
                        type.typeArguments[0],
                        type.typeArguments[1],
                        param.type
                    ).copy(nullable = type.isNullable)

                return RecursiveType(
                    type = mapType,
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
