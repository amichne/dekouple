package io.amichne.dekouple.dsl

import io.amichne.dekouple.core.conversion.ConversionRegistry
import io.amichne.dekouple.core.types.Host
import io.amichne.dekouple.layers.backend.BackendRequest
import io.amichne.dekouple.layers.backend.BackendResponse
import io.amichne.dekouple.operation.BackendCaller
import io.amichne.dekouple.operation.ExecutionEngine
import io.amichne.dekouple.operation.OperationRegistry
import io.amichne.dekouple.transport.http.Endpoint
import kotlin.reflect.KClass

/** DSL for building execution engines with host-scoped configuration. */

@DekoupleDsl
class ExecutionEngineBuilder {
    private val operationRegistry = OperationRegistry()
    private val conversionRegistry = ConversionRegistry()
    private val hostConfigurations = mutableMapOf<KClass<out Host<*, *>>, HostConfigurationBuilder<*, *, *>>()

    fun operations(block: OperationRegistryBuilder.() -> Unit) {
        OperationRegistryBuilder().apply(block).apply {
            build().forEach { this@ExecutionEngineBuilder.operationRegistry.register(it) }
        }
    }

    fun conversions(block: ConversionRegistry.() -> Unit) {
        conversionRegistry.apply(block)
    }

    /**
     * Configure a host with multiple endpoints/paths
     */
    fun <H : Host<Req, Res>, Req : BackendRequest<H>, Res : BackendResponse>
        host(hostClass: KClass<H>, hostInstance: H, block: HostConfigurationBuilder<H, Req, Res>.() -> Unit) {
        val builder = HostConfigurationBuilder<H, Req, Res>(hostInstance)
        builder.apply(block)
        hostConfigurations[hostClass as KClass<out Host<*, *>>] = builder
    }

    /**
     * Configure a host with multiple endpoints/paths using reified types
     */
    inline fun <reified H : Host<Req, Res>, reified Req : BackendRequest<H>, reified Res : BackendResponse>
        host(hostInstance: H, noinline block: HostConfigurationBuilder<H, Req, Res>.() -> Unit) {
        host(H::class, hostInstance, block)
    }

    fun build(): ExecutionEngine {
        val engine = ExecutionEngine(operationRegistry, conversionRegistry)

        // Register all host configurations and their endpoints
        hostConfigurations.forEach { (hostClass, configBuilder) ->
            configBuilder.registerWith(engine, hostClass)
        }

        return engine
    }
}

/**
 * Host configuration builder that supports multiple endpoints/paths
 */
@DekoupleDsl
class HostConfigurationBuilder<H : Host<Req, Res>, Req : BackendRequest<H>, Res : BackendResponse>(
    private val hostInstance: H
) {
    private lateinit var httpClientProvider: () -> io.amichne.dekouple.transport.http.HttpClient
    private val endpointBuilders = mutableListOf<EndpointBuilder<H, Req, Res>>()

    /**
     * Set the HTTP client provider for this host
     */
    fun httpClient(provider: () -> io.amichne.dekouple.transport.http.HttpClient) {
        this.httpClientProvider = provider
    }

    /**
     * Add an endpoint/path to this host
     */
    fun endpoint(endpoint: Endpoint, block: EndpointBuilder<H, Req, Res>.() -> Unit = {}) {
        val builder = EndpointBuilder<H, Req, Res>(endpoint)
        builder.apply(block)
        endpointBuilders.add(builder)
    }

    /**
     * Add an endpoint/path using HTTP method and path
     */
    fun endpoint(
        method: io.amichne.dekouple.transport.http.HttpMethod,
        path: String,
        block: EndpointBuilder<H, Req, Res>.() -> Unit = {}
    ) {
        endpoint(Endpoint(method, path), block)
    }

    @Suppress("UNCHECKED_CAST")
    internal fun registerWith(engine: ExecutionEngine, hostClass: KClass<out Host<*, *>>) {
        endpointBuilders.forEach { endpointBuilder ->
            @Suppress("UNCHECKED_CAST")
            val caller = endpointBuilder.buildCaller(hostInstance, httpClientProvider) as BackendCaller<*, *, *>
            engine.backendCallerRegistry.register(hostClass as KClass<H>, caller as BackendCaller<H, Req, Res>)
        }
    }
}

/**
 * Endpoint builder for configuring individual endpoints
 */
@DekoupleDsl
class EndpointBuilder<H : Host<Req, Res>, Req : BackendRequest<H>, Res : BackendResponse>(
    private val endpoint: Endpoint
) {
    private var customCaller: BackendCaller<H, Req, Res>? = null

    /**
     * Set a custom backend caller for this endpoint
     */
    fun caller(caller: BackendCaller<H, Req, Res>) {
        this.customCaller = caller
    }

    /**
     * Build a backend caller for this endpoint
     */
    @Suppress("UNCHECKED_CAST")
    fun buildCaller(
        hostInstance: H,
        httpClientProvider: () -> io.amichne.dekouple.transport.http.HttpClient
    ): BackendCaller<H, Req, Res> {
        return customCaller ?: object : BackendCaller<H, Req, Res> {
            override suspend fun call(request: Req): io.amichne.dekouple.core.types.Either<io.amichne.dekouple.core.failure.Failure, Res> {
                val fullPath = "${hostInstance.baseUrl}${endpoint.path}"
                val completeEndpoint = endpoint.copy(path = fullPath)
                return httpClientProvider().execute(completeEndpoint, request, Any::class.java) as io.amichne.dekouple.core.types.Either<io.amichne.dekouple.core.failure.Failure, Res>
            }
        }
    }
}

fun executionEngine(block: ExecutionEngineBuilder.() -> Unit): ExecutionEngine =
    ExecutionEngineBuilder().apply(block).build()