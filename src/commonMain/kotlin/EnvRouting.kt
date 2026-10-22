package com.example

import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureEnvRoutes() {
    routing {
        get("/env") {
            val publicEnv = application.environment.config.publicEnv()
            call.respond(publicEnv)
        }
    }
}

private const val PUBLIC_ENV_PREFIX = "PUBLIC_"

internal fun ApplicationConfig.publicEnv(): Map<String, String> =
    toMap()
        .entries
        .filter { (key, value) -> key.startsWith(PUBLIC_ENV_PREFIX) && value is String }
        .sortedBy { it.key }
        .associate { it.key to it.value as String }
