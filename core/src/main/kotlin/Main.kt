package io.amichne

import io.amichne.dekouple.examples.main

/**
 * Legacy Main.kt - now delegates to the modularized architecture.
 * 
 * The original monolithic implementation has been refactored into proper packages:
 * - io.amichne.dekouple.core.* - Core types and foundations
 * - io.amichne.dekouple.layers.* - Layer definitions (client/domain/backend)  
 * - io.amichne.dekouple.transport.* - Transport abstraction and implementations
 * - io.amichne.dekouple.middleware.* - Middleware system
 * - io.amichne.dekouple.operation.* - Operation handling and registry
 * - io.amichne.dekouple.dsl.* - DSL builders
 * - io.amichne.dekouple.examples.* - Example implementations
 */
suspend fun main() {
    // Delegate to the modularized example application
    main()
}
