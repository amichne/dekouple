package io.amichne.dekouple.core.failure

/** Categorized failure types for precise error handling. */
sealed class Failure {
    data class MappingFailure(
        val message: String,
        val source: Any? = null
    ) : Failure()

    data class DomainFailure(
        val code: String,
        val message: String
    ) : Failure()

    data class BackendFailure(
        val statusCode: Int,
        val message: String,
        val body: String? = null
    ) : Failure()

    data class TransportFailure(
        val message: String,
        val cause: Throwable? = null
    ) : Failure()

    data class ValidationFailure(
        val field: String,
        val message: String
    ) : Failure()
}
