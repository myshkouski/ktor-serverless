package com.example

import io.ktor.server.config.*

internal fun cloudflareApplicationConfig(): ApplicationConfig {
    // Try to get bindings from node:process (local dev), fallback to empty config
    val bindings: dynamic? = try {
        js("typeof process !== 'undefined' && process.env ? process.env : undefined")
    } catch (e: Exception) {
        null
    }
    
    if (bindings == null) {
        return MapApplicationConfig()
    }
    
    val keys = js("Object").keys(bindings) as Array<String>
    val map = mutableMapOf<String, String>()
    for (key in keys) {
        val value = bindings[key]
        if (value is String) {
            map[key] = value
        }
    }
    return MapApplicationConfig(map.entries.map { it.key to it.value })
}
