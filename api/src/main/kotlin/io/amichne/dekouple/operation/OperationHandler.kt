package io.amichne.dekouple.operation

import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either
import io.amichne.dekouple.layers.domain.Command
import io.amichne.dekouple.layers.domain.DomainResult
import io.amichne.dekouple.middleware.ExecutionContext

/** Business logic handler for a domain command. */
interface OperationHandler<C : Command, R : DomainResult> {
    suspend fun handle(
        command: C,
        context: ExecutionContext,
        backendCallerRegistry: BackendCallerRegistry
    ): Either<Failure, R>
}
