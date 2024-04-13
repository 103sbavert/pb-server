package com.pb

import com.pb.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
    configureSecurity()
    configureSerialization()
    configureAuthorizedDbAccess()
}
