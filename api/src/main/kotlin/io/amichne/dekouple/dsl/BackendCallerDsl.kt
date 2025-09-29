package io.amichne.dekouple.dsl

import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either
import io.amichne.dekouple.core.types.Host
import io.amichne.dekouple.layers.backend.BackendRequest
import io.amichne.dekouple.layers.backend.BackendResponse
import io.amichne.dekouple.operation.BackendCaller
import io.amichne.dekouple.transport.http.Endpoint
import io.amichne.dekouple.transport.http.HttpClient

/** DSL for building backend callers. */
@DekoupleDsl
class BackendCallerBuilder<H : Host<Req, Res>, Req : BackendRequest<H>, Res : BackendResponse> {
    lateinit var host: H
    lateinit var endpoint: Endpoint
    lateinit var httpClient: HttpClient

    inline fun <reified T : Res> build(): BackendCaller<H, Req, Res> {
        return object : BackendCaller<H, Req, Res> {
            override suspend fun call(request: Req): Either<Failure, Res> {
                val fullPath = "${host.baseUrl}${endpoint.path}"
                val completeEndpoint = endpoint.copy(path = fullPath)
                return httpClient.execute(completeEndpoint, request, T::class.java)
            }
        }
    }
}

inline fun <reified H : Host<Req, Res>, reified Req : BackendRequest<H>, reified Res : BackendResponse>
    backendInvoker(block: BackendCallerBuilder<H, Req, Res>.() -> Unit):
    BackendCaller<H, Req, Res> {
    return BackendCallerBuilder<H, Req, Res>().apply(block).build<Res>()
}
