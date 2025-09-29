package io.amichne.dekouple.dsl

import io.amichne.dekouple.core.conversion.MessageConverter
import io.amichne.dekouple.core.types.OpId
import io.amichne.dekouple.layers.client.ClientRequest
import io.amichne.dekouple.layers.client.ClientResponse
import io.amichne.dekouple.layers.domain.Command
import io.amichne.dekouple.layers.domain.DomainResult
import io.amichne.dekouple.operation.OperationHandler
import io.amichne.dekouple.operation.OperationSpec

/** DSL for building operation specifications. */
@DekoupleDsl
class OperationBuilder<
    CReq : ClientRequest,
    CRes : ClientResponse,
    Cmd : Command,
    DRes : DomainResult
    >(val id: OpId) {

    lateinit var clientToCommand: MessageConverter<CReq, Cmd>
    lateinit var handler: OperationHandler<Cmd, DRes>
    lateinit var resultToClient: MessageConverter<DRes, CRes>

    fun build(): OperationSpec<CReq, CRes, Cmd, DRes> {
        return OperationSpec(id, clientToCommand, handler, resultToClient)
    }
}

//inline fun <reified CReq : ClientRequest, reified CRes : ClientResponse,
//    reified Cmd : Command, reified DRes : DomainResult>
//    operation(
//    id: OpId,
//    crossinline block: OperationBuilder<CReq, CRes, Cmd, DRes>.() -> Unit
//): OperationSpec<CReq, CRes, Cmd, DRes> {
//    return OperationBuilder<CReq, CRes, Cmd, DRes>(id).apply(block).build()
//}

@DekoupleDsl
class BlindOperationBuilder<CReq : ClientRequest, CRes : ClientResponse,
    Cmd : Command, DRes : DomainResult>(val id: OpId) {
    @PublishedApi internal lateinit var clientToCommand: MessageConverter<CReq, Cmd>
    @PublishedApi internal lateinit var handler: OperationHandler<Cmd, DRes>
    @PublishedApi internal lateinit var resultToClient: MessageConverter<DRes, CRes>


    @Suppress("UNCHECKED_CAST")
    inline fun <reified CCReq : CReq, reified CCmd : Cmd> clientToCommand(
        converter: () -> MessageConverter<CCReq, CCmd>
    ) {
        clientToCommand = converter() as MessageConverter<CReq, Cmd>
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified CCmd : Cmd, reified CDRes : DRes> handler(
        operationHandler: () -> OperationHandler<CCmd, CDRes>
    ) {
        handler = operationHandler() as OperationHandler<Cmd, DRes>
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified CDRes : DRes, reified CCRes : CRes> resultToClient(
        converter: () -> MessageConverter<CDRes, CCRes>
    ) {
        resultToClient = converter() as MessageConverter<DRes, CRes>
    }

    @PublishedApi
    internal fun build(): OperationSpec<CReq, CRes, Cmd, DRes> {
        return OperationSpec(id, clientToCommand, handler, resultToClient)
    }

}

inline fun <reified CReq : ClientRequest, reified CRes : ClientResponse,
    reified Cmd : Command, reified DRes : DomainResult>
    operation(
    id: OpId,
    block: BlindOperationBuilder<CReq, CRes, Cmd, DRes>.() -> Unit
): OperationSpec<CReq, CRes, Cmd, DRes> {
    return BlindOperationBuilder<CReq, CRes, Cmd, DRes>(id).apply(block).build()
}
