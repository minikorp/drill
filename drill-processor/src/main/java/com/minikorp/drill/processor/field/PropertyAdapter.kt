package com.minikorp.drill.processor.field

import com.minikorp.drill.UNSET_VALUE
import com.minikorp.drill.processor.MutableClassModel
import com.minikorp.drill.processor.MutablePropertyModel
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@KotlinPoetMetadataPreview
abstract class PropertyAdapter(val sourceProp: MutablePropertyModel) {
    companion object {
        val unsetClassName = UNSET_VALUE::class.asClassName()

        fun createAdapter(sourceProp: MutablePropertyModel): PropertyAdapter {
            val type = sourceProp.type

            return when {
                sourceProp.ignore -> {
                    NoOpPropertyAdapter(sourceProp)
                }
                sourceProp.asReference -> {
                    SimplePropertyAdapter(sourceProp)
                }
                ReferencePropertyAdapter.supportsType(
                    type
                ) -> {
                    ReferencePropertyAdapter(sourceProp)
                }
                CollectionPropertyAdapter.supportsType(
                    type
                ) -> {
                    CollectionPropertyAdapter(sourceProp)
                }
                else -> SimplePropertyAdapter(sourceProp)
            }
        }
    }

    open val isDirtyExpression: CodeBlock
        get() {
            val dirtyIf = if (nullable) {
                "(${sourceProp.name}${call}dirty() ?: true)"
            } else {
                "${sourceProp.name}${call}dirty()"
            }
            return CodeBlock.of(
                "($backingPropertyName !== %T && $backingPropertyName !== $refPropertyAccessor) || $dirtyIf",
                unsetClassName
            )
        }
    abstract val freezeExpression: CodeBlock
    open val stringExpression: CodeBlock
        get() {
            return CodeBlock.of("\${${backingPropertyName}}")
        }

    abstract fun generate(builder: TypeSpec.Builder)

    /** parent.real_prop */
    val refPropertyAccessor = "${MutableClassModel.SOURCE_PROPERTY}.${sourceProp.name}"
    val refPropertyKdoc = CodeBlock.of("[%T.${sourceProp.name}]", sourceProp.container.originalClassType)
    val backingPropertyName = "_${sourceProp.name}"
    val nullable: Boolean = sourceProp.type.isNullable

    /** Safe call ?. or . */
    val call = if (nullable) "?." else "."

    /** Backing field that stores the mutable object or special [UNSET_VALUE] token. */
    val backingField = PropertySpec.builder(
        backingPropertyName,
        ANY.copy(nullable = true)
    ).addModifiers(KModifier.PRIVATE)
        .mutable(true)
        .initializer(
            "%T",
            unsetClassName
        )
        .build()


}