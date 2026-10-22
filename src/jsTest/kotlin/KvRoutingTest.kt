package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

private const val SCOPE = "123e4567-e89b-12d3-a456-426614174000"
private const val OTHER_SCOPE = "123e4567-e89b-12d3-a456-426614174001"

class KvRoutingTest {

    @Test
    fun `kv routes store, read, and delete scoped values`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        application {
            installCloudflare(fakeEnv()) {
                kv("CACHE")
            }
            rootModule()
            configureKvRoutes()
        }

        assertEquals(HttpStatusCode.OK, client.put("/kv/$SCOPE/hello") { setBody("world") }.status)
        assertEquals("world", client.get("/kv/$SCOPE/hello").bodyAsText())
        assertEquals("null", client.get("/kv/$OTHER_SCOPE/hello").bodyAsText())

        assertEquals(HttpStatusCode.OK, client.delete("/kv/$SCOPE/hello").status)
        assertEquals("null", client.get("/kv/$SCOPE/hello").bodyAsText())
    }

    @Test
    fun `kv route lists scoped keys across pages`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        application {
            installCloudflare(fakeEnv()) {
                kv("CACHE")
            }
            rootModule()
            configureKvRoutes()
        }

        client.put("/kv/$SCOPE/a") { setBody("1") }
        client.put("/kv/$SCOPE/b") { setBody("2") }
        client.put("/kv/$SCOPE/c") { setBody("3") }
        client.put("/kv/$OTHER_SCOPE/a") { setBody("other") }

        val response = client.get("/kv/$SCOPE")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""["a","b","c"]""", response.bodyAsText())
        assertEquals("""["a"]""", client.get("/kv/$OTHER_SCOPE").bodyAsText())
    }

    @Test
    fun `kv route rejects non uuid scopes`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        application {
            installCloudflare(fakeEnv()) {
                kv("CACHE")
            }
            rootModule()
            configureKvRoutes()
        }

        assertEquals(HttpStatusCode.BadRequest, client.get("/kv/not-a-uuid").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/kv/not-a-uuid/hello").status)
    }
}

private fun fakeEnv(): Any = js(
    """
    ({
        CACHE: {
            values: Object.create(null),
            get(key) { return Promise.resolve(key in this.values ? this.values[key] : null); },
            put(key, value) { this.values[key] = value; return Promise.resolve(); },
            delete(key) { delete this.values[key]; return Promise.resolve(); },
            list(options) {
                const prefix = options && options.prefix ? options.prefix : '';
                const names = Object.keys(this.values)
                    .filter(function (name) { return name.indexOf(prefix) === 0; })
                    .sort();
                const start = options && options.cursor ? Number(options.cursor) : 0;
                const page = names.slice(start, start + 2);
                const next = start + page.length;
                return Promise.resolve({
                    keys: page.map(function (name) { return { name: name }; }),
                    list_complete: next >= names.length,
                    cursor: next < names.length ? String(next) : undefined,
                });
            }
        }
    })
    """
)
