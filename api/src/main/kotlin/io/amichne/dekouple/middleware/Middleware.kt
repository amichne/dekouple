package io.amichne.dekouple.middleware

/**
 * Middleware function that can transform or inspect messages at various
 * stages.
 */
typealias Middleware<T> = suspend (T, suspend (T) -> T) -> T
