package com.example

import io.ktor.server.engine.*
import io.ktor.server.fetch.*

internal val embeddedServer = embeddedServer(
    Fetch,
    environment = applicationEnvironment {
        config = cloudflareApplicationConfig()
    }
) {
    // Don't install Cloudflare here - it's installed lazily in fetch() with the actual bindings
    rootModule()
    configureKvRoutes()
}