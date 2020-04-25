package mini.drill.processor.field

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import mini.drill.processor.MutablePropertyModel

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