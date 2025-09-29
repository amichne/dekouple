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

inline fun <reified CReq : ClientRequest, reified CRes : ClientResponse,
    reified Cmd : Command, reified DRes : DomainResult>
    operation(
    id: OpId,
    crossinline block: OperationBuilder<CReq, CRes, Cmd, DRes>.() -> Unit
): OperationSpec<CReq, CRes, Cmd, DRes> {
    return OperationBuilder<CReq, CRes, Cmd, DRes>(id).apply(block).build()
}
