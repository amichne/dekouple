package io.amichne

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

// ============================================================================
// Core Types and Foundations
// ============================================================================

/**
 * Monadic error handling container. Forces explicit handling of both
 * success and failure paths.
 */
sealed class Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>()
    data class Right<out R>(val value: R) : Either<Nothing, R>()

    inline fun <T> fold(
        ifLeft: (L) -> T,
        ifRight: (R) -> T
    ): T = when (this) {
        is Left -> ifLeft(value)
        is Right -> ifRight(value)
    }

    inline fun <T> map(transform: (R) -> T): Either<L, T> = when (this) {
        is Left -> this
        is Right -> Right(transform(value))
    }

    inline fun <T> flatMap(transform: (R) -> Either<@UnsafeVariance L, T>): Either<L, T> = when (this) {
        is Left -> this
        is Right -> transform(value)
    }

    companion object {
        fun <R> success(value: R): Either<Nothing, R> = Right(value)
        fun <L> failure(value: L): Either<L, Nothing> = Left(value)
    }
}

/** Categorized failure types for precise error handling. */
sealed class Failure {
    data class MappingFailure(
        val message: String,
        val source: Any? = null
    ) : Failure()

    data class DomainFailure(
        val code: String,
        val message: String
    ) : Failure()

    data class BackendFailure(
        val statusCode: Int,
        val message: String,
        val body: String? = null
    ) : Failure()

    data class TransportFailure(
        val message: String,
        val cause: Throwable? = null
    ) : Failure()

    data class ValidationFailure(
        val field: String,
        val message: String
    ) : Failure()
}

/**
 * Operation identifier that ties together all related artifacts at compile
 * time.
 */
@JvmInline
value class OpId(val value: String)

/** Host marker interface for backend service identification. */
interface Host {
    val baseUrl: String
}

// ============================================================================
// Layer Definitions
// ============================================================================

/** Client-facing layer - mirrors external client contract. */
interface ClientRequest
interface ClientResponse

/** Domain layer - pure business semantics. */
interface Command
interface DomainResult

/** Backend-facing layer - mirrors external backend API contract. */
interface BackendRequest<H : Host> {
    val host: H
}

interface BackendResponse

// ============================================================================
// Message Conversion System
// ============================================================================

/** Type-safe message converter between layers. */
interface MessageConverter<From, To> {
    fun convert(from: From): Either<Failure, To>
}

/** Registry for all available message conversions. */
class ConversionRegistry {
    @PublishedApi
    internal val converters: MutableMap<Pair<Class<*>, Class<*>>, MessageConverter<*, *>> = mutableMapOf()

    inline fun <reified From, reified To> register(converter: MessageConverter<From, To>) {
        converters[From::class.java to To::class.java] = converter
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified From, reified To> get(): MessageConverter<From, To>? =
        converters[From::class.java to To::class.java] as? MessageConverter<From, To>

    inline fun <reified From, reified To> convert(from: From): Either<Failure, To> =
        get<From, To>()?.convert(from) ?: Either.Left(
            Failure.MappingFailure("No converter registered for ${From::class.simpleName} -> ${To::class.simpleName}")
        )
}

// ============================================================================
// Middleware System
// ============================================================================

/**
 * Middleware function that can transform or inspect messages at various
 * stages.
 */
typealias Middleware<T> = suspend (T, suspend (T) -> T) -> T

/** Composable middleware chain. */
class MiddlewareChain<T> {
    private val middlewares = mutableListOf<Middleware<T>>()

    fun use(middleware: Middleware<T>): MiddlewareChain<T> {
        middlewares.add(middleware)
        return this
    }

    suspend fun execute(
        input: T,
        handler: suspend (T) -> T
    ): T {
        return middlewares.foldRight(handler) { middleware, next ->
            { value -> middleware(value, next) }
        }(input)
    }
}

/** Middleware context containing request lifecycle metadata. */
data class ExecutionContext(
    val opId: OpId,
    val correlationId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: MutableMap<String, Any> = mutableMapOf()
)

// ============================================================================
// Transport Abstraction
// ============================================================================

/** HTTP method enumeration. */
enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
}

/** Typed HTTP endpoint specification. */
data class Endpoint(
    val method: HttpMethod,
    val path: String,
    val headers: Map<String, String> = emptyMap()
)

/** Abstract HTTP client interface to decouple from OkHttp. */
interface HttpClient {
    suspend fun <Req, Res> execute(
        endpoint: Endpoint,
        request: Req,
        responseType: Class<Res>
    ): Either<Failure, Res>
}

/** OkHttp-based HTTP client implementation. */
class OkHttpClientAdapter internal constructor(
    private val client: OkHttpClient,
    private val serializer: JsonSerializer
) : HttpClient {
    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }

    override suspend fun <Req, Res> execute(
        endpoint: Endpoint,
        request: Req,
        responseType: Class<Res>
    ): Either<Failure, Res> {
        return try {
            val jsonBody = serializer.serialize(request)
                .fold({ return Either.Left(it) }, { it })

            val httpRequest = Request.Builder()
                .url(endpoint.path)
                .apply {
                    endpoint.headers.forEach { (key, value) ->
                        addHeader(key, value)
                    }

                    when (endpoint.method) {
                        HttpMethod.GET -> get()
                        HttpMethod.POST -> post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                        HttpMethod.PUT -> put(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                        HttpMethod.DELETE -> delete()
                        else -> throw UnsupportedOperationException("Method ${endpoint.method} not supported")
                    }
                }
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val rawBody = response.body.string()

                if (response.isSuccessful) {
                    serializer.deserialize(rawBody, responseType)
                } else {
                    Either.Left(
                        Failure.BackendFailure(
                            response.code,
                            response.message,
                            rawBody.ifBlank { null }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Either.Left(Failure.TransportFailure(e.message ?: "Unknown error", e))
        }
    }
}

/** JSON serialization abstraction. */
interface JsonSerializer {
    fun <T> serialize(value: T): Either<Failure, String>
    fun <T> deserialize(
        json: String,
        type: Class<T>
    ): Either<Failure, T>
}

/** Moshi-based JSON serializer implementation. */
class MoshiSerializer(private val moshi: Moshi) : JsonSerializer {

    override fun <T> serialize(value: T): Either<Failure, String> {
        return try {
            @Suppress("UNCHECKED_CAST")
            val adapter = moshi.adapter(value!!::class.java as Class<T>)
            Either.Right(adapter.toJson(value))
        } catch (e: Exception) {
            Either.Left(Failure.MappingFailure("Serialization failed: ${e.message}", value))
        }
    }

    override fun <T> deserialize(
        json: String,
        type: Class<T>
    ): Either<Failure, T> {
        return try {
            val adapter = moshi.adapter(type)
            val result = adapter.fromJson(json)
            if (result != null) {
                Either.Right(result)
            } else {
                Either.Left(Failure.MappingFailure("Deserialization returned null", json))
            }
        } catch (e: Exception) {
            Either.Left(Failure.MappingFailure("Deserialization failed: ${e.message}", json))
        }
    }
}

// ============================================================================
// Operation Handler and Registration
// ============================================================================

/** Business logic handler for a domain command. */
interface OperationHandler<C : Command, R : DomainResult> {
    suspend fun handle(
        command: C,
        context: ExecutionContext
    ): Either<Failure, R>
}

/** Backend service caller for external API interactions. */
interface BackendCaller<H : Host, Req : BackendRequest<H>, Res : BackendResponse> {
    suspend fun call(request: Req): Either<Failure, Res>
}

/** Complete operation specification binding all layers together. */
class OperationSpec<
    CReq : ClientRequest,
    CRes : ClientResponse,
    Cmd : Command,
    DRes : DomainResult
    >(
    val id: OpId,
    val clientToCommand: MessageConverter<CReq, Cmd>,
    val handler: OperationHandler<Cmd, DRes>,
    val resultToClient: MessageConverter<DRes, CRes>
)

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

// ============================================================================
// Execution Engine
// ============================================================================

/**
 * Main execution engine that orchestrates request processing through all
 * layers.
 */
class ExecutionEngine(
    @PublishedApi internal val operationRegistry: OperationRegistry,
    @PublishedApi internal val conversionRegistry: ConversionRegistry
) {
    @PublishedApi
    internal val inboundMiddleware = MiddlewareChain<Any>()

    @PublishedApi
    internal val executionMiddleware = MiddlewareChain<Either<Failure, *>>()

    @PublishedApi
    internal val outboundMiddleware = MiddlewareChain<Any>()

    suspend inline fun <reified CReq : ClientRequest, reified CRes : ClientResponse>
        execute(
        opId: OpId,
        request: CReq
    ): Either<Failure, CRes> {

        val context = ExecutionContext(opId)

        // Get operation specification
        val spec = operationRegistry.get<CReq, CRes, Command, DomainResult>(opId)
                   ?: return Either.Left(Failure.MappingFailure("Operation not found: ${opId.value}"))

        // Inbound: client request -> domain command
        val processedRequest = inboundMiddleware.execute(request) { it }

        @Suppress("UNCHECKED_CAST")
        val commandResult = spec.clientToCommand.convert(processedRequest as CReq)
        val command = when (commandResult) {
            is Either.Left -> return commandResult
            is Either.Right -> commandResult.value
        }

        // Execution: handle domain command
        @Suppress("UNCHECKED_CAST")
        val handlerResult = executionMiddleware.execute(
            spec.handler.handle(command, context)
        ) { it } as Either<Failure, DomainResult>

        val domainResult = when (handlerResult) {
            is Either.Left -> return handlerResult
            is Either.Right -> handlerResult.value
        }

        // Outbound: domain result -> client response
        val responseResult = spec.resultToClient.convert(domainResult)

        return when (responseResult) {
            is Either.Left -> responseResult
            is Either.Right -> {
                @Suppress("UNCHECKED_CAST")
                val processedResponse = outboundMiddleware.execute(responseResult.value) { it } as CRes
                Either.Right(processedResponse)
            }
        }
    }

    fun useInbound(middleware: Middleware<Any>) = inboundMiddleware.use(middleware)
    fun useExecution(middleware: Middleware<Either<Failure, *>>) = executionMiddleware.use(middleware)
    fun useOutbound(middleware: Middleware<Any>) = outboundMiddleware.use(middleware)
}

// ============================================================================
// DSL Builders for Fluent API
// ============================================================================

/** DSL for building operation specifications. */
class OperationBuilder<
    CReq : ClientRequest,
    CRes : ClientResponse,
    Cmd : Command,
    DRes : DomainResult
    >(val id: OpId) {

    lateinit var clientToCommand: MessageConverter<CReq, Cmd>
    lateinit var handler: OperationHandler<Cmd, DRes>
    lateinit var resultToClient: MessageConverter<DRes, CRes>

    fun build(): OperationSpec<CReq, CRes, Cmd, DRes> {
        return OperationSpec(id, clientToCommand, handler, resultToClient)
    }
}

inline fun <reified CReq : ClientRequest, reified CRes : ClientResponse,
    reified Cmd : Command, reified DRes : DomainResult>
    operation(
    id: OpId,
    block: OperationBuilder<CReq, CRes, Cmd, DRes>.() -> Unit
): OperationSpec<CReq, CRes, Cmd, DRes> {
    return OperationBuilder<CReq, CRes, Cmd, DRes>(id).apply(block).build()
}

class OkHttpClientAdapterBuilder internal constructor(
    private val okHttpClientBuilder: OkHttpClient.Builder = baseOkHttp.newBuilder(),
    private var jsonSerializer: JsonSerializer? = null
) {
    fun moshi(block: MoshiSerializerBuilder.() -> Unit) {
        require(jsonSerializer == null) { "JsonSerializer is already set to ${jsonSerializer!!}" }
        jsonSerializer = MoshiSerializerBuilder().apply(block).build()
    }

    fun okHttp(builder: OkHttpClient.Builder.() -> Unit) {
        okHttpClientBuilder.apply(builder)
    }

    internal fun build(): OkHttpClientAdapter =
        OkHttpClientAdapter(okHttpClientBuilder.build(), requireNotNull(jsonSerializer))

    companion object {
        private val baseOkHttp = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

fun okHttpClient(block: OkHttpClientAdapterBuilder.() -> Unit): OkHttpClientAdapter =
    OkHttpClientAdapterBuilder().apply(block).build()

class MoshiSerializerBuilder internal constructor(
    @PublishedApi
    internal val moshiBuilder: Moshi.Builder = defaultMoshiBuilder
) {
    companion object {
        private val defaultMoshiBuilder: Moshi.Builder = Moshi.Builder().addLast(KotlinJsonAdapterFactory())
    }

    inline fun <reified T : Any> adapter(block: () -> JsonAdapter<T>) {
        moshiBuilder.add<T>(T::class.java, block())
    }

    fun factory(block: () -> JsonAdapter.Factory) {
        moshiBuilder.add(block())
    }

    internal fun build(): MoshiSerializer = MoshiSerializer(moshiBuilder.build())
}

/** DSL for building backend callers. */
class BackendCallerBuilder<H : Host, Req : BackendRequest<H>, Res : BackendResponse> {
    lateinit var host: H
    lateinit var endpoint: Endpoint
    lateinit var httpClient: HttpClient
    lateinit var responseType: Class<Res>

    fun build(): BackendCaller<H, Req, Res> {
        return object : BackendCaller<H, Req, Res> {
            override suspend fun call(request: Req): Either<Failure, Res> {
                val fullPath = "${host.baseUrl}${endpoint.path}"
                val completeEndpoint = endpoint.copy(path = fullPath)
                return httpClient.execute(completeEndpoint, request, responseType)
            }
        }
    }
}

inline fun <reified H : Host, reified Req : BackendRequest<H>, reified Res : BackendResponse>
    backendCaller(block: BackendCallerBuilder<H, Req, Res>.() -> Unit):
    BackendCaller<H, Req, Res> {
    return BackendCallerBuilder<H, Req, Res>().apply {
        block()
        responseType = Res::class.java
    }.build()
}

// ============================================================================
// Example Usage
// ============================================================================

// Define hosts
@JvmInline
value class UserServiceHost(override val baseUrl: String) : Host {
    companion object {
        val default = UserServiceHost("https://cat-fact.herokuapp.com")
    }
}

object PaymentServiceHost : Host {
    override val baseUrl = "https://api.payments.example.com"
}

// Client layer types
data class CreateUserClientRequest(
    val name: String,
    val email: String,
    val age: Int
) : ClientRequest

data class CreateUserClientResponse(
    val userId: String,
    val message: String
) : ClientResponse

// Domain layer types
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

// Backend layer types
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

// Message converters
class ClientToCreateUserCommandConverter : MessageConverter<CreateUserClientRequest, CreateUserCommand> {
    override fun convert(from: CreateUserClientRequest): Either<Failure, CreateUserCommand> {
        // Validation
        if (from.age < 18) {
            return Either.Left(Failure.ValidationFailure("age", "Must be 18 or older"))
        }
        if (!from.email.contains("@")) {
            return Either.Left(Failure.ValidationFailure("email", "Invalid email format"))
        }

        return Either.Right(
            CreateUserCommand(
                name = from.name,
                email = from.email,
                age = from.age,
                validated = true
            )
        )
    }
}

class CommandToBackendRequestConverter : MessageConverter<CreateUserCommand, CreateUserBackendRequest> {
    override fun convert(from: CreateUserCommand): Either<Failure, CreateUserBackendRequest> {
        return Either.Right(
            CreateUserBackendRequest(
                fullName = from.name,
                emailAddress = from.email,
                ageInYears = from.age
            )
        )
    }
}

class BackendResponseToDomainResultConverter : MessageConverter<CreateUserBackendResponse, UserCreatedResult> {
    override fun convert(from: CreateUserBackendResponse): Either<Failure, UserCreatedResult> {
        // This would normally extract the name and email from somewhere
        // For demo purposes, using placeholder values
        return Either.Right(
            UserCreatedResult(
                userId = from.id,
                name = "Retrieved Name",
                email = "retrieved@email.com",
                createdAt = from.timestamp
            )
        )
    }
}

class DomainResultToClientResponseConverter : MessageConverter<UserCreatedResult, CreateUserClientResponse> {
    override fun convert(from: UserCreatedResult): Either<Failure, CreateUserClientResponse> {
        return Either.Right(
            CreateUserClientResponse(
                userId = from.userId,
                message = "User ${from.name} created successfully"
            )
        )
    }
}

// Operation handler with backend integration
class CreateUserHandler(
    private val backendCaller: BackendCaller<UserServiceHost, CreateUserBackendRequest, CreateUserBackendResponse>,
    private val commandToBackend: MessageConverter<CreateUserCommand, CreateUserBackendRequest>,
    private val backendToDomain: MessageConverter<CreateUserBackendResponse, UserCreatedResult>
) : OperationHandler<CreateUserCommand, UserCreatedResult> {

    override suspend fun handle(
        command: CreateUserCommand,
        context: ExecutionContext
    ): Either<Failure, UserCreatedResult> {
        // Convert command to backend request
        val backendRequest = commandToBackend.convert(command)
            .fold({ return Either.Left(it) }, { it })

        // Call backend
        val backendResponse = backendCaller.call(backendRequest)
            .fold({ return Either.Left(it) }, { it })

        // Convert backend response to domain result
        return backendToDomain.convert(backendResponse)
    }
}

// Middleware examples
val loggingMiddleware: Middleware<Any> = { request, next ->
    println("Processing request: $request")
    val result = next(request)
    println("Processed result: $result")
    result
}

val metricsMiddleware: Middleware<Either<Failure, *>> = { result, next ->
    val startTime = System.currentTimeMillis()
    val processedResult = next(result)
    val duration = System.currentTimeMillis() - startTime
    println("Operation took ${duration}ms")
    processedResult
}

// Application setup
class Application {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val httpClient = okHttpClient {
        moshi {
            factory {
                KotlinJsonAdapterFactory()
            }
        }
        okHttp {
            addInterceptor { chain ->
                chain.proceed(chain.call().request().apply { println(body?.toString()) })
            }
        }
    }

    private val conversionRegistry = ConversionRegistry().apply {
        register(ClientToCreateUserCommandConverter())
        register(CommandToBackendRequestConverter())
        register(BackendResponseToDomainResultConverter())
        register(DomainResultToClientResponseConverter())
    }

    private val operationRegistry = OperationRegistry()

    private val executionEngine = ExecutionEngine(operationRegistry, conversionRegistry).apply {
        useInbound(loggingMiddleware)
        useExecution(metricsMiddleware)
    }

    init {
        // Register operations
        val createUserBackendCaller =
            backendCaller<UserServiceHost, CreateUserBackendRequest, CreateUserBackendResponse> {
                host = UserServiceHost.default
                endpoint = Endpoint(HttpMethod.POST, "/facts")
                httpClient = this@Application.httpClient
            }

        val createUserHandler = CreateUserHandler(
            createUserBackendCaller,
            CommandToBackendRequestConverter(),
            BackendResponseToDomainResultConverter()
        )

        val createUserOp =
            operation<CreateUserClientRequest, CreateUserClientResponse, CreateUserCommand, UserCreatedResult>(
                OpId("create-user")
            ) {
                clientToCommand = ClientToCreateUserCommandConverter()
                handler = createUserHandler
                resultToClient = DomainResultToClientResponseConverter()
            }

        operationRegistry.register(createUserOp)
    }

    suspend fun handleRequest(
        opId: OpId,
        request: ClientRequest
    ): Either<Failure, ClientResponse> {
        return when (opId.value) {
            "create-user" -> {
                executionEngine.execute<CreateUserClientRequest, CreateUserClientResponse>(
                    opId,
                    request as CreateUserClientRequest
                )
            }

            else -> Either.Left(Failure.MappingFailure("Unknown operation: ${opId.value}"))
        }
    }
}

// Entry point demonstration
suspend fun main() {
    val app = Application()

    val request = CreateUserClientRequest(
        name = "John Doe",
        email = "john.doe@example.com",
        age = 25
    )

    val result = app.handleRequest(OpId("create-user"), request)

    result.fold(
        ifLeft = { failure ->
            println("Operation failed: $failure")
        },
        ifRight = { response ->
            println("Operation succeeded: $response")
        }
    )
}
