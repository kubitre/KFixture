package ru.kontur.kinfra.kfixture.scanner

import org.junit.jupiter.api.extension.ExtensionContext
import ru.kontur.kinfra.kfixture.model.CollectionSettings
import ru.kontur.kinfra.kfixture.processor.impl.FixtureProcessor
import ru.kontur.kinfra.kfixture.processor.scanner.GeneratorAnnotationScanner

class CachedScanner {
    fun getFixtureProcessor(
        paths: List<String>,
        settings: CollectionSettings,
        extensionContext: ExtensionContext
    ): FixtureProcessor {
        val stored = extensionContext.getStore(ExtensionContext.Namespace.GLOBAL)
            .get(FIXTURE_KEY, FixtureWrapper::class.java)

        if (stored != null) {
            return if (stored.paths.containsAll(paths)) {
                stored.fixtureProcessor
            } else {
                val wrapper = FixtureWrapper(
                    settings,
                    paths + listOf(
                        LIBRARY_PATH,
                        JAVAX_PATH
                    )
                )
                extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).put(FIXTURE_KEY, wrapper)
                wrapper.fixtureProcessor
            }
        } else {
            return if (extensionContext.parent.isPresent) {
                getFixtureProcessor(paths, settings, extensionContext.parent.get())
            } else {
                val wrapper = FixtureWrapper(
                    settings,
                    paths + listOf(
                        LIBRARY_PATH,
                        JAVAX_PATH
                    )
                )
                extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).put(FIXTURE_KEY, wrapper)
                wrapper.fixtureProcessor
            }
        }
    }

    fun getScanner(paths: List<String>, extensionContext: ExtensionContext): GeneratorAnnotationScanner {
        val stored = extensionContext.getStore(ExtensionContext.Namespace.GLOBAL)
            .get(DEFAULT_REFLECTION_KEY, ReflectionsWrapper::class.java)

        if (stored != null) {
            return if (stored.pathes.containsAll(paths)) {
                stored.cachedScanner
            } else {
                val wrapper = ReflectionsWrapper(
                    paths + listOf(
                        LIBRARY_PATH,
                        JAVAX_PATH
                    )
                )
                extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).put(DEFAULT_REFLECTION_KEY, wrapper)
                wrapper.cachedScanner
            }
        } else {
            return if (extensionContext.parent.isPresent) {
                getScanner(paths, extensionContext.parent.get())
            } else {
                val wrapper = ReflectionsWrapper(
                    paths + listOf(
                        LIBRARY_PATH,
                        JAVAX_PATH
                    )
                )
                extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).put(DEFAULT_REFLECTION_KEY, wrapper)
                wrapper.cachedScanner
            }
        }
    }

    private companion object {
        const val FIXTURE_KEY = "FIXTURE_REFERENCE"
        const val DEFAULT_REFLECTION_KEY = "REFLECTIONS_REFERENCES"
        const val LIBRARY_PATH = "ru.kontur.kinfra.kfixture"
        const val JAVAX_PATH = "javax.validation.constraints"
    }
}