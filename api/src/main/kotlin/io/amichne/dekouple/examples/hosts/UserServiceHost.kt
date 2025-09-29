package io.amichne.dekouple.examples.hosts

import io.amichne.dekouple.core.types.Host

@JvmInline
value class UserServiceHost(override val baseUrl: String) : Host {
    companion object {
        val default = UserServiceHost("https://cat-fact.herokuapp.com")
    }
}

object PaymentServiceHost : Host {
    override val baseUrl = "https://api.payments.example.com"
}
