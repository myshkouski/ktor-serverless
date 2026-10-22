import cloudflare.cloudflare
import cloudflare.NotFoundHandling

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.js-plain-objects")
    id("cloudflare")
}

group = "com.example"
version = "0.1.0-SNAPSHOT"

kotlin {
    js {
        nodejs()
        useEsModules()
        generateTypeScriptDefinitions()
        binaries.executable()

        cloudflare {
            fetchHandler = "com.example.ServerKt.fetch"
            wrangler {
                compatibilityDate = "2026-10-19"
                compatibilityFlags = setOf("nodejs_compat_v2")
                keepVars = true
                vars.put(
                    "PUBLIC_GREETING", "Hello from Ktor on Cloudflare Workers!"
                )
                kv("CACHE")

                // Assets configuration - serves static files from resources/assets/public
                assets {
                    directory = "src/jsMain/resources/assets/public"
                    binding = "ASSETS"
                    notFoundHandling = NotFoundHandling.NONE.value
                    // Run worker first for API routes, but not for API docs
                    runWorkerFirst(listOf("/api/*", "!/api/docs/*"))
                }
            }
        }

        compilations.named("main") {
            packageJson {
                devDependencies.putIfAbsent("wrangler", "^4.151.2")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(ktorLibs.server.core)
            implementation(ktorLibs.server.htmlBuilder)
            implementation(ktorLibs.server.doubleReceive)
            implementation(ktorLibs.server.contentNegotiation)
            implementation(ktorLibs.serialization.kotlinx.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(ktorLibs.server.testHost)
        }

        jsMain.dependencies {
            implementation("io.ktor:ktor-server-fetch:3.6.0")
        }
    }
}
