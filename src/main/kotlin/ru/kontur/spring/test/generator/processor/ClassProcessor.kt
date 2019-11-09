package ru.kontur.spring.test.generator.processor

import ru.kontur.spring.test.generator.api.ValidationConstructor
import ru.kontur.spring.test.generator.api.ValidationParamResolver
import ru.kontur.spring.test.generator.exceptions.NoOptionalRecursiveException
import ru.kontur.spring.test.generator.exceptions.NoSuchValidAnnotationException
import ru.kontur.spring.test.generator.utils.*
import javax.validation.Constraint
import javax.validation.constraints.*
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * @author Konstantin Volivach
 */
class ClassProcessor(
    private val generators: Map<KClass<out Annotation>, ValidationParamResolver>,
    private val constructors: Map<KClass<*>, ValidationConstructor<*>>
) {

    private val defaultPriority: Map<KClass<out Annotation>, Long> = mapOf(
        AssertFalse::class to 0L,
        AssertTrue::class to 0L,
        DecimalMax::class to 0L,
        DecimalMin::class to 0L,
        Digits::class to 0L,
        Email::class to 0L,
        Future::class to 0L,
        FutureOrPresent::class to 0L,
        Max::class to 0L,
        Min::class to 0L,
        Negative::class to 0L,
        NegativeOrZero::class to 0L,
        NotBlank::class to -10L,
        NotEmpty::class to -10L,
        NotNull::class to -10L,
        Null::class to -10L,
        Past::class to 0L,
        PastOrPresent::class to 0L,
        Pattern::class to 0L,
        Positive::class to 0L,
        PositiveOrZero::class to 0L,
        Size::class to 0L
    )

    fun generateParam(clazz: KClass<*>, type: KType, annotation: List<Annotation>?): Any? {
        val annotationSum = (annotation?.let { it + clazz.annotations }
            ?: clazz.annotations).filter { it.annotationClass.annotations.any { annotation -> annotation is Constraint } }
        return when {
            clazz.isSimple() -> processSimpleType(clazz, type, annotationSum)
            clazz.java.isEnum -> {
                val x = Random.nextInt(clazz.java.enumConstants.size)
                clazz.java.enumConstants[x]
            }
            else -> {
                when {
                    constructors.containsKey(clazz) -> constructors[clazz]?.call()
                    !annotationSum.isNullOrEmpty() -> {
                        var result: Any? = null
                        for (it in annotationSum) {
                            val generator = generators[it.annotationClass]
                            result = if (generator != null) {
                                generator.process(result, clazz, type, it)
                            } else {
                                createClazz(clazz) //Else create by default generators
                            }
                        }
                        return result
                    }
                    else -> {
                        createClazz(clazz)
                    }
                }
            }
        }
    }

    private fun createClazz(clazz: KClass<*>): Any {
        val constructor = clazz.constructors.toMutableList()[0]
        val arguments = constructor.parameters.map { param ->
            val newAnnotation =
                clazz.java.declaredFields.firstOrNull { it.name == param.name }?.annotations?.toList()
            val paramClazz = param.type.classifier as KClass<*>
            if (paramClazz == clazz) {
                if (param.isOptional) {
                    return@map null
                } else {
                    throw NoOptionalRecursiveException("Recursive field can't be required field.name=${param.name} clazz=${paramClazz}")
                }
            }
            generateParam(param.type.classifier as KClass<*>, param.type, newAnnotation)
        }.toTypedArray()
        return constructor.call(*arguments)
    }

    private fun processSimpleType(clazz: KClass<*>, type: KType, annotationList: List<Annotation>?): Any? {
        return when {
            annotationList != null && annotationList.any {
                generators.keys.contains(it.annotationClass)
            } -> {
                val sorted = annotationList.sortedBy {
                    return@sortedBy defaultPriority[it.annotationClass]
                }
                var generatedParam: Any? = null
                for (annotation in sorted) {
                    val generator = generators[annotation.annotationClass]
                        ?: throw NoSuchValidAnnotationException("Please annotate your validate annotation with ValidateAnnotation class")
                    generatedParam = generator.process(generatedParam, clazz, type, annotation)
                }
                generatedParam
            }
            else -> {
                this.generatePrimitiveValue(clazz, type, annotationList)
            }
        }
    }

    private fun KClass<*>.isSimple(): Boolean {
        return this == Int::class || this == Long::class || this == String::class || this == Boolean::class || this == List::class || this == Map::class
    }

    private fun generatePrimitiveValue(kclass: KClass<*>, type: KType?, annotationList: List<Annotation>?): Any? {
        return when (kclass) {
            Double::class -> {
                Random.nextDouble()
            }
            Int::class -> {
                Random.nextInt()
            }
            Float::class -> {
                Random.nextFloat()
            }
            Char::class -> {
                generateRandomChar()
            }
            String::class -> {
                generateString(Random.nextInt(100))
            }
            List::class -> {
                generateCollection(10, kclass, type!!, annotationList)
            }
            Map::class -> {
                generateMap(10, kclass, type!!, annotationList)
            }
            else -> null
        }
    }

    private fun generateMap(
        numOfElements: Int,
        classRef: KClass<*>,
        type: KType,
        annotationList: List<Annotation>?
    ): Map<Any, Any> {
        val keyType = type.arguments[0].type!!
        val valueType = type.arguments[1].type!!
        val keys =
            (1..numOfElements).mapNotNull { generateParam(keyType.classifier as KClass<*>, keyType, annotationList) }
        val values =
            (1..numOfElements).mapNotNull {
                generateParam(
                    valueType.classifier as KClass<*>,
                    valueType,
                    annotationList
                )
            }
        return keys.zip(values).toMap()
    }

    private fun generateCollection(
        numOfElements: Int,
        classRef: KClass<*>,
        type: KType,
        annotationList: List<Annotation>?
    ): Any {
        val elemType = type.arguments[0].type!!
        return (1..numOfElements).map { generateParam(elemType.classifier as KClass<*>, elemType, annotationList) }
    }
}