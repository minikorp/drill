package mini.drill.processor

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import mini.drill.Drill
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion

@KotlinPoetMetadataPreview
class Processor {
    val supportedSourceVersion: SourceVersion = SourceVersion.RELEASE_8
    val supportedAnnotationTypes: MutableSet<String> = mutableSetOf(Drill::class.java)
        .map { it.canonicalName }
        .toMutableSet()

    fun init(environment: ProcessingEnvironment) {
        env = environment
        typeUtils = env.typeUtils
        elementUtils = env.elementUtils
    }

    fun process(roundEnvironment: RoundEnvironment): Boolean {
        try {

            val drillTypes = roundEnvironment
                .getElementsAnnotatedWith(Drill::class.java)
                .filter { it.isClass }
            if (drillTypes.isEmpty()) return false

            val mutableClasses = drillTypes.map { MutableClassModel(it.asTypeElement()) }
            mutableClasses.forEach { it.generate() }

        } catch (e: Throwable) {
            val file = FileSpec.builder("mini", "DrillGenDebug")
            file.addComment(e.stackTraceString())
            file.addType(TypeSpec.classBuilder("DrillGenDebug").build())
            file.build().writeToFile()
        }

        return true
    }
}