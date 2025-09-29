package io.amichne.dekouple.transport.serialization

import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either

/** JSON serialization abstraction. */
interface JsonSerializer {
    fun <T> serialize(value: T): Either<Failure, String>
    fun <T> deserialize(
        json: String,
        type: Class<T>
    ): Either<Failure, T>
}
