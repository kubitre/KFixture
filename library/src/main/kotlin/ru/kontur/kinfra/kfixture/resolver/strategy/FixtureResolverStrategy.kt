package ru.kontur.kinfra.kfixture.resolver.strategy

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import ru.kontur.kinfra.kfixture.exceptions.FixtureGenerationException
import ru.kontur.kinfra.kfixture.processor.impl.FixtureProcessor
import ru.kontur.kinfra.kfixture.resolver.ResolverStrategy
import ru.kontur.kinfra.kfixture.utils.toKType

class FixtureResolverStrategy(
    private val fixtureProcessor: FixtureProcessor
) : ResolverStrategy {

    override fun resolve(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        val type = parameterContext.parameter.type
        val kType = parameterContext.parameter.parameterizedType
        return fixtureProcessor.generateParam(type.kotlin, kType.toKType(), null)
            ?: throw FixtureGenerationException(kType.typeName, parameterContext.parameter.name)
    }
}