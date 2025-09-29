package io.amichne.dekouple.transport.http

import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either

/** Abstract HTTP client interface to decouple from OkHttp. */
interface HttpClient {
    suspend fun <Req, Res> execute(
        endpoint: Endpoint,
        request: Req,
        responseType: Class<Res>
    ): Either<Failure, Res>
}
