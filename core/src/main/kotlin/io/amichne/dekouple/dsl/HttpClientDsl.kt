package io.amichne.dekouple.dsl

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.amichne.dekouple.transport.http.OkHttpClientAdapter
import io.amichne.dekouple.transport.serialization.JsonSerializer
import io.amichne.dekouple.transport.serialization.MoshiSerializer
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@DekoupleDsl
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

@DekoupleDsl
class MoshiSerializerBuilder internal constructor(
    @PublishedApi
    internal val moshiBuilder: Moshi.Builder = defaultMoshiBuilder
) {
    companion object {
        private val defaultMoshiBuilder: Moshi.Builder = Moshi.Builder().addLast(KotlinJsonAdapterFactory())
    }

    inline fun <reified T : Any> adapter(crossinline block: () -> JsonAdapter<T>) {
        moshiBuilder.add<T>(T::class.java, block())
    }

    fun factory(block: () -> JsonAdapter.Factory) {
        moshiBuilder.add(block())
    }

    internal fun build(): MoshiSerializer = MoshiSerializer(moshiBuilder.build())
}
