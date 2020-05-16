package com.minikorp.drill

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class ListDiffAdapterTest {

    @Test
    fun `add item to list from empty`() {
        val adapter = ListDiffAdapter<List<String>>()
        val result = adapter.diff(listOf(), listOf("added"))
        expectThat(result).isEqualTo("[] -> [added]")
    }

    @Test
    fun `add item to list from non empty`() {
        val adapter = ListDiffAdapter<List<String>>()
        val result = adapter.diff(listOf("original"), listOf("original", "added"))
        expectThat(result).isEqualTo("[] -> [added]")
    }
}