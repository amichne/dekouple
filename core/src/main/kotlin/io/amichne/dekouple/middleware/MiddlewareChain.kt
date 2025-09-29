package io.amichne.dekouple.middleware

/** Composable middleware chain. */
class MiddlewareChain<T> {
    private val middlewares = mutableListOf<Middleware<T>>()

    fun use(middleware: Middleware<T>): MiddlewareChain<T> {
        middlewares.add(middleware)
        return this
    }

    suspend fun execute(
        input: T,
        handler: suspend (T) -> T
    ): T {
        return middlewares.foldRight(handler) { middleware, next ->
            { value -> middleware(value, next) }
        }(input)
    }
}
