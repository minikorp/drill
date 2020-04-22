package mini.drill.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import mini.drill.UNSET_VALUE

@KotlinPoetMetadataPreview
abstract class MutableFieldModel(
    val container: MutableClassModel,
    val sourceProp: PropertySpec
) {

    abstract val freezeExpression: CodeBlock
    abstract fun generate(builder: TypeSpec.Builder)

    val sourceMemberName = "${MutableClassModel.SOURCE_PROPERTY}.${sourceProp.name}"
    val backingFieldName = "${MutableClassModel.PREFIX}backing_${sourceProp.name}"
    val nullable: Boolean = sourceProp.type.isNullable
    val call = if (nullable) "?." else "."

    val backingField = PropertySpec.builder(
        backingFieldName,
        ANY.copy(nullable = true)
    ).addModifiers(KModifier.PRIVATE)
        .mutable(true)
        .initializer("%T", unsetClassName)
        .build()

    companion object {
        val unsetClassName = UNSET_VALUE::class.asClassName()

        fun create(
            container: MutableClassModel,
            sourceProp: PropertySpec
        ): MutableFieldModel {
            val type = sourceProp.type

            val nestedMutableClass = ProcessorState.findMutableModel(type)
            if (nestedMutableClass != null) {
                return NestedMutableFieldModel(container, sourceProp, nestedMutableClass)
            }

            if (type is ParameterizedTypeName) {
                if (type.rawType == LIST || type.rawType == MAP) {
                    return MutableCollectionFieldModel(
                        container, sourceProp
                    )
                }
            }

            return SimpleMutableFieldModel(container, sourceProp)
        }
    }


    class SimpleMutableFieldModel(
        container: MutableClassModel,
        sourceProp: PropertySpec
    ) : MutableFieldModel(container, sourceProp) {

        override val freezeExpression: CodeBlock
            get() = CodeBlock.of(sourceProp.name)

        override fun generate(builder: TypeSpec.Builder) {
            builder.addProperty(
                PropertySpec.builder(
                    sourceProp.name,
                    sourceProp.type
                ).mutable(true)
                    .initializer("${MutableClassModel.SOURCE_PROPERTY}.${sourceProp.name}")
                    .setter(
                        FunSpec.setterBuilder()
                            .addParameter("value", sourceProp.type)
                            .addStatement("field = value")
                            .addStatement("markDirty()")
                            .build()
                    ).build()
            )
        }
    }

    class NestedMutableFieldModel(
        container: MutableClassModel,
        sourceProp: PropertySpec,
        private val nestedMutableClass: MutableClassModel
    ) : MutableFieldModel(container, sourceProp) {

        private val mutableClassTypeName = nestedMutableClass.mutableClassType.copy(nullable = nullable)

        override val freezeExpression =
            CodeBlock.of(
                "if ($backingFieldName === %T) " +
                        "$sourceMemberName " +
                        "else ($backingFieldName as %T)${call}freeze()",
                unsetClassName,
                mutableClassTypeName
            )

        override fun generate(builder: TypeSpec.Builder) {
            val backingField = PropertySpec.builder(
                backingFieldName,
                ANY.copy(nullable = true)
            )
                .addModifiers(KModifier.PRIVATE)
                .mutable(true)
                .initializer("%T", unsetClassName)
                .build()

            val accessorField = PropertySpec.builder(sourceProp.name, mutableClassTypeName)
                .mutable(false)
                .getter(
                    FunSpec.getterBuilder()
                        .beginControlFlow(
                            "if (${backingFieldName} === %T)", unsetClassName
                        )
                        .addStatement("$backingFieldName = ${sourceMemberName}${call}toMutable(this)")
                        .endControlFlow()
                        .addStatement("return $backingFieldName as %T", mutableClassTypeName)
                        .build()
                )
                .build()

            val setterFunction = FunSpec.builder("set" + sourceProp.name.capitalize())
                .addParameter(sourceProp.name, sourceProp.type)
                .addStatement("$backingFieldName = ${sourceProp.name}${call}toMutable(this)")
                .addStatement("markDirty()")
                .build()

            //Public mutable class accessor
            builder.addFunction(setterFunction)
            builder.addProperties(listOf(backingField, accessorField))
        }
    }
}