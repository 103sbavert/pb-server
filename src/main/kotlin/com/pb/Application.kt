package com.pb

import com.pb.plugins.configureAuthorizedDbAccess
import com.pb.plugins.configureSecurity
import com.pb.plugins.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
    configureSecurity()
    configureSerialization()
    configureAuthorizedDbAccess()
}
