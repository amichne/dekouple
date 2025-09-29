package io.amichne.dekouple.core.conversion

import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either

/** Type-safe message converter between layers. */
interface MessageConverter<From, To> {
    fun convert(from: From): Either<Failure, To>
}
