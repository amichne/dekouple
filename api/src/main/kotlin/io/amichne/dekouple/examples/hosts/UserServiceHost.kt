package io.amichne.dekouple.examples.hosts

import io.amichne.dekouple.core.types.Host
import io.amichne.dekouple.examples.backend.CreateUserBackendRequest
import io.amichne.dekouple.examples.backend.CreateUserBackendResponse

@JvmInline
value class UserServiceHost(override val baseUrl: String) : Host<CreateUserBackendRequest, CreateUserBackendResponse> {
    companion object {
        val default = UserServiceHost("https://cat-fact.herokuapp.com")
    }
}

object PaymentServiceHost : Host<CreateUserBackendRequest, CreateUserBackendResponse> {
    override val baseUrl = "https://api.payments.example.com"
}
