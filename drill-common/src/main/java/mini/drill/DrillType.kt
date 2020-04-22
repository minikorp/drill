package mini.drill

interface DrillType<T> {

    /** Used by generated code, do not modify */
    var refDrill: T

    /** Used by generated code, do not modify */
    var dirtyDrill: Boolean

    /** Used by generated code, do not modify */
    val parentDrill: DrillType<*>?

    /**
     * Update the underlying value with a new reference that will be kept when freezing.
     */
    fun set(value: T) {
        dirtyDrill = false
        refDrill = value
    }

    /**
     * Rebuild immutable object.
     */
    fun freeze(): T

    /**
     * Mark object as dirty recreating the object when frozen.
     */
    fun markDirty() {
        if (!dirtyDrill) {
            dirtyDrill = true
            parentDrill?.markDirty()
        }
    }
}

