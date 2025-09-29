package io.amichne.dekouple.operation

import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either
import io.amichne.dekouple.core.types.Host
import io.amichne.dekouple.layers.backend.BackendRequest
import io.amichne.dekouple.layers.backend.BackendResponse

/** Backend service caller for external API interactions. */
interface BackendCaller<H : Host, Req : BackendRequest<H>, Res : BackendResponse> {
    suspend fun call(request: Req): Either<Failure, Res>
}
