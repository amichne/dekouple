package io.amichne.dekouple.layers.backend

import io.amichne.dekouple.core.types.Host

/** Backend-facing layer - mirrors external backend API contract. */
interface BackendRequest<H : Host> {
    val host: H
}
