package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnvRoutingTest {

    @Test
    fun `env endpoint lists only public config values`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "PUBLIC_APP_NAME" to "ktor-serverless-sample",
                "PUBLIC_GREETING" to "hello \"world\"",
                "SECRET_TOKEN" to "secret"
            )
        }

        application {
            rootModule()
        }

        val response = client.get("/env")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.headers[HttpHeaders.ContentType]!!.startsWith("application/json"))
        assertEquals(
            """{"PUBLIC_APP_NAME":"ktor-serverless-sample","PUBLIC_GREETING":"hello \"world\""}""",
            response.bodyAsText()
        )
    }
}
