package io.amichne.dekouple.examples.backend

import io.amichne.dekouple.examples.hosts.UserServiceHost
import io.amichne.dekouple.layers.backend.BackendRequest
import io.amichne.dekouple.layers.backend.BackendResponse

data class CreateUserBackendRequest(
    val fullName: String,
    val emailAddress: String,
    val ageInYears: Int,
    override val host: UserServiceHost = UserServiceHost.default
) : BackendRequest<UserServiceHost>

data class CreateUserBackendResponse(
    val id: String,
    val timestamp: Long
) : BackendResponse
