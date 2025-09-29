package io.amichne.dekouple.examples.converters

import io.amichne.dekouple.core.conversion.MessageConverter
import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either
import io.amichne.dekouple.examples.backend.CreateUserBackendRequest
import io.amichne.dekouple.examples.backend.CreateUserBackendResponse
import io.amichne.dekouple.examples.client.CreateUserClientRequest
import io.amichne.dekouple.examples.client.CreateUserClientResponse
import io.amichne.dekouple.examples.domain.CreateUserCommand
import io.amichne.dekouple.examples.domain.UserCreatedResult

class ClientToCreateUserCommandConverter : MessageConverter<CreateUserClientRequest, CreateUserCommand> {
    override fun convert(from: CreateUserClientRequest): Either<Failure, CreateUserCommand> {
        // Validation
        if (from.age < 18) {
            return Either.Left(Failure.ValidationFailure("age", "Must be 18 or older"))
        }
        if (!from.email.contains("@")) {
            return Either.Left(Failure.ValidationFailure("email", "Invalid email format"))
        }

        return Either.Right(
            CreateUserCommand(
                name = from.name,
                email = from.email,
                age = from.age,
                validated = true
            )
        )
    }
}

class CommandToBackendRequestConverter : MessageConverter<CreateUserCommand, CreateUserBackendRequest> {
    override fun convert(from: CreateUserCommand): Either<Failure, CreateUserBackendRequest> {
        return Either.Right(
            CreateUserBackendRequest(
                fullName = from.name,
                emailAddress = from.email,
                ageInYears = from.age
            )
        )
    }
}

class BackendResponseToDomainResultConverter : MessageConverter<CreateUserBackendResponse, UserCreatedResult> {
    override fun convert(from: CreateUserBackendResponse): Either<Failure, UserCreatedResult> {
        // This would normally extract the name and email from somewhere
        // For demo purposes, using placeholder values
        return Either.Right(
            UserCreatedResult(
                userId = from.id,
                name = "Retrieved Name",
                email = "retrieved@email.com",
                createdAt = from.timestamp
            )
        )
    }
}

class DomainResultToClientResponseConverter : MessageConverter<UserCreatedResult, CreateUserClientResponse> {
    override fun convert(from: UserCreatedResult): Either<Failure, CreateUserClientResponse> {
        return Either.Right(
            CreateUserClientResponse(
                userId = from.userId,
                message = "User ${from.name} created successfully"
            )
        )
    }
}
