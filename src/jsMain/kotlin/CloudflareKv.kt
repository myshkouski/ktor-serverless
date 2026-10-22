package com.example

import io.ktor.server.application.*
import io.ktor.util.*
import kotlinx.coroutines.await
import kotlin.js.Promise

external interface KVNamespace {
    fun get(key: String): Promise<String?>
    fun put(key: String, value: String): Promise<Unit>
    fun put(key: String, value: String, options: KVNamespacePutOptions): Promise<Unit>
    fun delete(key: String): Promise<Unit>
    fun list(options: dynamic = definedExternally): Promise<KVNamespaceListResult>
}

external interface KVNamespacePutOptions {
    var expiration: Double
    var expirationTtl: Double
}

external interface KVNamespaceListResult {
    val keys: Array<KVNamespaceListKey>
    @JsName("list_complete")
    val listComplete: Boolean
    val cursor: String?
}

external interface KVNamespaceListKey {
    val name: String
}

private val CloudflareBindingsAttribute = AttributeKey<Any>("CloudflareBindings")
private val CloudflareKvAttribute = AttributeKey<MutableMap<String, CloudflareKv>>("CloudflareKv")

class CloudflareConfig(
    val bindings: dynamic,
    private val configuredBindings: MutableMap<String, String> = mutableMapOf()
) {
    fun kv(binding: String, scope: String? = null) {
        configuredBindings[binding] = scope ?: ""
    }
    
    internal fun getConfiguredBindings(): Map<String, String> = configuredBindings
}

object Cloudflare {
    // val key = AttributeKey<Cloudflare>("Cloudflare")
    
    fun install(pipeline: Application, config: CloudflareConfig) {
        val bindings = config.bindings
        
        pipeline.attributes.put(CloudflareBindingsAttribute, bindings)
        
        val kvMap = mutableMapOf<String, CloudflareKv>()
        config.getConfiguredBindings().forEach { (binding, scopeStr) ->
            val namespace = bindings[binding]
            check(namespace != null) { "Cloudflare KV binding '$binding' is not available." }
            val cloudflareKv = CloudflareKv(namespace.unsafeCast<KVNamespace>(), scopeStr.takeIf { it.isNotEmpty() })
            kvMap[binding] = cloudflareKv
        }
        pipeline.attributes.put(CloudflareKvAttribute, kvMap)
    }
}

fun Application.installCloudflare(bindings: dynamic, config: CloudflareConfig.() -> Unit): CloudflareConfig {
    val pluginConfig = CloudflareConfig(bindings)
    pluginConfig.config()
    Cloudflare.install(this, pluginConfig)
    return pluginConfig
}

val Application.cloudflareBindings: Any
    get() = attributes.getOrNull(CloudflareBindingsAttribute)
        ?: error("Cloudflare not installed. Call installCloudflare(bindings) { ... } first.")

fun Application.kv(binding: String): CloudflareKv {
    val kvMap = attributes.getOrNull(CloudflareKvAttribute)
        ?: error("Cloudflare not installed. Call installCloudflare(bindings) { ... } first.")
    return kvMap[binding] ?: error("KV binding '$binding' not configured. Add it to installCloudflare(bindings) { kv('$binding') }")
}

class CloudflareKv internal constructor(
    private val namespace: KVNamespace,
    scope: String? = null
) {
    private val scopePrefix: String? = scope?.let { "$it:" }

    private fun scoped(key: String): String = scopePrefix?.let { "$it$key" } ?: key

    fun withScope(scope: String): CloudflareKv = CloudflareKv(namespace, scope)

    suspend fun get(key: String): String? =
        namespace.get(scoped(key)).await()

    suspend fun put(key: String, value: String) {
        namespace.put(scoped(key), value).await()
    }

    suspend fun put(key: String, value: String, ttlSeconds: Double) {
        val options: KVNamespacePutOptions = js("({})")
        options.expirationTtl = ttlSeconds
        namespace.put(scoped(key), value, options).await()
    }

    suspend fun put(key: String, value: String, expirationTimestamp: Double, isExpirationTtl: Boolean = false) {
        val options: KVNamespacePutOptions = js("({})")
        if (isExpirationTtl) {
            options.expirationTtl = expirationTimestamp
        } else {
            options.expiration = expirationTimestamp
        }
        namespace.put(scoped(key), value, options).await()
    }

    suspend fun delete(key: String) {
        namespace.delete(scoped(key)).await()
    }

    suspend fun list(): List<String> {
        val keys = mutableListOf<String>()
        var cursor: String? = null

        do {
            val options: dynamic = js("({})")
            scopePrefix?.let { options.prefix = it }
            cursor?.let { options.cursor = it }

            val page = namespace.list(options).await()
            keys += page.keys.map { it.name.removePrefix(scopePrefix ?: "") }
            cursor = page.cursor.takeUnless { page.listComplete }
        } while (cursor != null)

        return keys
    }
}