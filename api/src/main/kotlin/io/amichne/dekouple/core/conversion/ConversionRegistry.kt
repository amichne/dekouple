package io.amichne.dekouple.core.conversion

import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either

/** Registry for all available message conversions. */
class ConversionRegistry {
    @PublishedApi
    internal val converters: MutableMap<Pair<Class<*>, Class<*>>, MessageConverter<*, *>> = mutableMapOf()

    inline fun <reified From, reified To> register(converter: MessageConverter<From, To>) {
        converters[From::class.java to To::class.java] = converter
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified From, reified To> get(): MessageConverter<From, To>? =
        converters[From::class.java to To::class.java] as? MessageConverter<From, To>

    inline fun <reified From, reified To> convert(from: From): Either<Failure, To> =
        get<From, To>()?.convert(from) ?: Either.Left(
            Failure.MappingFailure("No converter registered for ${From::class.simpleName} -> ${To::class.simpleName}")
        )
}
