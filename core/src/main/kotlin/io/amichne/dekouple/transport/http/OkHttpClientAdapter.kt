package io.amichne.dekouple.transport.http

import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either
import io.amichne.dekouple.transport.serialization.JsonSerializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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
