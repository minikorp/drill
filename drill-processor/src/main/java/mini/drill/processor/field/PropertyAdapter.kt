package mini.drill.processor.field

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import mini.drill.UNSET_VALUE
import mini.drill.processor.MutableClassModel
import mini.drill.processor.MutablePropertyModel

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
                ReferencePropertyAdapter.supportsType(type) -> {
                    ReferencePropertyAdapter(sourceProp)
                }
                CollectionPropertyAdapter.supportsType(type) -> {
                    CollectionPropertyAdapter(sourceProp)
                }
                else -> SimplePropertyAdapter(sourceProp)
            }
        }
    }

    abstract val freezeExpression: CodeBlock

    abstract fun generate(builder: TypeSpec.Builder)

    /** parent.real_prop */
    val refPropertyAccessor = "${MutableClassModel.SOURCE_PROPERTY}.${sourceProp.name}"
    val refPropertyKdoc = CodeBlock.of("[%T.${sourceProp.name}]", sourceProp.container.originalClassName)
    val backingPropertyName = "_${sourceProp.name.capitalize()}"
    val nullable: Boolean = sourceProp.type.isNullable

    /** Safe call ?. or . */
    val call = if (nullable) "?." else "."

    /** Backing field that stores the mutable object or special [UNSET_VALUE] token. */
    val backingField = PropertySpec.builder(
        backingPropertyName,
        ANY.copy(nullable = true)
    ).addModifiers(KModifier.PRIVATE)
        .mutable(true)
        .initializer("%T", unsetClassName)
        .build()


}