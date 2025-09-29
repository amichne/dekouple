package io.amichne.dekouple.examples.handlers

import io.amichne.dekouple.core.conversion.MessageConverter
import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either
import io.amichne.dekouple.examples.backend.CreateUserBackendRequest
import io.amichne.dekouple.examples.backend.CreateUserBackendResponse
import io.amichne.dekouple.examples.domain.CreateUserCommand
import io.amichne.dekouple.examples.domain.UserCreatedResult
import io.amichne.dekouple.examples.hosts.UserServiceHost
import io.amichne.dekouple.middleware.ExecutionContext
import io.amichne.dekouple.operation.BackendCaller
import io.amichne.dekouple.operation.OperationHandler

class CreateUserHandler(
    private val backendCaller: BackendCaller<UserServiceHost, CreateUserBackendRequest, CreateUserBackendResponse>,
    private val commandToBackend: MessageConverter<CreateUserCommand, CreateUserBackendRequest>,
    private val backendToDomain: MessageConverter<CreateUserBackendResponse, UserCreatedResult>
) : OperationHandler<CreateUserCommand, UserCreatedResult> {

    override suspend fun handle(
        command: CreateUserCommand,
        context: ExecutionContext
    ): Either<Failure, UserCreatedResult> {
        // Convert command to backend request
        val backendRequest = commandToBackend.convert(command)
            .fold({ return Either.Left(it) }, { it })

        // Call backend
        val backendResponse = backendCaller.call(backendRequest)
            .fold({ return Either.Left(it) }, { it })

        // Convert backend response to domain result
        return backendToDomain.convert(backendResponse)
    }
}
