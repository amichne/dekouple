package io.amichne.dekouple.core.types

import io.amichne.dekouple.layers.backend.BackendRequest
import io.amichne.dekouple.layers.backend.BackendResponse

/** Host interface that binds backend request/response types to the host. */
interface Host<Req : BackendRequest<*>, Res : BackendResponse> {
    val baseUrl: String
}
