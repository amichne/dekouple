package io.amichne.dekouple.dsl

import io.amichne.dekouple.core.conversion.ConversionRegistry
import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either
import io.amichne.dekouple.core.types.Host
import io.amichne.dekouple.layers.backend.BackendRequest
import io.amichne.dekouple.layers.backend.BackendResponse
import io.amichne.dekouple.middleware.Middleware
import io.amichne.dekouple.operation.ExecutionEngine
import io.amichne.dekouple.operation.OperationRegistry

/** DSL for building execution engines with middleware configuration. */

@DekoupleDsl
class ExecutionEngineBuilder {
    private val operationRegistry = OperationRegistry()
    private val conversionRegistry = ConversionRegistry()
    private val inboundMiddlewares = mutableListOf<Middleware<Any>>()
    private val executionMiddlewares = mutableListOf<Middleware<Either<Failure, *>>>()
    private val outboundMiddlewares = mutableListOf<Middleware<Any>>()

    @PublishedApi
    internal val backendCallerRegistrations = mutableListOf<(ExecutionEngine) -> Unit>()

    fun operations(block: OperationRegistryBuilder.() -> Unit) {
        OperationRegistryBuilder().apply(block).apply {
            build().forEach { this@ExecutionEngineBuilder.operationRegistry.register(it) }
        }
    }

    fun conversions(block: ConversionRegistry.() -> Unit) {
        conversionRegistry.apply(block)
    }

    fun inbound(block: InboundMiddlewareScope.() -> Unit) {
        InboundMiddlewareScope().apply(block)
    }

    fun execution(block: ExecutionMiddlewareScope.() -> Unit) {
        ExecutionMiddlewareScope().apply(block)
    }

    fun outbound(block: OutboundMiddlewareScope.() -> Unit) {
        OutboundMiddlewareScope().apply(block)
    }

    @DekoupleDsl
    inner class InboundMiddlewareScope {
        fun install(middleware: Middleware<Any>) {
            this@ExecutionEngineBuilder.inboundMiddlewares.add(middleware)
        }

        fun install(vararg middlewares: Middleware<Any>) {
            this@ExecutionEngineBuilder.inboundMiddlewares.addAll(middlewares)
        }

        fun logging(
            prefix: String = "Inbound",
            logRequest: Boolean = true,
            logResponse: Boolean = true
        ) {
            install(MiddlewareDsl.logging<Any>(prefix, logRequest, logResponse))
        }

        fun validation() {
            install(MiddlewareDsl.validation<Any>())
        }

        fun errorHandler(onError: (Throwable) -> Any) {
            install(MiddlewareDsl.errorHandler(onError))
        }

        fun conditional(
            predicate: (Any) -> Boolean,
            middleware: Middleware<Any>
        ) {
            install(MiddlewareDsl.conditional(predicate, middleware))
        }

        fun transform(transformation: (Any) -> Any) {
            install(MiddlewareDsl.transform(transformation))
        }
    }

    @DekoupleDsl
    inner class ExecutionMiddlewareScope {
        fun install(middleware: Middleware<Either<Failure, *>>) {
            this@ExecutionEngineBuilder.executionMiddlewares.add(middleware)
        }

        fun install(vararg middlewares: Middleware<Either<Failure, *>>) {
            this@ExecutionEngineBuilder.executionMiddlewares.addAll(middlewares)
        }

        fun metrics(name: String = "Operation") {
            install(MiddlewareDsl.metrics(name))
        }

        fun errorHandling() {
            install(MiddlewareDsl.errorHandling())
        }

        fun logging(
            prefix: String = "Execution",
            logRequest: Boolean = true,
            logResponse: Boolean = true
        ) {
            install(MiddlewareDsl.logging<Either<Failure, *>>(prefix, logRequest, logResponse))
        }

        fun conditional(
            predicate: (Either<Failure, *>) -> Boolean,
            middleware: Middleware<Either<Failure, *>>
        ) {
            install(MiddlewareDsl.conditional(predicate, middleware))
        }
    }

    @DekoupleDsl
    inner class OutboundMiddlewareScope {
        fun install(middleware: Middleware<Any>) {
            this@ExecutionEngineBuilder.outboundMiddlewares.add(middleware)
        }

        fun install(vararg middlewares: Middleware<Any>) {
            this@ExecutionEngineBuilder.outboundMiddlewares.addAll(middlewares)
        }

        fun logging(
            prefix: String = "Outbound",
            logRequest: Boolean = true,
            logResponse: Boolean = true
        ) {
            install(MiddlewareDsl.logging<Any>(prefix, logRequest, logResponse))
        }

        fun responseFormatting() {
            install(MiddlewareDsl.responseFormatting<Any>())
        }

        fun errorHandler(onError: (Throwable) -> Any) {
            install(MiddlewareDsl.errorHandler(onError))
        }

        fun conditional(
            predicate: (Any) -> Boolean,
            middleware: Middleware<Any>
        ) {
            install(MiddlewareDsl.conditional(predicate, middleware))
        }

        fun transform(transformation: (Any) -> Any) {
            install(MiddlewareDsl.transform(transformation))
        }

        inline fun <reified T : Middleware<Any>> install(
            middleware: T,
            noinline configure: T.() -> Unit = {}
        ) {
            middleware.configure()
            install(middleware)
        }
    }

    inline fun <reified H : Host<Req, Res>, reified Req : BackendRequest<H>, reified Res : BackendResponse>
        backendCaller(noinline block: BackendCallerBuilder<H, Req, Res>.() -> Unit) {
        val caller = BackendCallerBuilder<H, Req, Res>().apply(block).build<Res>()
        backendCallerRegistrations.add { engine ->
            engine.backendCallerRegistry.register(H::class, caller)
        }
    }

    fun build(): ExecutionEngine {
        return ExecutionEngine(operationRegistry, conversionRegistry).apply {
            inboundMiddlewares.forEach { useInbound(it) }
            executionMiddlewares.forEach { useExecution(it) }
            outboundMiddlewares.forEach { useOutbound(it) }
            backendCallerRegistrations.forEach { it(this) }
        }
    }
}

fun executionEngine(block: ExecutionEngineBuilder.() -> Unit): ExecutionEngine =
    ExecutionEngineBuilder().apply(block).build()
