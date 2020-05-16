package com.minikorp.drill

fun Any?.toQuotedString(): String {
    return if (this is String) return "\"$this\""
    else this.toString()
}