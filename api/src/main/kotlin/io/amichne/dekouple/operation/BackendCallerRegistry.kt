package io.amichne.dekouple.operation

import io.amichne.dekouple.core.types.Host
import io.amichne.dekouple.layers.backend.BackendRequest
import io.amichne.dekouple.layers.backend.BackendResponse
import kotlin.reflect.KClass

/**
 * Registry for managing BackendCaller instances by their type signatures.
 */
class BackendCallerRegistry {
    private val callers: MutableMap<Triple<KClass<out Host>, KClass<out BackendRequest<*>>, KClass<out BackendResponse>>, BackendCaller<*, *, *>> = mutableMapOf()

    /**
     * Register a BackendCaller for specific Host, Request, and Response types.
     */
    fun <H : Host, Req : BackendRequest<H>, Res : BackendResponse> register(
        hostClass: KClass<H>,
        requestClass: KClass<Req>,
        responseClass: KClass<Res>,
        caller: BackendCaller<H, Req, Res>
    ) {
        val key = Triple(hostClass as KClass<out Host>, requestClass as KClass<out BackendRequest<*>>, responseClass as KClass<out BackendResponse>)
        callers[key] = caller
    }

    /**
     * Get a BackendCaller for specific Host, Request, and Response types.
     */
    @Suppress("UNCHECKED_CAST")
    fun <H : Host, Req : BackendRequest<H>, Res : BackendResponse> get(
        hostClass: KClass<H>,
        requestClass: KClass<Req>,
        responseClass: KClass<Res>
    ): BackendCaller<H, Req, Res>? {
        val key = Triple(hostClass as KClass<out Host>, requestClass as KClass<out BackendRequest<*>>, responseClass as KClass<out BackendResponse>)
        return callers[key] as? BackendCaller<H, Req, Res>
    }

    /**
     * Inline convenience method for registering with reified types.
     */
    inline fun <reified H : Host, reified Req : BackendRequest<H>, reified Res : BackendResponse> register(
        caller: BackendCaller<H, Req, Res>
    ) {
        register(H::class, Req::class, Res::class, caller)
    }

    /**
     * Inline convenience method for getting with reified types.
     */
    inline fun <reified H : Host, reified Req : BackendRequest<H>, reified Res : BackendResponse> get(): BackendCaller<H, Req, Res>? {
        return get(H::class, Req::class, Res::class)
    }
}
