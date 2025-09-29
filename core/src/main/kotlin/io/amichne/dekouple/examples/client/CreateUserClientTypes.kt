package io.amichne.dekouple.examples.client

import io.amichne.dekouple.layers.client.ClientRequest
import io.amichne.dekouple.layers.client.ClientResponse

data class CreateUserClientRequest(
    val name: String,
    val email: String,
    val age: Int
) : ClientRequest

data class CreateUserClientResponse(
    val userId: String,
    val message: String
) : ClientResponse
