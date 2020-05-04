package com.minikorp.drill

import java.lang.annotation.Inherited


@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class Drill


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class DrillProperty(
    /**
     * Whether this field should appear in the generated class
     */
    val ignore: Boolean = false,

    /**
     * Whether this field should be treated as a plain reference
     * even if it could be converted into the corresponding mutable type
     */
    val asReference: Boolean = false
)

