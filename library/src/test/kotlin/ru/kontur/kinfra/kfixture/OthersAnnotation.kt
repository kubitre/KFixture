package ru.kontur.kinfra.kfixture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import ru.kontur.kinfra.kfixture.api.JavaxFixture
import ru.kontur.kinfra.kfixture.resolver.FixtureParameterResolver

@ExtendWith(FixtureParameterResolver::class)
class OthersAnnotation {

    annotation class Custom

    data class Data(
        @field:Custom
        val param: String
    )

    @Test
    fun testWithCustomAnnotation(
        @JavaxFixture data: Data
    ) {
        assertNotNull(data)
        assertNotNull(data.param)
        assertEquals(data.param::class, String::class)
    }
}