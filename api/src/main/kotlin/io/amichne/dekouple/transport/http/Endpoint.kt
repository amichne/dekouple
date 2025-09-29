package io.amichne.dekouple.transport.http

/** Typed HTTP endpoint specification. */
data class Endpoint(
    val method: HttpMethod,
    val path: String,
    val headers: Map<String, String> = emptyMap()
)
