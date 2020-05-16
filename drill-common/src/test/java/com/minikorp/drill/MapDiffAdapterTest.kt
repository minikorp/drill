package com.minikorp.drill

import org.junit.jupiter.api.Test

internal class MapDiffAdapterTest {

    @Test
    fun `added keys are diffed`() {
        val adapter = MapDiffAdapter<Map<String, String>>()
        println(adapter.diff(mapOf(), mapOf("a" to "b")))
    }

    @Test
    fun `modified keys are diffed`() {
        val adapter = MapDiffAdapter<Map<String, String>>()
        println(adapter.diff(mapOf("a" to "a"), mapOf("a" to "b")))
    }

    @Test
    fun `removed keys are diffed`() {
        val adapter = MapDiffAdapter<Map<String, String>>()
        println(adapter.diff(mapOf("a" to "a"), mapOf()))
    }
}