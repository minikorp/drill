package com.minikorp.drill.processor

import com.minikorp.drill.DefaultDrillType
import com.minikorp.drill.DiffAdapter
import com.minikorp.drill.DrillType
import com.minikorp.drill.processor.field.PropertyAdapter
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

@KotlinPoetMetadataPreview
data class MutableClassModel(val typeElement: TypeElement) {

    companion object {
        private const val DRILL_SUFFIX = "Mutable"
        val SOURCE_PROPERTY = "${DrillType<*>::ref.name}()"
        val DIRTY_PROPERTY = "${DrillType<*>::dirty.name}()"

        private val baseType = DefaultDrillType::class.asClassName()
        private val parentType = DrillType::class.asClassName().parameterizedBy(STAR)
        private val nullableParentType = parentType.copy(nullable = true)
    }

    private val debug = ArrayList<String>()

    val mutableClassType: ClassName
    private val fileBuilder: FileSpec.Builder

    private val spec: TypeSpec = typeElement.toTypeSpec()
    private val properties: List<MutablePropertyModel>

    val originalClassType: ClassName = typeElement.asClassName()
    private var adapters: List<PropertyAdapter> = emptyList()

    init {
        processorAssertion(
            "${spec.name} is not a data class",
            spec.modifiers.contains(KModifier.DATA),
            typeElement
        )

        // "Best" way to figure out what is actual main data class constructor
        // is to find generated copy method that has same parameters as constructor
        val constructors = typeElement.enclosedElements
            .filter { it.isConstructor && it is ExecutableElement }
            .map { it as ExecutableElement }
            .filter { it.parameters.size > 0 }

        //Data classes generate a no-args constructors skip them
        if (constructors.size > 1) {
            logWarning(
                "More than one constructor in data class might lead to bad mutable class generation",
                typeElement
            )
        }

        //First constructor in JVM should always correspond to main data class constructor
        val constructor = constructors[0]

        //Match constructor from JVM signature parameters to properties extracted
        //from KotlinPoet no other way for now without parsing kotlin metadata
        val matchedProperties = constructor.parameters.mapNotNull { constructorParameter ->
            spec.propertySpecs.find { it.name == constructorParameter.simpleName.toString() }
        }
        properties = matchedProperties.map {
            MutablePropertyModel(
                this,
                it
            )
        }

        mutableClassType =
            ClassName(
                originalClassType.packageName,
                "${originalClassType.simpleNames.joinToString("_")}$DRILL_SUFFIX"
            )

        fileBuilder = FileSpec.builder(
            mutableClassType.packageName,
            mutableClassType.simpleName
        )

        if (ProcessorState.DEBUG) {
            fileBuilder.addComment(spec.toString())
        }

        ProcessorState.registerMutableClass(this)
    }

    fun generate() {
        try {
            //Import mutable extension, static dispatch will take care of the rest
            //calling proper function
            fileBuilder.addImport(DrillType::class.asClassName().packageName, "toMutable")
            generateMutableClass()
            generateDiffAdapter()
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
            .addKdoc("[%T]", originalClassType)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(DrillType<*>::ref.name, originalClassType)
                    .addParameter(
                        DrillType<*>::parent.name,
                        nullableParentType
                    )
                    .build()
            )
            .superclass(baseType.parameterizedBy(originalClassType))
            .addSuperclassConstructorParameter("ref")
            .addSuperclassConstructorParameter("parent")

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
            .returns(originalClassType)
            .addModifiers(KModifier.OVERRIDE)
            .addAnnotation(suppressAnnotation(UNCHECKED))
            .beginControlFlow("if ($DIRTY_PROPERTY)")
            .addStatement("return %T($callArgs)", originalClassType)
            .endControlFlow()
            .addStatement("return $SOURCE_PROPERTY")
            .build()

        val toStringFun = FunSpec.builder("toString")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("val sb = %T()", StringBuilder::class)
            .apply {
                adapters
                    .filter { !it.sourceProp.ignore }//Don't generate for ignored fields
                    .forEach { adapter ->
                        beginControlFlow("if (${adapter.isDirtyExpression})")
                        addStatement("sb.append(\"\\n\", \"${adapter.sourceProp.name}:·${adapter.stringExpression}\")")
                        endControlFlow()
                    }
            }
            .addStatement("""return "{" + sb.toString().prependIndent("  ") + "\n}"""")
            .build()

        classBuilder.addFunction(toStringFun)
        classBuilder.addFunction(freezeFun)

    }

    private fun generateMutateExtension(fileBuilder: FileSpec.Builder) {
        val toMutableExtension = FunSpec.builder("toMutable")
            .returns(mutableClassType)
            .receiver(originalClassType)
            .addParameter(
                ParameterSpec.builder(
                    "parent",
                    nullableParentType
                )
                    .defaultValue("null")
                    .build()
            )
            .addStatement("return %T(this, parent)", mutableClassType)
            .build()

        val produceExtension = FunSpec.builder("produce")
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
            .returns(mutableClassType)
            .receiver(originalClassType)
            .addStatement("val mutable = this.${toMutableExtension.name}()")
            .addStatement("mutable.block()")
            .addStatement("return mutable")
            .build()

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
            .returns(originalClassType)
            .receiver(originalClassType)
            .addStatement("return this.${produceExtension.name}(block).freeze()")
            .build()

        fileBuilder.addFunction(produceExtension)
        fileBuilder.addFunction(mutateExtension)
        fileBuilder.addFunction(toMutableExtension)
    }

    private fun generateDiffAdapter() {
        val diffAdapterClassName = DiffAdapter::class.asClassName()

        //Import function
        fileBuilder.addImport(diffAdapterClassName.packageName, "diffObjects")

        val body = CodeBlock.builder()
            .addStatement("val sb = %T()", StringBuilder::class)
            .apply {
                properties.forEach { prop ->
                    addStatement(
                        "diffObjects(a.${prop.name}, b.${prop.name}," +
                                " indent)?.let { sb.append(\"\\n\", \"${prop.name}: \",it) }"
                    )
                }
            }
            .addStatement("return \"{\${sb.toString().prependIndent(indent)}\\n}\"")
            .build()

        val diffFun = FunSpec.builder("diff")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("a", originalClassType)
            .addParameter("b", originalClassType)
            .addParameter("indent", STRING)
            .returns(STRING.copy(nullable = true))
            .addCode(body)
            .build()

        val diffClass = TypeSpec.classBuilder(
            originalClassType.simpleName +
                    DiffAdapter.GENERATED_CLASS_SUFFIX
        ).addSuperinterface(diffAdapterClassName.parameterizedBy(originalClassType))
            .addFunction(diffFun)
            .build()

        fileBuilder.addType(diffClass)
    }

}


