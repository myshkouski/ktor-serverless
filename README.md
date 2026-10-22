# ktor-serverless-sample

This project demonstrates how to run a **Ktor server application on Cloudflare Workers** using **Kotlin/JS** and **Kotlin Multiplatform**.

[Public instance](https://ktor-serverless-sample.alexeimyshkouski.workers.dev)

## Motivation

Traditional serverless platforms often require you to write code in JavaScript/TypeScript, Python, or Go. If you're a Kotlin developer, you either have to:
- Learn a new language for serverless functions
- Use JVM-based runtimes (AWS Lambda, Google Cloud Functions) which have cold start penalties and higher memory overhead
- Give up on Kotlin's type safety, coroutines, and ecosystem

This project shows that you can write **idiomatic Ktor applications in Kotlin** and deploy them to **Cloudflare Workers** — a JavaScript-based edge runtime with:
- **Near-zero cold starts** (ms-level startup)
- **Global edge deployment** (300+ locations)
- **Generous free tier** (100k requests/day)
- **Native TypeScript interop** via Kotlin/JS

By compiling Kotlin to JavaScript via Kotlin/JS, we get the best of both worlds: Kotlin's developer experience with Cloudflare's performance and pricing.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Kotlin Multiplatform                      │
├─────────────────────┬───────────────────────────────────────┤
│     commonMain      │           jsMain                       │
│  (shared Ktor code) │  (Cloudflare-specific: Fetch engine,   │
│  - Routing          │   KV bindings, Wrangler config)        │
│  - Env handling     │                                        │
│  - Serialization    │                                        │
└─────────────────────┴───────────────────────────────────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │  Kotlin/JS Compiler │
                    └───────────────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │  Cloudflare Workers │
                    │  (via Wrangler)      │
                    └───────────────────┘
```

## Features

| Name | Description |
|------|-------------|
| **Ktor on Cloudflare Workers** | Run Ktor apps on Cloudflare's edge runtime via Kotlin/JS |
| **Kotlin Multiplatform** | Share routing, serialization, and business logic across platforms |
| **Cloudflare KV Integration** | Type-safe key-value storage with scoped namespaces |
| **Environment Variables** | Secure config via Wrangler vars/secrets with public/private distinction |
| **Custom Gradle Plugin** | `cloudflare` extension for Wrangler config generation and deployment (`wranglerDev`, `wranglerDeploy`, `wranglerDeployProduction`) |
| **AsyncAPI Support** | Generate API specs from Ktor routes (via `asyncapi-ktor`) |

## Project Structure

```
src/
├── commonMain/kotlin/          # Shared Ktor application logic
│   ├── Application.kt          # App entry point
│   ├── Routing.kt              # Basic routes (/)
│   ├── EnvRouting.kt           # /env endpoint (public/private config)
│   └── Json.kt                 # Serialization helpers
├── jsMain/kotlin/              # Cloudflare Workers target
│   ├── embeddedServer.kt       # Fetch-based server setup
│   ├── CloudflareEnv.kt        # Env var access (vars + secrets)
│   ├── CloudflareKv.kt         # KV namespace bindings
│   └── KvRouting.kt            # /kv/* routes for KV operations
└── commonTest/kotlin/          # Shared tests
```

## Building & Running

### Prerequisites
- JDK 17+ (Kotlin 2.x requirement)
- Node.js 20+ (for Wrangler)
- Cloudflare account (for deployment)

### Local Development

```bash
# Run tests
./gradlew test

# Build the project
./gradlew build

# Run locally with Wrangler dev server (hot reload)
./gradlew wranglerDev
```

If the server starts successfully, you'll see:
```
⛅️ wrangler 4.x.x
--------------------
Your worker has access to the following bindings:
- KV Namespaces: CACHE
- Vars: PUBLIC_GREETING
--------------------
Listening on http://localhost:8787
```

### Deploy to Cloudflare Workers

```bash
# Build and deploy to Cloudflare (preview/staging)
./gradlew wranglerDeploy

# Build and deploy to production
./gradlew wranglerDeployProduction
```

These tasks automatically:
1. Build the Kotlin/JS production bundle
2. Generate `wrangler.json` from the `cloudflare {}` DSL config
3. Run `npx wrangler deploy` from the generated package directory

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Hello World |
| GET | `/env` | Lists public environment variables |
| GET | `/kv/{scope}` | List all keys in a KV namespace scope |
| GET | `/kv/{scope}/{key}` | Get a value by key |
| PUT | `/kv/{scope}/{key}` | Store a value |
| DELETE | `/kv/{scope}/{key}` | Delete a value |

> **Note**: `scope` must be a UUID (used as KV namespace prefix for isolation).

## Configuration

### Wrangler Variables (public)
Set in `build.gradle.kts` under `cloudflare.wrangler.vars`:
```kotlin
vars.put("PUBLIC_GREETING", "Hello from Cloudflare Workers!")
```

### Wrangler Secrets (private)
Set via CLI (never committed):
```bash
npx wrangler secret put API_KEY
```

### KV Namespaces
Defined in `build.gradle.kts`:
```kotlin
kv("CACHE")
```
Creates a binding named `CACHE` available via `Cloudflare.kv("CACHE", scope)`.

## Useful Links

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Kotlin/JS Documentation](https://kotlinlang.org/docs/js-overview.html)
- [Cloudflare Workers Documentation](https://developers.cloudflare.com/workers/)
- [Wrangler CLI](https://developers.cloudflare.com/workers/wrangler/)
- [Ktor Project Generator](https://start.ktor.io)

## TODO

- [ ] **ktor-server-fetch module** — Extract the Cloudflare Workers `fetch`-based server engine into a reusable library ([ktor fork](https://github.com/myshkouski/ktor))
- [ ] **Cloudflare Gradle Plugin** — Publish the custom `cloudflare` Gradle extension (Wrangler config generation, KV bindings, deploy tasks) as a standalone plugin
- [ ] **D1 Database Integration** — Add type-safe Cloudflare D1 (SQLite) bindings and routing examples
- [ ] **Durable Objects Support** — Demonstrate stateful serverless patterns with Cloudflare Durable Objects
- [ ] **Edge Logging & Metrics** — Integrate Cloudflare Workers Analytics Engine for observability
- [ ] **Multi-environment Deployments** — Staging/production Wrangler configs via Gradle build variants