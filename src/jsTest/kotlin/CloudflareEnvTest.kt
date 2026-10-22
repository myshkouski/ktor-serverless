package com.example

import io.ktor.client.request.get
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.*

class CloudflareEnvTest {

    @Test
    fun `cloudflare bindings are accessible via application plugin`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "PUBLIC_GREETING" to "from-cloudflare-env",
                "SECRET_TOKEN" to "do-not-share"
            )
        }
        application {
            installCloudflare(js("({ PUBLIC_GREETING: 'from-cloudflare-env', SECRET_TOKEN: 'do-not-share', CACHE: {} })")) {
                kv("CACHE")
            }
            rootModule()
        }

        // Verify the plugin is installed and accessible via a route
        client.get("/env").apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            assertTrue(body.contains("from-cloudflare-env"))
        }
    }
}
