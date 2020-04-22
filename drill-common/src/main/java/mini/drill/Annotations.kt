package mini.drill

import java.lang.annotation.Inherited


@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class Drill


/** @hide */
object UNSET_VALUE