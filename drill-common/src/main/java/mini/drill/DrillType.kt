package mini.drill

@Suppress("PropertyName")
interface DrillType<T> {

    /** @hide */
    var _ref: T

    /** @hide */
    var _dirty: Boolean

    /** @hide */
    val _parent: DrillType<*>?

    /**
     * Update the underlying value with a new reference that will be kept when freezing.
     */
    fun set(value: T) {
        _dirty = false
        _ref = value
    }

    /**
     * Rebuild immutable object.
     */
    fun freeze(): T

    /**
     * Mark object as dirty recreating the object when frozen.
     */
    fun markDirty() {
        if (!_dirty) {
            _dirty = true
            _parent?.markDirty()
        }
    }
}