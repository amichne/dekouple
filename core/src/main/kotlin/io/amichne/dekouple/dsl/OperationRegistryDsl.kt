package io.amichne.dekouple.dsl

import io.amichne.dekouple.operation.OperationSpec

@DekoupleDsl
class OperationRegistryDsl {
    private val registryList: MutableList<() -> OperationSpec<*, *, *, *>> = mutableListOf()
}
