package com.minikorp.drill

internal fun Any?.toQuotedString(): String {
    return if (this is String) return "\"$this\""
    else this.toString()
}