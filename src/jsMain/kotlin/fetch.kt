@file:OptIn(ExperimentalJsExport::class)

package com.example

import io.ktor.server.fetch.*
import kotlinx.atomicfu.locks.synchronized
import org.w3c.fetch.Request
import org.w3c.fetch.Response
import kotlin.js.Promise

private var serverStarted = false

@JsExport
@JsName("fetch")
suspend fun fetch(request: Request, env: Any): Promise<Response> {
    synchronized(serverStarted) {
        if (!serverStarted) {
            // Install Cloudflare plugin with the bindings from the request
            embeddedServer.application.installCloudflare(env) {
                kv("CACHE")
            }
            embeddedServer.startSuspend(true)
        }
        serverStarted = true
    }
    return embeddedServer.toFetchHandler()(request)
}
