package io.amichne.dekouple.dsl

import io.amichne.dekouple.core.conversion.ConversionRegistry
import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either
import io.amichne.dekouple.core.types.Host
import io.amichne.dekouple.layers.backend.BackendRequest
import io.amichne.dekouple.layers.backend.BackendResponse
import io.amichne.dekouple.middleware.Middleware
import io.amichne.dekouple.operation.BackendCaller
import io.amichne.dekouple.operation.ExecutionEngine
import io.amichne.dekouple.operation.OperationRegistry
import kotlin.reflect.KClass

/** DSL for building execution engines with middleware configuration. */

@DekoupleDsl
class ExecutionEngineBuilder {
    private val operationRegistry = OperationRegistry()
    private val conversionRegistry = ConversionRegistry()
    private val inboundMiddlewares = mutableListOf<Middleware<Any>>()
    private val executionMiddlewares = mutableListOf<Middleware<Either<Failure, *>>>()
    private val outboundMiddlewares = mutableListOf<Middleware<Any>>()
    private val backendCallerRegistrations = mutableListOf<(ExecutionEngine) -> Unit>()

    fun operations(block: OperationRegistryBuilder.() -> Unit) {
        OperationRegistryBuilder().apply(block).apply {
            build().forEach { this@ExecutionEngineBuilder.operationRegistry.register(it) }
        }
    }

    fun conversions(block: ConversionRegistry.() -> Unit) {
        conversionRegistry.apply(block)
    }

    fun inboundMiddleware(middleware: Middleware<Any>) {
        inboundMiddlewares.add(middleware)
    }

    fun executionMiddleware(middleware: Middleware<Either<Failure, *>>) {
        executionMiddlewares.add(middleware)
    }

    fun outboundMiddleware(middleware: Middleware<Any>) {
        outboundMiddlewares.add(middleware)
    }

    fun <H : Host, Req : BackendRequest<H>, Res : BackendResponse> backendCaller(
        hostClass: KClass<H>,
        requestClass: KClass<Req>,
        responseClass: KClass<Res>,
        block: BackendCallerBuilder<H, Req, Res>.() -> Unit
    ) {
        val caller = BackendCallerBuilder<H, Req, Res>().apply {
            block()
            responseType = responseClass.java
        }.build()
        backendCallerRegistrations.add { engine ->
            engine.backendCallerRegistry.register(hostClass, requestClass, responseClass, caller)
        }
    }

    inline fun <reified H : Host, reified Req : BackendRequest<H>, reified Res : BackendResponse>
    backendCaller(noinline block: BackendCallerBuilder<H, Req, Res>.() -> Unit) {
        backendCaller(H::class, Req::class, Res::class, block)
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
