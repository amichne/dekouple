package io.amichne.dekouple.operation

import io.amichne.dekouple.core.conversion.ConversionRegistry
import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either
import io.amichne.dekouple.core.types.OpId
import io.amichne.dekouple.layers.client.ClientRequest
import io.amichne.dekouple.layers.client.ClientResponse
import io.amichne.dekouple.layers.domain.Command
import io.amichne.dekouple.layers.domain.DomainResult
import io.amichne.dekouple.middleware.ExecutionContext
import io.amichne.dekouple.middleware.Middleware
import io.amichne.dekouple.middleware.MiddlewareChain

/**
 * Main execution engine that orchestrates request processing through all
 * layers.
 */
class ExecutionEngine(
    @PublishedApi internal val operationRegistry: OperationRegistry,
    @PublishedApi internal val conversionRegistry: ConversionRegistry
) {
    @PublishedApi
    internal val inboundMiddleware = MiddlewareChain<Any>()

    @PublishedApi
    internal val executionMiddleware = MiddlewareChain<Either<Failure, *>>()

    @PublishedApi
    internal val outboundMiddleware = MiddlewareChain<Any>()

    suspend inline fun <reified CReq : ClientRequest, reified CRes : ClientResponse>
        execute(
        opId: OpId,
        request: CReq
    ): Either<Failure, CRes> {

        val context = ExecutionContext(opId)

        // Get operation specification
        val spec = operationRegistry.get<CReq, CRes, Command, DomainResult>(opId)
                   ?: return Either.Left(Failure.MappingFailure("Operation not found: ${opId.value}"))

        // Inbound: client request -> domain command
        val processedRequest = inboundMiddleware.execute(request) { it }

        @Suppress("UNCHECKED_CAST")
        val commandResult = spec.clientToCommand.convert(processedRequest as CReq)
        val command = when (commandResult) {
            is Either.Left -> return commandResult
            is Either.Right -> commandResult.value
        }

        // Execution: handle domain command
        @Suppress("UNCHECKED_CAST")
        val handlerResult = executionMiddleware.execute(
            spec.handler.handle(command, context)
        ) { it } as Either<Failure, DomainResult>

        val domainResult = when (handlerResult) {
            is Either.Left -> return handlerResult
            is Either.Right -> handlerResult.value
        }

        // Outbound: domain result -> client response
        val responseResult = spec.resultToClient.convert(domainResult)

        return when (responseResult) {
            is Either.Left -> responseResult
            is Either.Right -> {
                @Suppress("UNCHECKED_CAST")
                val processedResponse = outboundMiddleware.execute(responseResult.value) { it } as CRes
                Either.Right(processedResponse)
            }
        }
    }

    fun useInbound(middleware: Middleware<Any>) = inboundMiddleware.use(middleware)
    fun useExecution(middleware: Middleware<Either<Failure, *>>) = executionMiddleware.use(middleware)
    fun useOutbound(middleware: Middleware<Any>) = outboundMiddleware.use(middleware)
}
