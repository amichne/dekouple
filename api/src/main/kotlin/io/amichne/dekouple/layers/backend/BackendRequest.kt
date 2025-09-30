package io.amichne.dekouple.layers.backend

/** Backend-facing layer - mirrors external backend API contract. */
interface BackendRequest<H : Host<*, *>> {
    val host: H
}
