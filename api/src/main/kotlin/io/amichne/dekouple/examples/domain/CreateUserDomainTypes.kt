package io.amichne.dekouple.examples.domain

import io.amichne.dekouple.layers.domain.Command
import io.amichne.dekouple.layers.domain.DomainResult

data class CreateUserCommand(
    val name: String,
    val email: String,
    val age: Int,
    val validated: Boolean = false
) : Command

data class UserCreatedResult(
    val userId: String,
    val name: String,
    val email: String,
    val createdAt: Long
) : DomainResult
