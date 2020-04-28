package mini.drill.processor

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import mini.drill.Drill
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment

@KotlinPoetMetadataPreview
class Processor {
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
            if (e !is ProcessorException) {
                logError(
                    "Drill compiler crashed, open an issue please!\n" +
                            " ${e.stackTraceString()}"
                )
            }
        }
        return true
    }
}