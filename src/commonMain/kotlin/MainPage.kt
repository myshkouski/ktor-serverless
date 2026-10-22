package com.example

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Application.mainPage() {
    routing {
        get("/") {
            call.respondHtml {
                head {
                    title { +"Ktor on Cloudflare Workers" }
                    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                    style { +"""
                        body { font-family: system-ui, sans-serif; max-width: 800px; margin: 2rem auto; padding: 0 1rem; line-height: 1.6; }
                        h1 { color: #333; }
                        h2 { color: #555; margin-top: 2rem; }
                        code { background: #f4f4f4; padding: 0.1rem 0.3rem; border-radius: 3px; font-size: 0.9em; }
                        pre { background: #f4f4f4; padding: 1rem; border-radius: 5px; overflow-x: auto; }
                        table { width: 100%; border-collapse: collapse; margin: 1rem 0; }
                        th, td { border: 1px solid #ddd; padding: 0.5rem; text-align: left; }
                        th { background: #f4f4f4; }
                        .method { font-family: monospace; font-weight: bold; }
                        .method-get { color: #2e7d32; }
                        .method-put { color: #f57c00; }
                        .method-delete { color: #c62828; }
                        .endpoint { font-family: monospace; }
                        ul { margin: 0.5rem 0; }
                        li { margin: 0.25rem 0; }
                    """ }
                }
                body {
                    h1 { +"Ktor on Cloudflare Workers" }

                    p {
                        +"This project demonstrates how to run a "
                        strong { +"Ktor server application" }
                        +" on "
                        strong { +"Cloudflare Workers" }
                        +" using "
                        strong { +"Kotlin/JS" }
                        +" and "
                        strong { +"Kotlin Multiplatform" }
                        +"."
                    }

                    h2 { +"Motivation" }
                    p {
                        +"Traditional serverless platforms often require you to write code in JavaScript/TypeScript, Python, or Go. "
                        +"If you're a Kotlin developer, you either have to learn a new language, use JVM-based runtimes with cold start penalties, "
                        +"or give up on Kotlin's type safety, coroutines, and ecosystem."
                    }
                    p {
                        +"This project shows that you can write "
                        strong { +"idiomatic Ktor applications in Kotlin" }
                        +" and deploy them to "
                        strong { +"Cloudflare Workers" }
                        +" — a JavaScript-based edge runtime with:"
                    }
                    ul {
                        li { +"Near-zero cold starts (ms-level startup)" }
                        li { +"Global edge deployment (300+ locations)" }
                        li { +"Generous free tier (100k requests/day)" }
                        li { +"Native TypeScript interop via Kotlin/JS" }
                    }
                    p {
                        +"By compiling Kotlin to JavaScript via Kotlin/JS, we get the best of both worlds: "
                        +"Kotlin's developer experience with Cloudflare's performance and pricing."
                    }

                    h2 { +"Architecture" }
                    p {
                        +"The project uses Kotlin Multiplatform to share Ktor routing, serialization, and business logic "
                        +"(in "
                        code { +"commonMain" }
                        +") while keeping Cloudflare-specific code (Fetch engine, KV bindings, Wrangler config) "
                        + "in "
                        code { +"jsMain" }
                        +". The Kotlin/JS compiler outputs JavaScript that runs directly on Cloudflare Workers via Wrangler."
                    }

                    h2 { +"API Endpoints" }
                    p { +"All endpoints are available at the worker's base URL:" }

                    table {
                        thead {
                            tr {
                                th { +"Method" }
                                th { +"Path" }
                                th { +"Description" }
                            }
                        }
                        tbody {
                            tr {
                                td { span(classes = "method method-get") { +"GET" } }
                                td { code(classes = "endpoint") { +"/" } }
                                td { +"Hello World / Project landing page" }
                            }
                            tr {
                                td { span(classes = "method method-get") { +"GET" } }
                                td { code(classes = "endpoint") { +"/env" } }
                                td { +"Lists public environment variables (prefixed with PUBLIC_)" }
                            }
                            tr {
                                td { span(classes = "method method-get") { +"GET" } }
                                td { code(classes = "endpoint") { +"/kv/{scope}" } }
                                td { +"List all keys in a KV namespace scope" }
                            }
                            tr {
                                td { span(classes = "method method-get") { +"GET" } }
                                td { code(classes = "endpoint") { +"/kv/{scope}/{key}" } }
                                td { +"Get a value by key" }
                            }
                            tr {
                                td { span(classes = "method method-put") { +"PUT" } }
                                td { code(classes = "endpoint") { +"/kv/{scope}/{key}" } }
                                td { +"Store a value" }
                            }
                            tr {
                                td { span(classes = "method method-delete") { +"DELETE" } }
                                td { code(classes = "endpoint") { +"/kv/{scope}/{key}" } }
                                td { +"Delete a value" }
                            }
                            tr {
                                td { span(classes = "method method-get") { +"ANY" } }
                                td { code(classes = "endpoint") { +"/echo" } }
                                td { +"Echoes back the full HTTP request as plaintext" }
                            }
                        }
                    }

                    p {
                        strong { +"Note: " }
                        code { +"scope" }
                        +" must be a UUID (used as KV namespace prefix for isolation)."
                    }

                    h2 { +"Features" }
                    ul {
                        li {
                            strong { +"Ktor on Cloudflare Workers" }
                            +": Run Ktor apps on Cloudflare's edge runtime via Kotlin/JS" }
                        li {
                            strong { +"Kotlin Multiplatform" }
                            +": Share routing, serialization, and business logic across platforms" }
                        li {
                            strong { +"Cloudflare KV Integration" }
                            +": Type-safe key-value storage with scoped namespaces" }
                        li {
                            strong { +"Environment Variables" }
                            +": Secure config via Wrangler vars/secrets with public/private distinction" }
                        li {
                            strong { +"Custom Gradle Plugin" }
                            +": Cloudflare extension for Wrangler config generation and deployment" }
                    }

                    h2 { +"Project Structure" }
                    pre { +"""src/
├── commonMain/kotlin/          # Shared Ktor application logic
│   ├── Application.kt          # App entry point
│   ├── MainPage.kt             # This page
│   ├── EnvRouting.kt           # /env endpoint (public/private config)
│   └── Json.kt                 # Serialization helpers
├── jsMain/kotlin/              # Cloudflare Workers target
│   ├── embeddedServer.kt       # Fetch-based server setup
│   ├── CloudflareEnv.kt        # Env var access (vars + secrets)
│   ├── CloudflareKv.kt         # KV namespace bindings
│   └── KvRouting.kt            # /kv/* routes for KV operations
└── commonTest/kotlin/          # Shared tests""" }

                    h2 { +"Useful Links" }
                    ul {
                        li { externalLink(href = "https://ktor.io/docs") { +"Ktor Documentation" } }
                        li { externalLink(href = "https://kotlinlang.org/docs/js-overview.html") { +"Kotlin/JS Documentation" } }
                        li { externalLink(href = "https://developers.cloudflare.com/workers/") { +"Cloudflare Workers Documentation" } }
                        li { externalLink(href = "https://developers.cloudflare.com/workers/wrangler/") { +"Wrangler CLI" } }
                        li { externalLink(href = "https://start.ktor.io") { +"Ktor Project Generator" } }
                    }

                    h2 { +"TODO" }
                    ul {
                        li {
                            strong { +"ktor-server-fetch module" }
                            +" — Extract the Cloudflare Workers "
                            code { +"fetch" }
                            +"-based server engine into a reusable library (see "
                            externalLink(href = "https://github.com/myshkouski/ktor-serverless.git") { +"myshkouski/ktor-serverless" }
                            +") repo"
                        }
                        li {
                            strong { +"Cloudflare Gradle Plugin" }
                            +" — Publish the custom "
                            code { +"cloudflare" }
                            +" Gradle extension (Wrangler config generation, KV bindings, deploy tasks) as a standalone plugin"
                        }
                        li {
                            strong { +"D1 Database Integration" }
                            +" — Add type-safe Cloudflare D1 (SQLite) bindings and routing examples"
                        }
                        li {
                            strong { +"Durable Objects Support" }
                            +" — Demonstrate stateful serverless patterns with Cloudflare Durable Objects"
                        }
                        li {
                            strong { +"Edge Logging & Metrics" }
                            +" — Integrate Cloudflare Workers Analytics Engine for observability"
                        }
                        li {
                            strong { +"Multi-environment Deployments" }
                            +" — Staging/production Wrangler configs via Gradle build variants"
                        }
                    }
                }
            }
        }
    }
}

private fun FlowOrInteractiveOrPhrasingContent.externalLink(
    href: String,
    block: A.() -> Unit = {}
) {
    a(href = href, target = "blank", block = block)
}
