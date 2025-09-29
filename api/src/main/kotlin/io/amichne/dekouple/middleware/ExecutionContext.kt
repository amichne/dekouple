package io.amichne.dekouple.middleware

import io.amichne.dekouple.core.types.OpId
import java.util.UUID

/** Middleware context containing request lifecycle metadata. */
data class ExecutionContext(
    val opId: OpId,
    val correlationId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: MutableMap<String, Any> = mutableMapOf()
)
