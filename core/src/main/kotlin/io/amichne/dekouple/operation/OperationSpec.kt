package io.amichne.dekouple.operation

import io.amichne.dekouple.core.conversion.MessageConverter
import io.amichne.dekouple.core.types.OpId
import io.amichne.dekouple.layers.client.ClientRequest
import io.amichne.dekouple.layers.client.ClientResponse
import io.amichne.dekouple.layers.domain.Command
import io.amichne.dekouple.layers.domain.DomainResult

/** Complete operation specification binding all layers together. */
data class OperationSpec<
    CReq : ClientRequest,
    CRes : ClientResponse,
    Cmd : Command,
    DRes : DomainResult
    >(
    val id: OpId,
    val clientToCommand: MessageConverter<out CReq, out Cmd>,
    val handler: OperationHandler<out Cmd, out DRes>,
    val resultToClient: MessageConverter<out DRes, out CRes>
) {
    companion object {
        fun <CReq : ClientRequest, CRes : ClientResponse, Cmd : Command, DRes : DomainResult>
            create(
                id: OpId,
                clientToCommand: MessageConverter<out CReq, out Cmd>,
                handler: OperationHandler<out Cmd, out DRes>,
                resultToClient: MessageConverter<out DRes, out CRes>
            ): OperationSpec<CReq, CRes, Cmd, DRes> {
            return OperationSpec(id, clientToCommand, handler, resultToClient)
        }
    }
}
