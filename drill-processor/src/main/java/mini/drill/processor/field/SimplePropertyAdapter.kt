package mini.drill.processor.field

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import mini.drill.processor.MutableClassModel
import mini.drill.processor.MutablePropertyModel

/**
 * Plain reference that just makes val -> var
 */
@KotlinPoetMetadataPreview
class SimplePropertyAdapter(sourceProp: MutablePropertyModel) : PropertyAdapter(sourceProp) {

    override val freezeExpression: CodeBlock
        get() = CodeBlock.of(sourceProp.name)

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
                        .addStatement("field = value")
                        .addStatement("markDirty()")
                        .build()
                ).build()
        )
    }
}