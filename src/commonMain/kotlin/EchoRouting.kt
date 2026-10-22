package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureEchoRoutes() {
    routing {
        route("/echo") {
            get { handleEcho() }
            post { handleEcho() }
            put { handleEcho() }
            delete { handleEcho() }
            patch { handleEcho() }
            head { handleEcho() }
            options { handleEcho() }
        }
    }
}

private suspend fun RoutingContext.handleEcho() {
    val call = this.call
    val request = call.request
    val responseBuilder = StringBuilder()

    // Request line: METHOD PATH HTTP/VERSION
    val method = request.httpMethod.value
    val path = request.uri.toString()
    val version = "HTTP/1.1"
    responseBuilder.append("$method $path $version\r\n")

    // Headers
    request.headers.forEach { name, values ->
        values.forEach { value ->
            responseBuilder.append("$name: $value\r\n")
        }
    }

    // Empty line before body
    responseBuilder.append("\r\n")

    // Body - use receive<ByteArray?>() to handle empty/no body gracefully
    val bodyBytes = call.receive<ByteArray?>()
    if (bodyBytes != null && bodyBytes.isNotEmpty()) {
        responseBuilder.append(bodyBytes.decodeToString())
    }

    call.respondText(
        responseBuilder.toString(),
        ContentType.Text.Plain
    )
}