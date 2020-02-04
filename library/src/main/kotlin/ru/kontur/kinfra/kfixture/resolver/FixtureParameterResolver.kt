package ru.kontur.kinfra.kfixture.resolver

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import ru.kontur.kinfra.kfixture.annotations.Fixture
import ru.kontur.kinfra.kfixture.annotations.JavaxFixture
import ru.kontur.kinfra.kfixture.api.FixtureGeneratorMeta
import ru.kontur.kinfra.kfixture.processor.GeneratorAnnotationScanner
import ru.kontur.kinfra.kfixture.resolver.strategy.FixtureResolverStrategy
import ru.kontur.kinfra.kfixture.resolver.strategy.JavaxFixtureResolverStrategy
import ru.kontur.kinfra.kfixture.scanner.CachedReflections

/**
 * @author Konstantin Volivach
 */
class FixtureParameterResolver : ParameterResolver {
    private val cachedReflections: CachedReflections = CachedReflections()

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.annotations.filterIsInstance<Fixture>().isNotEmpty() ||
                parameterContext.parameter.annotations.filterIsInstance<JavaxFixture>().isNotEmpty()
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        val meta = extensionContext.testInstance.get()::class.annotations.firstOrNull {
            it is FixtureGeneratorMeta
        } as? FixtureGeneratorMeta

        val annotationScanner = GeneratorAnnotationScanner(
            cachedReflections.getReflections(
                paths = meta?.pathes?.toList() ?: listOf(),
                extensionContext = extensionContext
            )
        )

        val fixture = parameterContext.parameter.annotations.filterIsInstance<Fixture>()
        val javaxFixture = parameterContext.parameter.annotations.filterIsInstance<JavaxFixture>()

        return when {
            fixture.isNotEmpty() -> {
                val fixtureStrategy = FixtureResolverStrategy(
                    annotationScanner
                )
                fixtureStrategy.resolve(parameterContext, extensionContext)
            }
            javaxFixture.isNotEmpty() -> {
                val javaxStrategy = JavaxFixtureResolverStrategy(
                    annotationScanner
                )
                javaxStrategy.resolve(parameterContext, extensionContext)
            }
            else -> {
                throw IllegalArgumentException("Class wasn't annotated, name=${parameterContext.parameter.name}")
            }
        }
    }
}