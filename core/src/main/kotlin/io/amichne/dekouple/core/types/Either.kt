package io.amichne.dekouple.core.types

/**
 * Monadic error handling container. Forces explicit handling of both
 * success and failure paths.
 */
sealed class Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>()
    data class Right<out R>(val value: R) : Either<Nothing, R>()

    inline fun <T> fold(
        ifLeft: (L) -> T,
        ifRight: (R) -> T
    ): T = when (this) {
        is Left -> ifLeft(value)
        is Right -> ifRight(value)
    }

    inline fun <T> map(transform: (R) -> T): Either<L, T> = when (this) {
        is Left -> this
        is Right -> Right(transform(value))
    }

    inline fun <T> flatMap(transform: (R) -> Either<@UnsafeVariance L, T>): Either<L, T> = when (this) {
        is Left -> this
        is Right -> transform(value)
    }

    companion object {
        fun <R> success(value: R): Either<Nothing, R> = Right(value)
        fun <L> failure(value: L): Either<L, Nothing> = Left(value)
    }
}
