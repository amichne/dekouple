package io.amichne.dekouple.dsl

import io.amichne.dekouple.core.conversion.MessageConverter
import io.amichne.dekouple.core.types.OpId
import io.amichne.dekouple.layers.client.ClientRequest
import io.amichne.dekouple.layers.client.ClientResponse
import io.amichne.dekouple.layers.domain.Command
import io.amichne.dekouple.layers.domain.DomainResult
import io.amichne.dekouple.operation.OperationHandler
import io.amichne.dekouple.operation.OperationSpec

@DekoupleDsl
class OperationRegistryBuilder {
    @PublishedApi internal val registryList: MutableList<() -> OperationSpec<*, *, *, *>> = mutableListOf()

    fun registerOperation(
        id: OpId,
        clientToCommand: MessageConverter<ClientRequest, Command>,
        handler: OperationHandler<Command, DomainResult>,
        resultToClient: MessageConverter<DomainResult, ClientResponse>
    ) {
        registryList.add { OperationSpec.create(id, clientToCommand, handler, resultToClient) }
    }

    inline fun <reified CReq : ClientRequest, reified CRes : ClientResponse,
        reified Cmd : Command, reified DRes : DomainResult>
        operation(
        id: OpId,
        noinline block: BlindOperationBuilder<CReq, CRes, Cmd, DRes>.() -> Unit
    ) {
        val spec = io.amichne.dekouple.dsl.operation<CReq, CRes, Cmd, DRes>(id, block)
        registryList.add { spec }
    }

    fun build(): List<OperationSpec<*, *, *, *>> {
        return registryList.map { it() }
    }
}
