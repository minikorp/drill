package mini.drill.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import mini.drill.DrillType
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.lang.model.element.TypeElement

@KotlinPoetMetadataPreview
data class MutableClassModel(val typeElement: TypeElement) {

    companion object {
        const val SUFFIX = "_Mutable"
        const val PREFIX = "__"
        val SOURCE_PROPERTY = DrillType<*>::refDrill.name
        val DIRTY_PROPERTY = DrillType<*>::dirtyDrill.name
        val PARENT_PROPERTY = DrillType<*>::parentDrill.name

        private val mutableInterfaceTypeName = DrillType::class.asClassName()
        private val mutableInterfaceTypeNameStar = DrillType::class.asClassName()
            .parameterizedBy(STAR)
        val mutableInterfaceTypeNameStarNullable = mutableInterfaceTypeNameStar.copy(nullable = true)
    }

    private val debug = ArrayList<String>()
    val mutableClassType: ClassName
    val fileBuilder: FileSpec.Builder

    private val spec: TypeSpec = typeElement.toTypeSpec()
    val originalClassName: ClassName = typeElement.asClassName()
    private var fields: List<MutableFieldModel> = emptyList()

    init {
        mutableClassType =
            ClassName(
                originalClassName.packageName,
                "${originalClassName.simpleNames.joinToString("_")}$SUFFIX"
            )
        ProcessorState.mutableClasses[originalClassName.toString()] = this
        fileBuilder = FileSpec.builder(
            mutableClassType.packageName,
            mutableClassType.simpleName
        )
    }

    fun generate() {
        try {
            //Import mutable extension
            fileBuilder.addImport("mini.drill", "toMutable")
            generateMutableClass()
            fileBuilder.addComment(debug.joinToString(separator = "\n")).build()
        } catch (e: Throwable) {
            val out = ByteArrayOutputStream()
            e.printStackTrace(PrintStream(out))
            fileBuilder.addComment(out.toString())
            //logError(out.toString(), typeElement)
        } finally {
            fileBuilder.build().writeToFile(typeElement)
        }
    }

    private fun generateMutableClass() {
        fields = spec.propertySpecs.map { prop ->
            MutableFieldModel.create(this, prop)
        }

        val classBuilder = TypeSpec.classBuilder(mutableClassType)
            .addSuperinterface(mutableInterfaceTypeName.parameterizedBy(originalClassName))
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(SOURCE_PROPERTY, originalClassName)
                    .addParameter(PARENT_PROPERTY, mutableInterfaceTypeNameStarNullable)
                    .build()
            ).addProperty( //Source field
                PropertySpec.builder(SOURCE_PROPERTY, originalClassName)
                    .initializer(SOURCE_PROPERTY)
                    .addModifiers(KModifier.OVERRIDE)
                    .mutable(true)
                    .build()
            )
            .addProperty( //Parent field
                PropertySpec.builder(PARENT_PROPERTY, mutableInterfaceTypeNameStarNullable)
                    .initializer(PARENT_PROPERTY)
                    .addModifiers(KModifier.OVERRIDE)
                    .build()
            )
            .addProperty( //Dirty field
                PropertySpec.builder(DIRTY_PROPERTY, BOOLEAN)
                    .mutable(true)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("false")
                    .build()
            )

        generateGenericFunctions(classBuilder)
        fields.map { it.generate(classBuilder) }

        fileBuilder.addType(classBuilder.build())
        generateMutateExtension(fileBuilder)
    }

    private fun generateGenericFunctions(classBuilder: TypeSpec.Builder) {
        val callCodeBlock = CodeBlock.builder()
        fields.forEach {
            callCodeBlock.add(it.freezeExpression)
        }
        val callArgs = fields.joinToString(separator = ",\n") {
            "${it.sourceProp.name} = ${it.freezeExpression}"
        }
        val freezeFun = FunSpec.builder("freeze")
            .returns(originalClassName)
            .addModifiers(KModifier.OVERRIDE)
            .addAnnotation(UNCHECKED)
            .beginControlFlow("if (${DIRTY_PROPERTY})")
            .addStatement("return %T($callArgs)", originalClassName)
            .endControlFlow()
            .addStatement("return $SOURCE_PROPERTY")
            .build()

        classBuilder.addFunction(freezeFun)

    }

    private fun generateMutateExtension(fileBuilder: FileSpec.Builder) {
        val toMutableExtension = FunSpec.builder("toMutable")
            .returns(mutableClassType)
            .receiver(originalClassName)
            .addParameter(
                ParameterSpec.builder("parent", mutableInterfaceTypeNameStarNullable)
                    .defaultValue("null")
                    .build()
            )
            .addStatement("return %T(this, parent)", mutableClassType)
            .build()

        val mutateExtension = FunSpec.builder("mutate")
            .addModifiers(KModifier.INLINE)
            .addParameter(
                ParameterSpec
                    .builder(
                        "fn", LambdaTypeName.get(
                            receiver = null,
                            parameters = *arrayOf(mutableClassType),
                            returnType = Unit::class.asClassName()
                        )
                    )
                    .addModifiers(KModifier.CROSSINLINE)
                    .build()
            )
            .returns(originalClassName)
            .receiver(originalClassName)
            .addStatement("val mutable = this.${toMutableExtension.name}()")
            .addStatement("fn(mutable)")
            .addStatement("return mutable.freeze()")
            .build()

        fileBuilder.addFunction(mutateExtension)
        fileBuilder.addFunction(toMutableExtension)
    }

}


