package io.amichne.dekouple.middleware

import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either

/**
 * Central middleware manager that defines middleware for all hosts.
 * Middleware is no longer exposed in a mutable way through the DSL.
 */
class MiddlewareManager {
    private val inboundMiddlewares = mutableListOf<Middleware<Any>>()
    private val executionMiddlewares = mutableListOf<Middleware<Either<Failure, *>>>()
    private val outboundMiddlewares = mutableListOf<Middleware<Any>>()

    /**
     * Configure inbound middleware centrally for all hosts
     */
    fun configureInbound(vararg middlewares: Middleware<Any>) {
        inboundMiddlewares.addAll(middlewares)
    }

    /**
     * Configure execution middleware centrally for all hosts
     */
    fun configureExecution(vararg middlewares: Middleware<Either<Failure, *>>) {
        executionMiddlewares.addAll(middlewares)
    }

    /**
     * Configure outbound middleware centrally for all hosts
     */
    fun configureOutbound(vararg middlewares: Middleware<Any>) {
        outboundMiddlewares.addAll(middlewares)
    }

    /**
     * Get all configured inbound middlewares
     */
    internal fun getInboundMiddlewares(): List<Middleware<Any>> = inboundMiddlewares.toList()

    /**
     * Get all configured execution middlewares
     */
    internal fun getExecutionMiddlewares(): List<Middleware<Either<Failure, *>>> = executionMiddlewares.toList()

    /**
     * Get all configured outbound middlewares
     */
    internal fun getOutboundMiddlewares(): List<Middleware<Any>> = outboundMiddlewares.toList()

    companion object {
        /**
         * Default middleware manager instance
         */
        val default = MiddlewareManager()
    }
}