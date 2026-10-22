package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val UUID_REGEX = Regex(
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
)
private const val MAX_TTL_SECONDS = 600

fun Application.configureKvRoutes() {
    routing {
        route("/kv") {
            get("/{scope}") {
                val scope = call.scopeParameter() ?: return@get
                call.respond(
                    kv("CACHE").withScope(scope).list()
                )
            }

            get("/{scope}/{key}") {
                val scope = call.scopeParameter() ?: return@get
                val key = call.parameters["key"]!!
                call.respondText(kv("CACHE").withScope(scope).get(key) ?: "null")
            }

            put("/{scope}/{key}") {
                val scope = call.scopeParameter() ?: return@put
                val key = call.parameters["key"]!!
                val ttlParam = call.request.queryParameters["ttl"]
                val value = call.receiveText()

                val ttl = ttlParam?.toDoubleOrNull()
                    ?: MAX_TTL_SECONDS.toDouble()

                if (ttl <= 0 || ttl > MAX_TTL_SECONDS) {
                    call.respondText("TTL must be between 1 and $MAX_TTL_SECONDS seconds", status = HttpStatusCode.BadRequest)
                    return@put
                }

                kv("CACHE").withScope(scope).put(key, value, ttl)
                call.respondText("ok")
            }

            delete("/{scope}/{key}") {
                val scope = call.scopeParameter() ?: return@delete
                val key = call.parameters["key"]!!
                kv("CACHE").withScope(scope).delete(key)
                call.respondText("deleted")
            }
        }
    }
}

private suspend fun ApplicationCall.scopeParameter(): String? {
    val scope = parameters["scope"]!!
    return if (UUID_REGEX.matches(scope)) {
        scope
    } else {
        respondText("Scope must be a valid UUID", status = HttpStatusCode.BadRequest)
        null
    }
}
