package io.amichne.dekouple.operation

import io.amichne.dekouple.core.types.OpId
import io.amichne.dekouple.layers.client.ClientRequest
import io.amichne.dekouple.layers.client.ClientResponse
import io.amichne.dekouple.layers.domain.Command
import io.amichne.dekouple.layers.domain.DomainResult

/** Registry for all operations in the system. */
class OperationRegistry {
    private val operations = mutableMapOf<OpId, OperationSpec<*, *, *, *>>()

    fun <CReq : ClientRequest, CRes : ClientResponse, Cmd : Command, DRes : DomainResult>
        register(spec: OperationSpec<CReq, CRes, Cmd, DRes>) {
        operations[spec.id] = spec
    }

    @Suppress("UNCHECKED_CAST")
    fun <CReq : ClientRequest, CRes : ClientResponse, Cmd : Command, DRes : DomainResult>
        get(id: OpId): OperationSpec<CReq, CRes, Cmd, DRes>? {
        return operations[id] as? OperationSpec<CReq, CRes, Cmd, DRes>
    }
}
