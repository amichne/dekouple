package io.amichne.dekouple.transport.serialization

import com.squareup.moshi.Moshi
import io.amichne.dekouple.core.failure.Failure
import io.amichne.dekouple.core.types.Either

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
