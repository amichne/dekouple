package io.amichne.dekouple.examples

import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.amichne.dekouple.core.types.Either
import io.amichne.dekouple.core.types.OpId
import io.amichne.dekouple.dsl.executionEngine
import io.amichne.dekouple.dsl.okHttpClient
import io.amichne.dekouple.examples.client.CreateUserClientRequest
import io.amichne.dekouple.examples.client.CreateUserClientResponse
import io.amichne.dekouple.examples.converters.BackendResponseToDomainResultConverter
import io.amichne.dekouple.examples.converters.ClientToCreateUserCommandConverter
import io.amichne.dekouple.examples.converters.CommandToBackendRequestConverter
import io.amichne.dekouple.examples.converters.DomainResultToClientResponseConverter
import io.amichne.dekouple.examples.handlers.CreateUserHandler
import io.amichne.dekouple.examples.hosts.UserServiceHost
import io.amichne.dekouple.layers.client.ClientRequest
import io.amichne.dekouple.layers.client.ClientResponse
import io.amichne.dekouple.dsl.MiddlewareDsl
import io.amichne.dekouple.dsl.MiddlewareDsl.errorHandling
import io.amichne.dekouple.dsl.MiddlewareDsl.responseFormatting
import io.amichne.dekouple.dsl.MiddlewareDsl.validation
import io.amichne.dekouple.examples.backend.CreateUserBackendRequest
import io.amichne.dekouple.examples.backend.CreateUserBackendResponse
import io.amichne.dekouple.middleware.MiddlewareManager
import io.amichne.dekouple.operation.ExecutionEngine
import io.amichne.dekouple.transport.http.Endpoint
import io.amichne.dekouple.transport.http.HttpMethod

/** Example application showcasing the modularized architecture. */
class Application {
    init {
        // Configure central middleware manager (not exposed mutably through DSL)
        MiddlewareManager.default.apply {
            configureInbound(
                MiddlewareDsl.logging("Inbound"),
                MiddlewareDsl.validation()
            )
            configureExecution(
                MiddlewareDsl.metrics("CreateUser"),
                MiddlewareDsl.errorHandling()
            )
            configureOutbound(
                MiddlewareDsl.logging("Outbound"),
                MiddlewareDsl.responseFormatting()
            )
        }
    }
    private val httpClient = okHttpClient {
        moshi {
            factory { KotlinJsonAdapterFactory() }
        }
        okHttp {
            addInterceptor { chain ->
                val request = chain.request()
                println("Making request to: ${request.url}")
                chain.proceed(request)
            }
        }
    }

    private val executionEngine: ExecutionEngine = executionEngine {
        conversions {
            register(ClientToCreateUserCommandConverter())
            register(CommandToBackendRequestConverter())
            register(BackendResponseToDomainResultConverter())
            register(DomainResultToClientResponseConverter())
        }

        // Configure hosts with multiple endpoints/paths - host instance is now a parameter
        host(UserServiceHost.default) {
            httpClient { this@Application.httpClient }

            // Multiple endpoints for the same host
            endpoint(HttpMethod.POST, "/facts") {
                // Endpoint-specific configuration
            }

            endpoint(HttpMethod.GET, "/users/{id}") {
                // Another endpoint for the same host
            }

            endpoint(HttpMethod.PUT, "/users/{id}/profile") {
                // Yet another endpoint for user profile updates
            }
        }

        operations {
            operation(OpId("create-user")) {
                clientToCommand {
                    ClientToCreateUserCommandConverter()
                }
                handler {
                    CreateUserHandler(
                        CommandToBackendRequestConverter(),
                        BackendResponseToDomainResultConverter()
                    )
                }
                resultToClient { DomainResultToClientResponseConverter() }
            }
        }
    }

    suspend fun handleRequest(
        opId: OpId,
        request: ClientRequest
    ): Either<io.amichne.dekouple.core.failure.Failure, ClientResponse> {
        return when (opId.value) {
            "create-user" -> {
                executionEngine.execute<CreateUserClientRequest, CreateUserClientResponse>(
                    opId,
                    request as CreateUserClientRequest
                )
            }

            else -> Either.Left(
                io.amichne.dekouple.core.failure.Failure.MappingFailure("Unknown operation: ${opId.value}")
            )
        }
    }
}

/** Entry point demonstration using the modularized architecture. */
suspend fun main() {
    val app = Application()

    val request = CreateUserClientRequest(
        name = "John Doe",
        email = "john.doe@example.com",
        age = 25
    )

    val result = app.handleRequest(OpId("create-user"), request)

    result.fold(
        ifLeft = { failure ->
            println("Operation failed: $failure")
        },
        ifRight = { response ->
            println("Operation succeeded: $response")
        }
    )
}
