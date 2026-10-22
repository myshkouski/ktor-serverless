@file:JsModule("node:process")

package com.example

import kotlin.js.Json

@JsName("env")
internal external val cloudflareEnv: Json
