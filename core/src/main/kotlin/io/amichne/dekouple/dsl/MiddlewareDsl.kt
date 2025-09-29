package io.amichne.dekouple.dsl

import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either
import io.amichne.dekouple.middleware.Middleware

/** DSL for creating common middleware patterns. */
object MiddlewareDsl {

    fun <T> logging(
        prefix: String = "Middleware",
        logRequest: Boolean = true,
        logResponse: Boolean = true
    ): Middleware<T> = { request, next ->
        if (logRequest) println("[$prefix] Processing request: $request")
        val result = next(request)
        if (logResponse) println("[$prefix] Processed result: $result")
        result
    }

    fun metrics(name: String = "Operation"): Middleware<Either<Failure, *>> = { result, next ->
        val startTime = System.currentTimeMillis()
        val processedResult = next(result)
        val duration = System.currentTimeMillis() - startTime
        println("[$name] took ${duration}ms")
        processedResult
    }

    fun <T> errorHandler(
        onError: (Throwable) -> T
    ): Middleware<T> = { request, next ->
        try {
            next(request)
        } catch (e: Throwable) {
            onError(e)
        }
    }

    fun <T> conditional(
        predicate: (T) -> Boolean,
        middleware: Middleware<T>
    ): Middleware<T> = { request, next ->
        if (predicate(request)) {
            middleware(request, next)
        } else {
            next(request)
        }
    }

    fun <T> transform(
        transformation: (T) -> T
    ): Middleware<T> = { request, next ->
        next(transformation(request))
    }
}
