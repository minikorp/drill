package mini.drill.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import mini.drill.DrillType
import mini.drill.processor.field.PropertyAdapter
import javax.lang.model.element.TypeElement

@KotlinPoetMetadataPreview
data class MutableClassModel(val typeElement: TypeElement) {

    companion object {
        const val SUFFIX = "_Mutable"
        val SOURCE_PROPERTY = DrillType<*>::_ref.name
        val DIRTY_PROPERTY = DrillType<*>::_dirty.name
        val PARENT_PROPERTY = DrillType<*>::_parent.name

        private val mutableInterfaceTypeName = DrillType::class.asClassName()
        private val mutableInterfaceTypeNameStar = DrillType::class.asClassName()
            .parameterizedBy(STAR)
        val mutableInterfaceTypeNameStarNullable = mutableInterfaceTypeNameStar.copy(nullable = true)
    }

    private val debug = ArrayList<String>()
    val mutableClassType: ClassName
    val fileBuilder: FileSpec.Builder

    private val spec: TypeSpec = typeElement.toTypeSpec()
    private val properties: List<MutablePropertyModel> = spec.propertySpecs.map { MutablePropertyModel(this, it) }

    val originalClassName: ClassName = typeElement.asClassName()
    private var adapters: List<PropertyAdapter> = emptyList()

    init {
        mutableClassType =
            ClassName(
                originalClassName.packageName,
                "${originalClassName.simpleNames.joinToString("_")}$SUFFIX"
            )

        fileBuilder = FileSpec.builder(
            mutableClassType.packageName,
            mutableClassType.simpleName
        )

        ProcessorState.registerMutableClass(this)
    }

    fun generate() {
        try {
            //Import mutable extension
            fileBuilder.addImport("mini.drill", "toMutable")
            generateMutableClass()
            fileBuilder.addComment(debug.joinToString(separator = "\n")).build()
        } catch (e: Throwable) {
            fileBuilder.addComment(e.stackTraceString())
            throw e
        } finally {
            fileBuilder.build().writeToFile(typeElement)
        }
    }

    private fun generateMutableClass() {
        adapters = properties.map { prop ->
            PropertyAdapter.createAdapter(prop)
        }

        val classBuilder = TypeSpec.classBuilder(mutableClassType)
            .addSuperinterface(mutableInterfaceTypeName.parameterizedBy(originalClassName))
            .addKdoc("[%T]", originalClassName)
            .addModifiers(KModifier.DATA)
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
        adapters.map { it.generate(classBuilder) }

        fileBuilder.addType(classBuilder.build())
        generateMutateExtension(fileBuilder)
    }

    private fun generateGenericFunctions(classBuilder: TypeSpec.Builder) {
        val callCodeBlock = CodeBlock.builder()
        adapters.forEach {
            callCodeBlock.add(it.freezeExpression)
        }
        val callArgs = adapters.joinToString(separator = ",\n") {
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

        val x = ""
        x.let { }

        val mutateExtension = FunSpec.builder("mutate")
            .addModifiers(KModifier.INLINE)
            .addParameter(
                ParameterSpec
                    .builder(
                        "block", LambdaTypeName.get(
                            receiver = mutableClassType,
                            returnType = Unit::class.asClassName()
                        )
                    )
                    .addModifiers(KModifier.CROSSINLINE)
                    .build()
            )
            .returns(originalClassName)
            .receiver(originalClassName)
            .addStatement("val mutable = this.${toMutableExtension.name}()")
            .addStatement("mutable.block()")
            .addStatement("return mutable.freeze()")
            .build()

        fileBuilder.addFunction(mutateExtension)
        fileBuilder.addFunction(toMutableExtension)
    }

}


