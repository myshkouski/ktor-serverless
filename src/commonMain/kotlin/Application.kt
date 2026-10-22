package com.example

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.doublereceive.*

fun Application.rootModule() {
    install(ContentNegotiation) {
        json()
    }

    install(DoubleReceive)

    mainPage()

    configureEnvRoutes()
    configureEchoRoutes()
}
