package io.amichne.dekouple.operation

import io.amichne.dekouple.core.types.Host
import io.amichne.dekouple.layers.backend.BackendRequest
import io.amichne.dekouple.layers.backend.BackendResponse
import kotlin.reflect.KClass

/**
 * Registry for managing BackendCaller instances by their type signatures.
 */
class BackendCallerRegistry {
    private val callers: MutableMap<KClass<out Host<*, *>>, BackendCaller<*, *, *>> = mutableMapOf()

    /**
     * Register a BackendCaller for a specific Host type.
     */
    fun <H : Host<Req, Res>, Req : BackendRequest<H>, Res : BackendResponse> register(
        hostClass: KClass<H>,
        caller: BackendCaller<H, Req, Res>
    ) {
        callers[hostClass as KClass<out Host<*, *>>] = caller
    }

    /**
     * Get a BackendCaller for a specific Host type.
     */
    @Suppress("UNCHECKED_CAST")
    fun <H : Host<Req, Res>, Req : BackendRequest<H>, Res : BackendResponse> get(
        hostClass: KClass<H>
    ): BackendCaller<H, Req, Res>? {
        return callers[hostClass as KClass<out Host<*, *>>] as? BackendCaller<H, Req, Res>
    }

    /**
     * Inline convenience method for registering with reified types.
     */
    inline fun <reified H : Host<Req, Res>, reified Req : BackendRequest<H>, reified Res : BackendResponse> register(
        caller: BackendCaller<H, Req, Res>
    ) {
        register(H::class, caller)
    }

    /**
     * Inline convenience method for getting with reified types.
     */
    inline fun <reified H : Host<Req, Res>, reified Req : BackendRequest<H>, reified Res : BackendResponse> get(): BackendCaller<H, Req, Res>? {
        return get(H::class)
    }
}
